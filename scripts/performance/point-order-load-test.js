import http from 'k6/http';
import { check, sleep } from 'k6';

// ──────────────────────────────────────────────
// 환경변수 설정 (기본값 포함)
// 실행 시 -e 옵션으로 덮어쓸 수 있음
// 예시: k6 run -e VUS=20 -e DURATION=60s point-order-load-test.js
// 로컬에선 localhost이지만, 일단 자료에는 host.docker.internal이라 함
// ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL   || 'http://host.docker.internal:8080';
// ✅ 수정: 단일 EMAIL 환경변수 제거
//         → VU별 이메일을 자동 생성 (test1@gmail.com ~ testN@gmail.com)
//         → 각 VU가 서로 다른 userId를 가지므로 락 키 분산됨
//            wallet:{userId_1} ~ wallet:{userId_N}

const PASSWORD  = __ENV.PASSWORD  || 'test1234@';
const VU_COUNT  = Number(__ENV.VUS || 5);

// ✅ 409를 HTTP 성공으로 간주 (비즈니스 정상 응답) — 기존 코드 유지
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409));

// ──────────────────────────────────────────────
// 부하 설정
// ──────────────────────────────────────────────
export const options = {
    vus:      VU_COUNT,
    duration: __ENV.DURATION   || '30s',
    thresholds: {
        // HTTP 오류율 1% 미만
        http_req_failed:   ['rate<0.01'],
        // P95 응답시간 500ms 미만
        http_req_duration: ['p(95)<500'],
        // 체크 성공률 99% 초과
        checks:            ['rate>0.99'],
    },
};

// ──────────────────────────────────────────────
// setup() — 테스트 시작 전 1회만 실행
// ✅ 수정: VU별로 각각 로그인하여 토큰 배열로 반환
//         기존: 토큰 1개 반환 → 모든 VU가 동일 userId 사용 → 락 경합 발생
//         변경: 토큰 N개 반환 → VU마다 다른 userId 사용 → 락 키 분산
// ──────────────────────────────────────────────
export function setup() {
    const tokens = [];
    const headers = { 'Content-Type': 'application/json' };

    for (let i = 1; i <= VU_COUNT; i++) {
        const email    = `test${i}@gmail.com`;
        const name     = `tester${i}`;

        // ── 1. 회원가입 시도 (이미 존재하면 409 → 무시하고 로그인 진행) ──
        const signupRes = http.post(
            `${BASE_URL}/api/users/signup`,
            JSON.stringify({ name, email, password: PASSWORD }),
            { headers }
        );

        if (signupRes.status === 201) {
            console.log(`VU${i} 계정 생성 완료 (${email})`);
        } else if (signupRes.status === 409) {
            console.log(`VU${i} 계정 이미 존재 → 로그인 진행 (${email})`);
        } else {
            console.warn(`VU${i} 회원가입 예상 외 응답 [${signupRes.status}]: ${signupRes.body}`);
        }

        // ── 2. 로그인 ──────────────────────────────────────────────────────
        const loginRes = http.post(
            `${BASE_URL}/api/users/login`,
            JSON.stringify({ email, password: PASSWORD }),
            { headers }
        );

        check(loginRes, {
            [`setup: VU${i} login status is 200`]: (r) => r.status === 200,
        });

        const accessToken = loginRes.json('data.accessToken');

        if (!accessToken) {
            console.error(`VU${i} 로그인 실패 (${email}). 응답: ${loginRes.body}`);
        }

        tokens.push(accessToken);
    }

    return { tokens };
}
// ──────────────────────────────────────────────
// default function — VU마다 duration 동안 반복 실행
// ✅ 수정: __VU(1-based)로 자신의 토큰 선택
//         VU1 → test1@gmail.com 토큰
//         VU2 → test2@gmail.com 토큰 ...
// ──────────────────────────────────────────────
export default function (data) {
    const headers = {
        'Content-Type':  'application/json',
        // ✅ 수정: data.accessToken → data.tokens[__VU - 1]
        //         __VU는 1부터 시작, 배열은 0부터 시작이므로 -1
        'Authorization': `Bearer ${data.tokens[__VU - 1]}`,
    };

    // ── 1. 내 지갑 조회 ──────────────────────────
    // 구독 구매 전 포인트 잔액 확인
    const walletRes = http.get(
        `${BASE_URL}/api/me/wallets`,
        { headers, tags: { api: 'wallet_get' } }
    );

    check(walletRes, {
        'wallet: status is 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // ── 2. 구독 목록 조회 ─────────────────────────
    // 현재 구독 상태 확인
    const subscriptionRes = http.get(
        `${BASE_URL}/api/me/subscriptions`,
        { headers, tags: { api: 'subscription_get' } }
    );

    check(subscriptionRes, {
        'subscription: status is 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // ── 3. 구독 구매 (FREE 플랜) ──────────────────
    // FREE 플랜은 계정당 1회 제한 → 이미 사용했으면 409 반환
    // RECURRING 플랜은 50P 필요 → 잔액 부족 시 400 반환
    const purchaseRes = http.post(
        `${BASE_URL}/api/me/point-orders/purchases`,
        JSON.stringify({ planType: 'FREE' }),
        { headers, tags: { api: 'point_order_purchase' } }
    );

    const purchaseOk = check(purchaseRes, {
        // 201: 구매 성공
        // 409: 이미 FREE 플랜 사용 or 이미 구독 중 (정상 비즈니스 응답)
        'purchase: status is 201 or 409': (r) =>
            r.status === 201 || r.status === 409,
    });

    if (!purchaseOk) {
        console.error(`purchase 실패 [VU${__VU}] status=${purchaseRes.status} body=${purchaseRes.body}`);
    }

    sleep(1);
}
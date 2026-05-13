import http from 'k6/http';
import { check, sleep } from 'k6';

// ──────────────────────────────────────────────
// 환경변수 설정 (기본값 포함)
// 실행 시 -e 옵션으로 덮어쓸 수 있음
// 예시: k6 run -e VUS=20 -e DURATION=60s point-order-load-test.js
// 로컬에선 localhost이지만, Dokcer 환경에선 host.docker.internal이라 함
// ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL   || 'http://host.docker.internal:8080';
// 수정: 단일 EMAIL 환경변수 제거
//         → VU별 이메일을 자동 생성 (test1@gmail.com ~ testN@gmail.com)
//         → 각 VU가 서로 다른 userId를 가지므로 락 키 분산됨
//            wallet:{userId_1} ~ wallet:{userId_N}

const PASSWORD  = __ENV.PASSWORD  || 'test1234@';
const VU_COUNT  = Number(__ENV.VUS || 5);

//  409를 HTTP 성공으로 간주 (비즈니스 정상 응답) — 기존 코드 유지
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409));

// ──────────────────────────────────────────────
// 부하 설정
// ──────────────────────────────────────────────
export const options = {
    vus:          VU_COUNT,
    duration:     __ENV.DURATION || '30s',
    // setup()에서 병렬 batch 요청 허용 수 확대
    batch:        200,
    batchPerHost: 200,
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
//    수정: VU별로 각각 로그인하여 토큰 배열로 반환
//         기존: 토큰 1개 반환 → 모든 VU가 동일 userId 사용 → 락 경합 발생
//         변경: 토큰 N개 반환 → VU마다 다른 userId 사용 → 락 키 분산
// ──────────────────────────────────────────────
// 배열을 size 단위로 나누는 헬퍼
function chunkArray(arr, size) {
    const chunks = [];
    for (let i = 0; i < arr.length; i += size) {
        chunks.push(arr.slice(i, i + size));
    }
    return chunks;
}

export function setup() {
    const headers = { 'Content-Type': 'application/json' };
    const CHUNK_SIZE = 100; // 한 번에 병렬로 보낼 요청 수

    // ── 1. 회원가입 병렬 처리 (100개씩 청크) ────────────────────────────
    const signupReqs = [];
    for (let i = 1; i <= VU_COUNT; i++) {
        signupReqs.push({
            method: 'POST',
            url:    `${BASE_URL}/api/users/signup`,
            body:   JSON.stringify({ name: `tester${i}`, email: `test${i}@gmail.com`, password: PASSWORD }),
            params: { headers },
        });
    }

    let created = 0, existed = 0;
    for (const chunk of chunkArray(signupReqs, CHUNK_SIZE)) {
        const responses = http.batch(chunk);
        responses.forEach(r => {
            if (r.status === 201)      created++;
            else if (r.status === 409) existed++;
            else console.warn(`회원가입 예상 외 응답 [${r.status}]: ${r.body}`);
        });
    }
    console.log(`계정 준비 완료 — 신규: ${created}개, 기존: ${existed}개`);

    // ── 2. 로그인 병렬 처리 (100개씩 청크) ─────────────────────────────
    const loginReqs = [];
    for (let i = 1; i <= VU_COUNT; i++) {
        loginReqs.push({
            method: 'POST',
            url:    `${BASE_URL}/api/users/login`,
            body:   JSON.stringify({ email: `test${i}@gmail.com`, password: PASSWORD }),
            params: { headers },
        });
    }

    const tokens = [];
    let loginOk = 0, loginFail = 0;
    for (const chunk of chunkArray(loginReqs, CHUNK_SIZE)) {
        const responses = http.batch(chunk);
        responses.forEach((r, idx) => {
            const token = r.status === 200 ? r.json('data.accessToken') : null;
            if (token) {
                loginOk++;
            } else {
                loginFail++;
                console.error(`로그인 실패 [${r.status}]: ${r.body}`);
            }
            tokens.push(token);
        });
    }
    console.log(`로그인 완료 — 성공: ${loginOk}개, 실패: ${loginFail}개`);

    return { tokens };
}
// ──────────────────────────────────────────────
// default function — VU마다 duration 동안 반복 실행
//    수정: __VU(1-based)로 자신의 토큰 선택
//         VU1 → test1@gmail.com 토큰
//         VU2 → test2@gmail.com 토큰 ...
// ──────────────────────────────────────────────
export default function (data) {
    // ──────────────────────────────────────────────
    // 토끼 수정 :
    // 만일 로그인에 실패하면 data.tokens[0] === null
    // 이 경우 실제 헤더 요청은 Authorization: Bearer null
    // 인증 자체가 실패하므로 토큰 없는 경우 스킵 실행 추가
    // K6에서 가상유저는 1부터 시작, 배열은 0부터 시작이니 -1
    // ──────────────────────────────────────────────
    const token = data.tokens[__VU - 1];
    if (!token) {
        console.error(`VU${__VU} 토큰 없음 — iteration 스킵`);
        sleep(1);
        return;
    }
    const headers = {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${token}`,
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
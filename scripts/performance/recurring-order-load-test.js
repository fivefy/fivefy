import http from 'k6/http';
import { check, sleep } from 'k6';

// ──────────────────────────────────────────────
// RECURRING 구독 구매 동시성 부하테스트
// ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL  || 'http://host.docker.internal:8080';
const PASSWORD  = __ENV.PASSWORD || 'test1234@';
const VU_COUNT  = Number(__ENV.VUS || 10);

// 400(잔액 부족), 409(중복 구매)도 HTTP 레벨 실패로 처리하지 않음
http.setResponseCallback(http.expectedStatuses(
    { min: 200, max: 399 }, 400, 409
));

// ──────────────────────────────────────────────
// 부하 설정
// ──────────────────────────────────────────────
export const options = {
    vus:          VU_COUNT,
    duration:     __ENV.DURATION || '30s',
    batch:        200,
    batchPerHost: 200,
    thresholds: {
        // HTTP 에러율 1% 미만
        http_req_failed:   ['rate<0.01'],
        // P95 응답시간 500ms 미만
        http_req_duration: ['p(95)<500'],
        // 체크 성공률 99% 초과
        checks:            ['rate>0.99'],
    },
};

// ──────────────────────────────────────────────
// 헬퍼: 배열 청크 분할
// ──────────────────────────────────────────────
function chunkArray(arr, size) {
    const chunks = [];
    for (let i = 0; i < arr.length; i += size) {
        chunks.push(arr.slice(i, i + size));
    }
    return chunks;
}

// ──────────────────────────────────────────────
// setup() — 계정 생성 + 로그인 + 잔액 경고
// FREE 플랜 테스트와 계정 충돌 방지: recurring_test{i}@gmail.com 사용
// ──────────────────────────────────────────────
export function setup() {
    const headers = { 'Content-Type': 'application/json' };
    const CHUNK_SIZE = 100;

    // ── 1. 회원가입 병렬 처리 ─────────────────────────
    const signupReqs = [];
    for (let i = 1; i <= VU_COUNT; i++) {
        signupReqs.push({
            method: 'POST',
            url:    `${BASE_URL}/api/users/signup`,
            body:   JSON.stringify({
                name:     `recur${i}`,   // recur1(6자) ~ recur100(8자) → 최대 10자 이하
                email:    `recurring_test${i}@gmail.com`,
                password: PASSWORD,
            }),
            params: { headers },
        });
    }
    let created = 0, existed = 0;
    for (const chunk of chunkArray(signupReqs, CHUNK_SIZE)) {
        http.batch(chunk).forEach(r => {
            if (r.status === 201)      created++;
            else if (r.status === 409) existed++;
            else console.warn(`회원가입 예상 외 응답 [${r.status}]: ${r.body}`);
        });
    }
    console.log(`계정 준비 완료 — 신규: ${created}개, 기존: ${existed}개`);

    // ── 2. 로그인 병렬 처리 ──────────────────────────
    const loginReqs = [];
    for (let i = 1; i <= VU_COUNT; i++) {
        loginReqs.push({
            method: 'POST',
            url:    `${BASE_URL}/api/users/login`,
            body:   JSON.stringify({
                email:    `recurring_test${i}@gmail.com`,
                password: PASSWORD,
            }),
            params: { headers },
        });
    }
    const tokens = [];
    for (const chunk of chunkArray(loginReqs, CHUNK_SIZE)) {
        http.batch(chunk).forEach(r => {
            const token = r.status === 200 ? r.json('data.accessToken') : null;
            if (!token) console.error(`로그인 실패 [${r.status}]: ${r.body}`);
            tokens.push(token);
        });
    }
    console.log(`로그인 완료 — 성공: ${tokens.filter(Boolean).length}개`);

    // ── 3. 잔액 확인 (포인트 부족 계정 경고) ─────────
    // 토끼 수정 :
    // 로그인 실패 시 null값도 배열에 포함되어 요청
    // 생성할 경우 Authorization: Bearer null가 발생
    // const validTokens = tokens.filter(Boolean);으로
    // null, undefined, false, 빈 문자열 제거
    // ──────────────────────────────────────────────
    const validTokens = tokens.filter(Boolean);
    const walletReqs = validTokens.map(token => ({
        method: 'GET',
        url:    `${BASE_URL}/api/me/wallets`,
        params: { headers: { 'Authorization': `Bearer ${token}` } },
    }));
    let insufficient = 0;
    for (const chunk of chunkArray(walletReqs, CHUNK_SIZE)) {
        http.batch(chunk).forEach(r => {
            const total = r.status === 200 ? (r.json('data.totalBalance') ?? 0) : 0;
            if (total < 50) insufficient++;
        });
    }
    if (insufficient > 0) {
        console.warn(`   포인트 부족 계정 ${insufficient}개 발견 — RECURRING 구매가 400으로 실패합니다`);
        console.warn(`   아래 MySQL 명령어로 포인트를 충전 후 재실행하세요:`);
        console.warn(`   docker exec -it mysql mysql -u root -p12345678 fivefy_db -e "UPDATE wallets w JOIN users u ON w.user_id = u.id SET w.event_balance = 10000, w.total_balance = w.balance + 10000 WHERE u.email LIKE 'recurring\\_test%@gmail.com';"`);
    } else {
        console.log(`잔액 확인 완료 — 전원 50P 이상 보유`);
    }

    return { tokens };
}

// ──────────────────────────────────────────────
// default — VU마다 duration 동안 반복 실행
//
// 흐름:
//      지갑 조회 → RECURRING 구매 시도 → (성공 시) 구독 취소 → 반복
//
// 상태코드 의미:
//      201 → 구매 성공 (50P 차감됨) → 취소해서 다음 반복 준비
//      409 → 이미 활성 구독 있음 → 취소 후 재시도
//      400 → 잔액 부족 → 포인트 충전 필요 (테스트 중단 신호)
// ──────────────────────────────────────────────
export default function (data) {
    const token = data.tokens[__VU - 1];
    if (!token) {
        console.error(`VU${__VU} 토큰 없음 — 스킵`);
        return;
    }

    const headers = {
        'Content-Type':  'application/json',
        'Authorization': `Bearer ${token}`,
    };

    // ── 1. 지갑 조회 ─────────────────────────────────
    const walletRes = http.get(
        `${BASE_URL}/api/me/wallets`,
        { headers, tags: { api: 'wallet_get' } }
    );
    check(walletRes, {
        'wallet: status is 200': (r) => r.status === 200,
    });

    sleep(0.3);

    // ── 2. RECURRING 구매 시도 ────────────────────────
    const purchaseRes = http.post(
        `${BASE_URL}/api/me/point-orders/purchases`,
        JSON.stringify({ planType: 'RECURRING' }),
        { headers, tags: { api: 'recurring_purchase' } }
    );

    const purchaseOk = check(purchaseRes, {
        // 201: 구매 성공 (50P 차감)
        // 409: 이미 활성 구독 존재 → 취소 후 재시도
        'purchase: 201(성공) or 409(중복)': (r) =>
            r.status === 201 || r.status === 409,
    });

    if (!purchaseOk) {
        // 400: 잔액 부족 — 포인트 충전 필요
        console.error(`purchase 실패 [VU${__VU}] status=${purchaseRes.status} body=${purchaseRes.body}`);
        sleep(1);
        return;
    }

    sleep(0.3);

    // ── 3. 구독 취소 (다음 iteration에서 재구매 가능하도록) ──
    // 201(구매 성공) or 409(이미 활성) 모두 취소 시도
    const cancelRes = http.del(
        `${BASE_URL}/api/me/subscriptions`,
        null,
        { headers, tags: { api: 'subscription_cancel' } }
    );

    check(cancelRes, {
        // 204: 취소 성공
        // 404: 취소할 구독 없음 (이미 취소됐거나 없는 경우) → 허용
        'cancel: 204(성공) or 404(없음)': (r) =>
            r.status === 204 || r.status === 404,
    });

    sleep(0.4);
}

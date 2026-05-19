import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

// cmd에서 ACCESS_TOKEN으로 넘겨받기
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;

if (!ACCESS_TOKEN) {
    throw new Error('ACCESS_TOKEN is required');
}

export const options = {
    vus: 100,
    iterations: 10000,
};

export default function () {
    const payload = JSON.stringify({
        trackId: 50,
        position: 1
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${ACCESS_TOKEN}`,
        },
    };

    const response = http.patch(
        `${BASE_URL}/api/playlists/1/tracks/index`,
        payload,
        params
    );

    console.log(response.status);
    console.log(response.body);

    check(response, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}

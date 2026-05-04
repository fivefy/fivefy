import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TRACK_ID = __ENV.TRACK_ID || '28';
const PAGE = __ENV.PAGE || '0';
const SIZE = __ENV.SIZE || '20';

export const options = {
    vus: Number(__ENV.VUS || 10),
    duration: __ENV.DURATION || '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        checks: ['rate>0.99'],
    },
};

export function setup() {
    const trackDetailUrl = `${BASE_URL}/api/tracks/${TRACK_ID}`;

    const response = http.get(trackDetailUrl);

    check(response, {
        'track detail warm-up status is 200': (res) => res.status === 200,
    });
}

export default function () {
    getPublicTracks();
    getTrackDetail();
    getTrackComments();

    sleep(1);
}

function getPublicTracks() {
    const url = `${BASE_URL}/api/tracks?page=${PAGE}&size=${SIZE}`;
    const response = http.get(url, {
        tags: {
            api: 'public_tracks',
        },
    });

    check(response, {
        'public tracks status is 200': (res) => res.status === 200,
    });
}

function getTrackDetail() {
    const url = `${BASE_URL}/api/tracks/${TRACK_ID}`;
    const response = http.get(url, {
        tags: {
            api: 'track_detail',
        },
    });

    check(response, {
        'track detail status is 200': (res) => res.status === 200,
    });
}

function getTrackComments() {
    const url = `${BASE_URL}/api/tracks/${TRACK_ID}/comments?page=${PAGE}&size=${SIZE}`;
    const response = http.get(url, {
        tags: {
            api: 'track_comments',
        },
    });

    check(response, {
        'track comments status is 200': (res) => res.status === 200,
    });
}
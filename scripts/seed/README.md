# Seed Scripts

성능 검증용 seed 데이터 생성/적재/검증/초기화 스크립트입니다.

## 목적

- 대량 데이터 기반 성능 병목 분석
- 인덱스 적용 전후 비교
- 캐싱 적용 전후 비교
- 부하 테스트용 데이터 준비

현재는 `integrated` 데이터셋을 제공합니다.

`integrated` 데이터셋은 여러 파트 API 성능 검증에 사용할 수 있도록 사용자, 지갑/포인트, 결제/구독, 음악 컨텐츠, 
플레이리스트, 댓글, 신청, 행동 로그 데이터를 함께 생성하는 통합 seed 데이터셋입니다.

## 구조

```text
scripts/seed/
├── README.md
├── requirements.txt
├── run_seed.sh
├── datasets/
│   └── integrated/
│       ├── generate.py
│       ├── insert.py
│       ├── validate.sql
│       └── clean.sql
└── output/                 # 생성 CSV, Git 제외
    └── integrated/
```

## 사전 준비

```bash
python3 -m pip install -r scripts/seed/requirements.txt
```

DB 접속 정보는 프로젝트 루트의 `.env`를 사용합니다.

```env
DB_NAME=fivefy_db
DB_USER=root
DB_PASSWORD=your-password
DB_HOST=127.0.0.1
DB_PORT=3306
```

## 실행 명령

```bash
scripts/seed/run_seed.sh [command] [dataset] [scale] [seed]
```

기본값은 `run integrated smoke 42`입니다.

```bash
scripts/seed/run_seed.sh
scripts/seed/run_seed.sh run integrated test 42
scripts/seed/run_seed.sh run integrated local 42
scripts/seed/run_seed.sh clean
scripts/seed/run_seed.sh reset integrated smoke 42
```

## Command

| command | 설명 |
|---|---|
| `run` | CSV 생성 → DB 적재 → 검증 SQL 실행 |
| `clean` | DB seed 데이터 초기화 + `scripts/seed/output/` 삭제 |
| `reset` | `clean` 실행 후 `run` 실행 |

## Scale

| scale | 목적 |
|---|---|
| `smoke` | 스크립트/적재 흐름 검증 |
| `test` | `local` 실행 전 중간 규모 검증 |
| `local` | 로컬 성능 분석 |
| `dev` | 개발 서버 부하 테스트 |

각 scale의 실제 생성 row 수는 아래 `Scale별 데이터 규모`를 기준으로 합니다.

<details>
  <summary>📊 Scale별 row 수 보기</summary>

### Scale별 데이터 규모

| table | smoke | test | local | dev |
|---|---:|---:|---:|---:|
| users | 1,000 | 10,000 | 100,000 | 500,000 |
| wallets | 1,000 | 10,000 | 100,000 | 500,000 |
| point_histories | 2,000 | 20,000 | 200,000 | 1,000,000 |
| billing_keys | 500 | 5,000 | 50,000 | 250,000 |
| cash_orders | 2,000 | 20,000 | 200,000 | 1,000,000 |
| point_orders | 1,000 | 10,000 | 100,000 | 500,000 |
| payments | 1,500 | 15,000 | 150,000 | 750,000 |
| subscriptions | 800 | 8,000 | 80,000 | 400,000 |
| artists | 1,000 | 10,000 | 100,000 | 500,000 |
| albums | 3,000 | 30,000 | 300,000 | 1,500,000 |
| tracks | 10,000 | 100,000 | 1,000,000 | 10,000,000 |
| likes | 5,000 | 50,000 | 500,000 | 3,000,000 |
| follows | 2,000 | 20,000 | 200,000 | 1,000,000 |
| playbacks | 30,000 | 300,000 | 3,000,000 | 30,000,000 |
| search_histories | 3,000 | 30,000 | 300,000 | 1,000,000 |
| playlists | 1,000 | 10,000 | 100,000 | 500,000 |
| playlist_tracks | 5,000 | 50,000 | 500,000 | 3,000,000 |
| track_comments | 30,000 | 300,000 | 3,000,000 | 30,000,000 |
| artist_applications | 1,000 | 10,000 | 100,000 | 300,000 |
| album_applications | 2,000 | 20,000 | 200,000 | 600,000 |
| track_applications | 3,000 | 30,000 | 300,000 | 1,000,000 |
</details>

## 현재 integrated 데이터셋 범위

```text
users
wallets
point_histories
billing_keys
cash_orders
point_orders
payments
subscriptions
artists
albums
tracks
likes
follows
playbacks
search_histories
playlists
playlist_tracks
track_comments
artist_applications
album_applications
track_applications
```

## 현재 적재 방식

`LOAD DATA` 방식은 nullable `DATETIME` 처리에서 `0000-00-00 00:00:00` 문제가 발생할 수 있어 사용하지 않습니다.

```text
CSV 생성
→ PyMySQL batch insert
→ Python None을 MySQL NULL로 바인딩
→ 검증 SQL 실행
```

## 검증 기준

`validate.sql`은 전체 row count와 주요 FK 정합성을 확인합니다.

현재 검증 대상은 다음과 같습니다.

- `playlist_tracks.playlist_id` → `playlists.id`
- `playlist_tracks.track_id` → `tracks.id`
- `likes.user_id` → `users.id`
- `likes.target_id` → `tracks.id`
- `follows.artist_id` → `artists.id`
- `follows.user_id` → `users.id`
- `playbacks.playlist_id` → `playlists.id`
- `playbacks.track_id` → `tracks.id`
- `playbacks.user_id` → `users.id`
- `search_histories.user_id` → `users.id`

성공 기준은 모든 `invalid_count`가 `0`인 상태입니다.

## 주의사항

- `scripts/seed/output/`은 Git에 포함하지 않습니다.
- `clean.sql`은 로컬 seed 데이터 초기화 전용입니다.
- 실제 운영/공유 DB에서 실행하지 않습니다.
- 처음에는 반드시 `smoke`로 검증한 뒤 `test`, `local` 이상으로 증량합니다.
- DB 비밀번호 등 민감 정보는 루트 `.env`에만 둡니다.
- 행동 로그 데이터는 현재 엔티티/DB 컬럼 기준에 맞춰 생성하며, nullable 컬럼은 `insert.py`에서 `None`으로 변환합니다.

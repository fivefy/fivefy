# Seed Scripts

성능 검증용 seed 데이터 생성/적재/검증/초기화 스크립트입니다.

## 목적

- 대량 데이터 기반 성능 병목 분석
- 인덱스 적용 전후 비교
- 캐싱 적용 전후 비교
- 부하 테스트용 데이터 준비

현재는 `integrated` 데이터셋을 제공합니다.

`integrated` 데이터셋은 여러 파트 API 성능 검증에 사용할 수 있도록 사용자, 음악 컨텐츠, 댓글, 신청 데이터를 함께 생성하는 통합 seed 데이터셋입니다.

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

Python 패키지를 설치합니다.

```bash
python3 -m pip install -r scripts/seed/requirements.txt
```

DB 접속 정보는 프로젝트 루트의 `.env`를 사용합니다.

필수 값:

```env
DB_NAME=fivefy_db
DB_USER=root
DB_PASSWORD=your-password
DB_HOST=127.0.0.1
DB_PORT=3306
```

## 실행 명령

`run_seed.sh`는 아래 형식으로 실행합니다.

```bash
scripts/seed/run_seed.sh [command] [dataset] [scale] [seed]
```

기본값은 아래와 같습니다.

```text
command: run
dataset: integrated
scale: smoke
seed: 42
```

따라서 기본 실행은 `integrated smoke 42` 데이터셋 생성/적재/검증으로 동작합니다.

```bash
scripts/seed/run_seed.sh
```

명시적으로 실행할 수도 있습니다.

```bash
scripts/seed/run_seed.sh run integrated smoke 42
```

중간 규모 검증은 `test`를 사용합니다.

```bash
scripts/seed/run_seed.sh run integrated test 42
```

로컬 성능 분석용 데이터는 `local`을 사용합니다.

```bash
scripts/seed/run_seed.sh run integrated local 42
```

개발 서버 또는 발표용 부하 테스트 데이터는 `dev`를 사용합니다.

```bash
scripts/seed/run_seed.sh run integrated dev 42
```

## 초기화

기본 데이터셋을 초기화하려면 아래 명령어를 실행합니다.

```bash
scripts/seed/run_seed.sh clean
```

명시적으로 데이터셋을 지정할 수도 있습니다.

```bash
scripts/seed/run_seed.sh clean integrated
```

초기화 후 바로 다시 생성/적재/검증하려면 `reset`을 사용합니다.

```bash
scripts/seed/run_seed.sh reset integrated smoke 42
```

`local` 데이터셋을 초기화 후 다시 생성/적재/검증하려면 아래처럼 실행합니다.

```bash
scripts/seed/run_seed.sh reset integrated local 42
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

## 현재 integrated 데이터셋 범위

현재 `integrated` 데이터셋은 아래 테이블을 생성합니다.

```text
users
artists
albums
tracks
track_comments
artist_applications
album_applications
track_applications
```

추후 다른 파트 seed를 추가할 때도 같은 `integrated` 데이터셋 안에서 확장합니다.

예상 추가 대상:

```text
billing_keys
cash_orders
payments
point_orders
wallets
point_histories
subscriptions
playbacks
popular_charts
playlists
playlist_tracks
likes
follows
search_histories
notifications
recommendations
```

## 데이터셋 추가/확장 규칙

새로운 독립 데이터셋이 필요한 경우 아래 구조로 추가합니다.

```text
scripts/seed/datasets/{dataset_name}/
├── generate.py
├── insert.py
├── validate.sql
└── clean.sql
```

다만 같은 서비스 성능 검증 목적이라면 새 데이터셋을 만들기보다 `integrated` 데이터셋을 확장합니다.

## 중복 데이터 생성 규칙

- 이미 `integrated` 데이터셋이 생성하는 테이블은 다른 데이터셋에서 중복 생성하지 않습니다.
- 다른 데이터셋이 기존 데이터가 필요한 경우 `integrated`가 만든 id 범위를 참조합니다.
- 공통 기반 테이블이 복잡해지면 별도의 `shared` 데이터셋 분리를 검토합니다.
- 현재는 사용자와 음악 컨텐츠 기반 데이터가 여러 파트의 기준 데이터 역할을 합니다.

## 현재 적재 방식

`LOAD DATA` 방식은 nullable `DATETIME` 처리에서 `0000-00-00 00:00:00` 문제가 발생할 수 있어 사용하지 않습니다.

현재는 아래 흐름을 사용합니다.

```text
CSV 생성
→ PyMySQL batch insert
→ Python None을 MySQL NULL로 바인딩
→ 검증 SQL 실행
```

## 실행 흐름

`run_seed.sh run`은 아래 순서로 동작합니다.

```text
1. 루트 .env 로드
2. PyMySQL 설치 여부 확인
3. scripts/seed/output/ 삭제
4. CSV 생성
5. PyMySQL batch insert
6. validate.sql 실행
```

`run_seed.sh clean`은 아래 순서로 동작합니다.

```text
1. 루트 .env 로드
2. datasets/{dataset}/clean.sql 실행
3. scripts/seed/output/ 삭제
```

`run_seed.sh reset`은 아래 순서로 동작합니다.

```text
1. clean 실행
2. run 실행
```

## 주의사항

- `scripts/seed/output/`은 Git에 포함하지 않습니다.
- `clean.sql`은 로컬 seed 데이터 초기화 전용입니다.
- 실제 운영/공유 DB에서 실행하지 않습니다.
- 처음에는 반드시 `smoke`로 검증한 뒤 `test`, `local` 이상으로 증량합니다.
- DB 비밀번호 등 민감 정보는 루트 `.env`에만 둡니다.
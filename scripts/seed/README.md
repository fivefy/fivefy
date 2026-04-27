# Music Content Seed

음악 컨텐츠 도메인 성능 검증용 seed 데이터 생성/적재 스크립트입니다.

## 목적

- 공개 트랙 목록 조회
- 트랙 상세 조회
- 트랙 댓글 목록 조회
- 신청 목록 조회

위 API의 성능 병목 분석, 인덱스 적용 전후 비교, 캐싱/부하 테스트를 위한 대량 데이터를 생성합니다.

## 구조

```text
scripts/seed/
├── generate_music_content_seed.py
├── insert_music_content_seed.py
├── validate_music_content_seed.sql
├── clean_music_content_seed.sql
├── run_music_content_seed.sh
├── requirements.txt
├── README.md
└── output/               # 생성 CSV, Git 제외
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

## 실행 순서

처음에는 반드시 `smoke`로 검증합니다.

```bash
scripts/seed/run_music_content_seed.sh smoke 42
```

검증 후 로컬 성능 분석용 데이터를 생성합니다.

```bash
scripts/seed/run_music_content_seed.sh local 42
```

## 초기화

seed 데이터를 삭제하려면 아래 명령어를 실행합니다.

```bash
mysql -h 127.0.0.1 -u root -p fivefy_db < scripts/seed/clean_music_content_seed.sql
```

## Scale

| scale | 목적 |
|---|---|
| smoke | 스크립트/적재 흐름 검증 |
| test | local 실행 전 중간 규모 검증 |
| local | 로컬 성능 분석 |
| dev | 개발 서버 부하 테스트 |

## 주의사항

- `scripts/seed/output/`은 Git에 포함하지 않습니다.
- `clean_music_content_seed.sql`은 로컬 seed 데이터 초기화 전용입니다.
- 실제 운영/공유 DB에서 실행하지 않습니다.
- 처음에는 반드시 `smoke`로 검증한 뒤 `local` 이상으로 증량합니다.
- DB 비밀번호 등 민감 정보는 루트 `.env`에만 둡니다.

## 현재 적재 방식

`LOAD DATA` 방식은 nullable `DATETIME` 처리에서 `0000-00-00 00:00:00` 문제가 발생할 수 있어 사용하지 않습니다.

현재는 아래 흐름을 사용합니다.

```text
CSV 생성
→ PyMySQL batch insert
→ Python None을 MySQL NULL로 바인딩
→ 검증 SQL 실행
```

## 커밋 제외 대상

아래 디렉터리는 Git에 포함하지 않습니다.

```text
scripts/seed/output/
```
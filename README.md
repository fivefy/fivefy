# 🎵 음악 구독 서비스

> YouTube Music 을 벤치마킹한 **AI 기반 음악 스트리밍 구독 서비스**
> Spring Boot 4.0.5 · Java 17 · MySQL 8.4 · PostgreSQL + pgvector · Redis · Kafka 3.7 · Spring AI 2.0.0-M2 (Ollama · Claude)

---

## 📌 목차

- [프로젝트 소개](#-프로젝트-소개)
- [팀원 소개](#-팀원-소개)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [주요 기능](#-주요-기능)
- [ERD](#-erd)
- [API 문서](#-api-문서)
- [시작하기](#-시작하기)
- [브랜치 전략](#-브랜치-전략)
- [커밋 컨벤션](#-커밋-컨벤션)
- [트러블슈팅·기술 의사결정](#-트러블슈팅--기술-의사결정)

---

## 프로젝트 소개

> **Fivefy** 는 사용자의 취향과 상황에 맞는 음악 경험을 제공하는 **AI 기반 음악 구독 스트리밍 플랫폼**입니다.
단순한 음악 재생을 넘어 **AI 개인화 추천**, **자연어 무드 검색**, **추천 이유 설명 생성**, **실시간 인기 차트**, **플레이리스트 관리**, **구독·포인트 결제** 시스템을 함께 제공합니다.

**핵심 목표**

| 목표 | 내용 |
|---|---|
| 대용량 데이터 처리 | 트랙 **1억 건**, 재생 기록 월 **3,000만 건** |
| AI 기능 통합 | 추천, 자연어 검색, 이유 설명 생성 (Spring AI · Ollama · Claude) |
| 고가용성 | 목표 **RPS 1,000 / P95 200ms 이하** |

**개발 기간** : `2026.04` ~ `2026.05` (6주)

---

## 팀원 소개

| 이름  | 역할      | GitHub                                        |
|-----|---------|-----------------------------------------------|
| 곽현민 | 인증, 인프라 | [prAha1030](https://github.com/prAha1030)     |
| 나은총 | 음악 컨텐츠  | [popo2381](https://github.com/popo2381)       |
| 유지현 | 알림, 소셜  | [jihyeon1346](https://github.com/jihyeon1346) |
| 방효경 | 음악 활동   | [Banhklo2](https://github.com/Banhklo2)       |
| 이준석 | 구독, 결제  | [Perfect-Bee](https://github.com/Perfect-Bee) |

---

## 🛠️ 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.5-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI_2.0.0--M2-6DB33F?style=flat&logo=spring&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat&logo=spring&logoColor=white)
![Querydsl](https://img.shields.io/badge/Querydsl_7.1-0769AD?style=flat)
![JWT](https://img.shields.io/badge/JWT_0.13-000000?style=flat&logo=jsonwebtokens&logoColor=white)

### Database & Cache
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat&logo=mysql&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16_+_pgvector-336791?style=flat&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_8.6-DC382D?style=flat&logo=redis&logoColor=white)
![Redisson](https://img.shields.io/badge/Redisson_4.3-DC382D?style=flat)
![ShedLock](https://img.shields.io/badge/ShedLock_7.7-yellow?style=flat)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat&logo=flyway&logoColor=white)

### Message Queue
![Kafka](https://img.shields.io/badge/Apache_Kafka_3.7-231F20?style=flat&logo=apachekafka&logoColor=white)

### AI / Resilience
![Ollama](https://img.shields.io/badge/Ollama-000000?style=flat&logo=ollama&logoColor=white)
![Anthropic](https://img.shields.io/badge/Anthropic_Claude-D4A373?style=flat&logo=anthropic&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector_0.1.6-336791?style=flat)
![Resilience4j](https://img.shields.io/badge/Resilience4j_2.2-FF6B6B?style=flat)

### Infra & DevOps
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat&logo=amazonec2&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white)
![CloudFront](https://img.shields.io/badge/CloudFront-FF9900?style=flat&logo=amazonaws&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)
![CodeRabbit](https://img.shields.io/badge/CodeRabbit-FF6B35?style=flat)

### Payment
![PortOne](https://img.shields.io/badge/PortOne_V2-0080FF?style=flat)

### Docs & Test
![Spring REST Docs](https://img.shields.io/badge/Spring_REST_Docs-6DB33F?style=flat&logo=spring&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=flat&logo=junit5&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=flat&logo=k6&logoColor=white)

---

## 🏗️ 아키텍처

### 모듈러 모놀리스 구조

```
com.fivefy
 ├─ FivefyApplication.java
 │
 ├─ ai/                       # AI 모듈 (Spring AI · Ollama · Claude)
 │   ├─ controller / service / repository / domain / dto / enums
 │   ├─ job/                  # Spring Batch 잡 (임베딩 배치 등)
 │   └─ observability/        # Resilience4j Circuit Breaker / Retry / 메트릭
 │
 ├─ common/                   # 공통 인프라
 │   ├─ config / dto / entity / enums / exception / filter / initializer
 │   ├─ lock/                 # Redisson 분산락
 │   ├─ portone/              # PortOne V2 연동
 │   ├─ storage/              # AWS S3
 │   └─ util/
 │
 └─ domain/                   # 21개 도메인 모듈
     ├─ user
     ├─ artist · album · track       # 각 *Application 신청 흐름 포함
     ├─ playlist · playlisttrack
     ├─ playback · popularchart · recommendation
     ├─ search · chat · notification · follow · like
     ├─ subscription · payment · billingkey · billingattempt
     └─ wallet · pointorder · cashorder
```

### 전체 요청 흐름

```
[Client]
   │  JWT (Access 30분 / Refresh 7일) / OAuth2
   ▼
[EC2 — Spring Boot 4.0.5]
   ├─ user · 인증 ─────────── MySQL (Soft delete + 30일 익명화)
   ├─ search ──────────────── MySQL (ngram FULLTEXT 2-gram)
   ├─ track · application ─── MySQL (PESSIMISTIC_WRITE + Generated UNIQUE + app_id UNIQUE)
   ├─ playback · chart ────── MySQL + Spring Batch Snapshot
   ├─ track 상세 ──────────── Redis (정책 검증은 캐시 앞) ↔ MySQL fallback
   ├─ ai · recommendation ─── PostgreSQL pgvector ↔ Ollama / Claude
   │                            + Resilience4j Circuit Breaker / Retry
   ├─ subscription · payment ─ PortOne V2 (빌링키) + Redisson 분산락
   ├─ wallet · point · cash ── MySQL (포인트 / 캐시 분리)
   └─ notification
         ├─ 단건: @TransactionalEventListener(AFTER_COMMIT)
         │         → Outbox(MySQL) → Worker(@SchedulerLock) → SSE
         └─ 대량: Kafka (3-broker, RF=2)
                 ├─ Topics: notification.send · playback.event (3 partitions)
                 └─ 다중 Consumer → bulk INSERT + SSE
                                ↓
                       [AWS S3] 음원 · Presigned URL · CloudFront
```

---

## ✨ 주요 기능

### 🔐 인증

- **JWT 이중 토큰** : Access Token 30분 + Refresh Token 7일 (Stateless · 수평 확장 대응)
- **회원 탈퇴 2단계** : 즉시 차단 → 30일 후 익명화 배치 (개인정보 보호법 + 서비스 데이터 보존 균형)
- **탈퇴 시 비밀번호 BCrypt 재확인** : 토큰 탈취 공격 차단

### 🎵 스트리밍

- **상태 전이 기반 재생 모델** : `PLAYING / PAUSED / STOPPED / SKIPPED / COMPLETED`
- **시간 필드 분리** : `startedAt` / `lastPlayedAt` / `endedAt` — 실제 재생만 누적 (PAUSED 시간 제외)
- **연속 재생 세션** : `sessionId` + `playlistId` 로 끊김 없는 재생 흐름 표현
- **음원 배포** : AWS S3 Presigned URL + CloudFront (audioKey 기반)

### 🎙️ 아티스트 · 앨범 · 트랙 관리

- **신청 ↔ 운영 테이블 분리** : `*Application` 패키지로 상태 전이 (PENDING → APPROVED / REJECTED) 명확화
- **Application 다층 방어선** :
    - 1차: `PESSIMISTIC_WRITE` (`findByIdForUpdate`) — 승인/거절 동시성
    - 2차: Generated Column + UNIQUE — 신청 row 중복 (TOCTOU)
    - 최종: `application_id` UNIQUE — 엔티티 중복 생성 방지
- **검증 결과** : 동시 10건 요청 → 1건 201 CREATED / 9건 409 CONFLICT / DB row 1건 / 500 응답 0건

### 📈 인기차트

- **유효 재생 기준** : `playedDuration ≥ 30초` + 종료 상태(`STOPPED`/`SKIPPED`/`COMPLETED`) + 세션 중복 제거
- **주간 Snapshot** : 매 요청 실시간 계산이 아닌 사전 집계 (조회 시 단순 read only)
- **Projection 기반 집계** + DB LIMIT 적용 — Entity 생성 최소화
- **"데이터 없음" 상태 표현** : snapshot 먼저 삭제 후 결과 처리

### 💳 포인트 · 결제

- **PortOne V2 + KakaoPay** 정기결제 (빌링키 기반)
- **Redisson 분산락** 으로 중복 결제 방지
- **결제 도메인 4분할** : `billingkey` / `billingattempt` / `cashorder` / `pointorder` / `wallet`
- **스케줄러** : 매월 1일 08시 충전 / 09시 정기결제 (ShedLock 다중 인스턴스 락)

### 🎧 플레이리스트

- **Soft delete + 30일 cleanup scheduler** (ShedLock)
- **활성 데이터 기준 unique** : `(user_id, title, deleted)` 복합 unique
- **부분 재정렬** : 영향 범위만 update + 임시 음수 position 으로 unique 충돌 회피
- **LexoRank / LinkedList 검토 후 보류** — 적정 기술 선택

### 🔔 알림

- **3단계 진화** :
    1. `@TransactionalEventListener(AFTER_COMMIT) + @Async` — 정합성 확보
    2. **Outbox 패턴** — DB 영속화로 메모리 큐 유실 해소 (단건)
    3. **Kafka 청크 fan-out** — 대량 알림 처리량 확보 (10만 팔로워 알림 118초 → 12초)
- **통신 방식 분리** : 알림은 단방향 **SSE**, 채팅은 양방향 **WebSocket**

### 🔎 검색

- **MySQL ngram FULLTEXT INDEX (2-gram)** — `LIKE '%키워드%'` 풀스캔 회피
- Hibernate 7 QueryDSL 의 `MATCH AGAINST` 미지원 우회 — EntityManager 네이티브 쿼리
- Elasticsearch 는 MVP 이후로 보류

### 👍 좋아요 · 팔로우

- **다형성 좋아요** : Track / Album 공통 처리 (`targetId + targetType`)
- **고아 객체 정리** : `TrackDeletedEvent` / `AlbumDeletedEvent` 이벤트 기반 + `@TransactionalEventListener` 롤백 보장

### 🤖 AI 추천

- **재생 이력 기반 트랙 추천** (Spring AI)
- **자연어 검색** : 사용자 입력 → 임베딩 → pgvector 유사도 검색
- **추천 이유 설명 생성** : Anthropic Claude
- **임베딩 멱등성** : `source_hash` (SHA-256) 기반 변경 감지로 외부 API 호출 99% 감소


---

## ERD

<details>
<summary>ERD 이미지 보기</summary>

<img width="800" height="600" alt="Spring 최종 프로젝트" src="https://github.com/user-attachments/assets/f72c54bf-8ac5-40cb-9135-a0649d8c6d48" />

</details>

<details>
<summary>테이블 목록 보기</summary>

| 카테고리 | 테이블 | 설명 |
|---|---|---|
| **유저** | `users` | 회원 정보 (soft delete + 익명화) |
| **아티스트** | `artists` / `artist_applications` | 아티스트 프로필 / 등록 신청 흐름 |
| **앨범** | `albums` / `album_applications` | 앨범 정보 / 등록 신청 흐름 |
| **트랙** | `tracks` / `track_applications` | 트랙 정보 / 등록 신청 흐름 (FREE_CREATION / OFFICIAL_RELEASE) |
| **검색** | (FULLTEXT INDEX) | `ft_artist_name`, `ft_track_title`, `ft_album_title` (ngram) |
| **재생** | `playbacks` | 상태 전이 재생 이벤트 (sessionId + playlistId) |
| **인기차트** | `popular_chart_snapshots` | 주간 Top100 |
| **플레이리스트** | `playlists` / `playlist_tracks` | 유저 플레이리스트 (소프트 삭제, 부분 재정렬) |
| **좋아요** | `likes` | 다형성 좋아요 (`targetId + targetType`) |
| **댓글** | `track_comments` | 트랙 댓글 (`(track_id, deleted_at, created_at DESC)` 복합 인덱스) |
| **팔로우** | `follows` | 아티스트 팔로우 |
| **알림** | `notifications` / `notification_outbox` | 알림 / Outbox 패턴 영속화 |
| **임베딩** | `track_embedding` | pgvector 임베딩 + `source_hash` + `model_version` |
| **구독** | `subscriptions` | 구독 플랜 및 상태 |
| **결제** | `payments` / `billing_keys` / `billing_attempts` | 결제 / 빌링키 / 시도 이력 |
| **포인트** | `wallets` / `point_orders` / `cash_orders` | 지갑 / 포인트 주문 / 캐시 충전 주문 |
| **검색** | `search_histories` | 유저 검색 이력 |
| **추천** | `recommendations` | AI 추천 결과 |
| **채팅** | `chats` | AI 챗봇 답변 스트리밍 (SSE) |

</details>

---

## API 문서

> Spring REST Docs 기반 API 문서 (배포 후 링크 추가 예정)

---

## 시작하기

### 사전 요구사항

- JDK 21
- Docker & Docker Compose
- MySQL 8.4
- PostgreSQL 16 (벡터 DB)
- RabbitMQ

### 환경 설정

```bash
# 1. 레포지토리 클론
git clone https://github.com/fivefy/fivefy.git
cd fivefy

# 2. 환경변수 설정
touch .env
touch application-local.yml
# .env, application-local.yml 파일에 실제 값 입력

# 3. 인프라 컨테이너 실행 (Kafka + Redis)
docker compose up -d

# 4. Kafka 토픽 생성 (최초 1회)
docker exec kafka-1 kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic notification.send --partitions 3 --replication-factor 2
docker exec kafka-1 kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic playback.event --partitions 3 --replication-factor 2

# 5. DB 초기화 (Flyway 가 마이그레이션 자동 적용)
#    필요 시 seed 데이터:
#    smoke / test / local / dev scale 분리되어 있음

# 6. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 실행 포트

| 서비스 | 포트 |
|---|---|
| Spring Boot | 8080 |
| Kafka Broker 1 | 9092 |
| Kafka Broker 2 | 9093 |
| Kafka Broker 3 | 9094 |
| Kafka UI | 8088 |
| Redis | 6379 |
| RedisInsight | 5540 |
| MySQL | 3306 |
| PostgreSQL (vector) | 5432 |
 
---

## 브랜치 전략

```
main ← stage ← dev ← feature/{도메인}-{기능명}
```

| 브랜치 | 용도 | PR 승인 요건 |
|---|---|---|
| `main` | 프로덕션 배포 | 전원 승인 |
| `stage` | 스테이징 배포 | 3인 이상 |
| `dev` | 개발 통합 | 1인 이상 + CI 통과 |
| `feature/{도메인}-{기능명}` | 기능 개발 | — |

### CI / CD 파이프라인

- **CI** (`.github/workflows/ci.yml`) — PR → `dev/stage/main` 트리거
  - 서비스 컨테이너 : Redis 7.4 + MySQL 8.4 + PostgreSQL 16
  - JDK 21 Temurin + Gradle 캐시 + `./gradlew build`
  - 실패 시 PR 자동 코멘트 (`❌ 빌드/테스트 실패`)
  - Test report 아티팩트 업로드
- **CD** (`.github/workflows/cd.yml`) — 배포 자동화
- **CodeRabbit** (`.coderabbit.yml`) — 자동 코드 리뷰 (다층 방어선·인덱스 정책의 출발점)


---

## 커밋 컨벤션

```
<타입>: <제목>

[본문 — 선택]
```

| 타입         | 설명            |
|------------|---------------|
| `feat`     | 새로운 기능 추가     |
| `fix`      | 버그 수정         |
| `docs`     | 문서 수정         |
| `init`     | 초기 세팅         |
| `refactor` | 코드 리팩터링       |
| `test`     | 테스트 코드 추가·수정  |
| `chore`    | 기타 작업         |

**예시**
```
feat: 트랙 재생 권한 체크 Redis 캐싱 적용

- permission:{userId}:{trackId} 키로 TTL 5분 캐싱
- 캐시 미스 시 구독 상태 DB 조회 후 캐싱
- Redis 장애 시 DB 직접 조회 Fallback 처리
```

---

## 🚨 트러블슈팅 · 기술 의사결정

> 개발 기간 동안 정리한 주요 의사결정·트러블슈팅 기록입니다.

### 알림 시스템

| 문제 | 해결 |
|---|---|
| `@Async` 메모리 큐로 인한 알림 유실 (서버 재시작 / 스레드풀 포화 / 리스너 예외) | **Outbox 패턴 도입** — 비즈니스 트랜잭션과 같은 트랜잭션으로 PENDING 영속화, ShedLock + REQUIRES_NEW + 1분 후 retry / 최대 3회 |
| 10만 팔로워 대상 발매 알림 단일 워커로 ≈17분 예상 | **RabbitMQ 청크 fan-out** ([실제 구현] Kafka 로 전환) — 청크 단위 다중 Consumer 병렬 처리 |
| `JdbcTemplate.batchUpdate` 가 명목상 batch 였던 함정 (단건 INSERT 1,000번) | **`rewriteBatchedStatements=true`** MySQL JDBC 옵션 활성화 — 단일 round-trip multi-value INSERT 로 청크 처리 시간 6.7배 단축 |
| Page 기반 페이지네이션의 불필요한 count 쿼리 | **Slice 기반 전환** — `LIMIT + 1` 전략 |

**성능 개선** : 10만 팔로워 알림 **118초 → 12초 (약 10배 단축)**

### 임베딩 배치

| 문제 | 해결 |
|---|---|
| 매일 새벽 100% 트랙 Ollama 재호출 (일 변경률 1% 미만) | **`source_hash`** **SHA-256 멱등성** — 동일 hash 면 외부 호출 스킵 |
| Hash 조회 N+1 (청크 100건 마다 SELECT 100회) | **IN 절 일괄 조회** — `findHashesByTrackIds` (SELECT 100 → 1) |
| UPSERT 안전장치 부재 | `WHERE source_hash <> EXCLUDED.source_hash` DB 레벨 이중 필터 |
| 외부 API 호출이 `@Transactional` 내부에 있어 HikariCP 풀 100% 점유 | **Service ↔ PersistService 빈 분리** — `@Transactional` self-invocation 함정 회피, "외부 호출은 트랜잭션 밖, DB 작업만 트랜잭션 안" 원칙 |

**성능 개선** : 1,000곡 임베딩 배치 **23,190ms → 336ms (약 69배 단축)**

### 데이터 무결성

| 문제 | 해결 |
|---|---|
| `deletedAt` 만으로 soft delete 시 동일 제목 재생성 unique 충돌 | **`deleted` 컬럼 분리** + `(user_id, title, deleted)` 복합 unique — 활성 데이터끼리만 중복 금지 |
| Application 동시 승인 시 엔티티 중복 생성 위험 | **`PESSIMISTIC_WRITE`** 승인/거절 경로에만 `findByIdForUpdate` |
| Application 생성 시 TOCTOU (CodeRabbit 지적) | **Generated Column + UNIQUE** + `saveAndFlush` 로 즉시 unique 충돌 감지 |
| 승인 결과 엔티티 중복 생성 가능성 | **`application_id` UNIQUE** 최종 방어선 |
| Like 다형성 구조로 FK CASCADE 적용 불가 | **이벤트 기반 처리** (`TrackDeletedEvent` / `AlbumDeletedEvent`) + `@TransactionalEventListener` 롤백 보장 |

**검증** : 동시 10건 요청 → 1건 201 / 9건 409 / DB row 1건 / 500 응답 0건

### 도메인 모델링

| 문제 | 해결 |
|---|---|
| 행동 중심 enum (`START / PAUSE / SKIP`) 으로 행동/상태 혼재 | **상태 중심 enum 재설계** (`PLAYING / PAUSED / STOPPED / SKIPPED / COMPLETED`) — "Playback 한 건이 무엇을 의미하는가" 부터 재정의 |
| 단순 `playback count` 로 인기 차트 신뢰도 문제 (짧은 재생·반복·비정상 종료 포함) | **유효 재생 기준** (`playedDuration ≥ 30초` + 종료 상태 + 세션 중복 제거) + **주간 Snapshot** |
| Snapshot 결과 없음을 표현하지 못함 | snapshot 먼저 삭제 후 결과 처리하도록 순서 변경 |
| 전체 재정렬로 인한 PlaylistTrack 불필요한 update | **부분 재정렬** + 임시 음수 position 으로 unique 충돌 회피 (LexoRank / LinkedList 검토 후 보류) |

### 캐시 · 검색 · 인덱스

| 문제 | 해결 |
|---|---|
| `containsIgnoreCase` → `LIKE '%키워드%'` 풀스캔 | **MySQL ngram FULLTEXT (2-gram)** — `홍길동 → "홍길"/"길동"` B-Tree 적중 |
| 트랙 상세 캐시 hit 시 정식 발매 공개 검증 우회 | **정책 검증을 캐시보다 앞으로** — loader 에는 데이터 구성 책임만 |
| Redis 장애가 트랙 상세 API 실패로 전파 | **모든 캐시 실패를 cache miss 폴백** (Redis get/역직렬화/set/delete 실패 모두 처리) |
| 댓글 목록 502ms 응답 (filesort) | **`(track_id, deleted_at, created_at DESC)` 복합 인덱스** — Using filesort 제거 |
| 인덱스 추가가 오히려 응답 악화 (관리자 신청 목록 690ms) | **인덱스 미채택** — EXPLAIN 개선 ≠ 응답속도 개선 |
| 운영 환경 인덱스 마이그레이션 락 위험 | **`ALGORITHM=INPLACE, LOCK=NONE`** + PR 본문에 롤백 SQL 명시 |

**성능 개선** : 트랙 댓글 목록 **502ms → 19.5ms (약 25배 단축)**

### 인증 · 회원

| 문제 | 해결 |
|---|---|
| Session 의 수평 확장 한계 vs JWT 의 즉시 무효화 어려움 | **JWT 이중 토큰** (Access 30분 / Refresh 7일) — Stateless + 짧은 만료 보완 |
| 회원 탈퇴 시 개인정보 보호 ↔ 서비스 데이터 보존 균형 | **Soft delete + 30일 후 익명화 2단계** — 재생 기록·댓글 은 익명 상태로 유지 |
| 토큰 탈취 시 계정 탈퇴 공격 가능성 | **탈퇴 시 비밀번호 BCrypt 재확인** |

---

Made with 5️⃣ by Fivefy

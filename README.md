# 🎵 음악 구독 서비스

> YouTube Music 벤치마킹 음악 스트리밍 구독 서비스  
> Spring Boot 4.0 · MySQL 8.4 · Redis 8.6 · Kafka 3.7 · Java 17/JDK 21

---

## 📌 목차

- [프로젝트 소개](#프로젝트-소개)
- [팀원 소개](#팀원-소개)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [주요 기능](#주요-기능)
- [ERD](#erd)
- [API 문서](#api-문서)
- [시작하기](#시작하기)
- [브랜치 전략](#브랜치-전략)
- [커밋 컨벤션](#커밋-컨벤션)
- [트러블슈팅](#트러블슈팅)

---

## 프로젝트 소개

> 작성 예정

**핵심 목표**

| 목표 | 내용 |
|---|---|
| 대용량 데이터 처리 | 트랙 1억 건, 재생 기록 월 3,000만 건 |
| AI 기능 통합 | 추천, 자연어 검색, 이유 설명 생성 |
| 고가용성 | 목표 RPS 1,000 / P95 200ms 이하 |

**개발 기간** : `2026.04` ~ `2026.05` (6주)

---

## 팀원 소개

| 이름  | 역할 | GitHub |
|-----|----|---|
| 곽현민 | -  | - |
| 나은총 | -  | - |
| 유지현 | -  | - |
| 방효경 | -  | - |
| 이준석 | -  | - |

---

## 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.1-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI_1.1.x-6DB33F?style=flat&logo=spring&logoColor=white)

### Database & Cache
![MySQL](https://img.shields.io/badge/MySQL_8.4-4479A1?style=flat&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_8.6-DC382D?style=flat&logo=redis&logoColor=white)

### Message Queue
![Kafka](https://img.shields.io/badge/Apache_Kafka_3.7-231F20?style=flat&logo=apachekafka&logoColor=white)

### Infra & DevOps
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=flat&logo=amazonec2&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat&logo=amazons3&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)

### Docs
![Spring REST Docs](https://img.shields.io/badge/Spring_REST_Docs-6DB33F?style=flat&logo=spring&logoColor=white)

---

## 아키텍처

> 아키텍처 다이어그램 첨부 예정

```
[Client]
   │
   ▼
[EC2 — Spring Boot 4.0]
   ├── Redis 8.6         (캐싱 · JWT RT · 재생수 집계)
   ├── Kafka 3.7 Cluster (알림 비동기 · 재생수 파이프라인)
   └── MySQL 8.4         (EC2 직접 설치)
         └── S3           (음원 파일 · Presigned URL)
```

---

## 주요 기능

### 인증
- 내용 추가 예정

### 스트리밍
- 내용 추가 예정

### 아티스트 · 앨범 · 트랙 관리
- 내용 추가 예정

### 인기차트
- 내용 추가 예정

### 포인트
- 내용 추가 예정

### 플레이리스트
- 내용 추가 예정

### 알림
- 내용 추가 예정

### 검색
- 내용 추가 예정

### 좋아요 · 팔로우
- 내용 추가 예정

### AI 추천
- 재생 이력 기반 트랙 추천 (Spring AI)
- 자연어 검색
- 추천 이유 설명 생성

---

## ERD

> ERD 이미지 첨부 예정

<details>
<summary>테이블 목록 보기</summary>

| 테이블 | 설명 |
|---|---|
| 유저 | 회원 정보 |
| 아티스트 | 아티스트 프로필 |
| 아티스트 등록 요청 | 아티스트 등록 승인 요청 |
| 앨범 | 앨범 정보 |
| 앨범 등록 요청 | 앨범 등록 승인 요청 |
| 트랙 | 트랙 정보 |
| 트랙 등록 요청 | 트랙 등록 승인 요청 |
| 구독 | 구독 플랜 및 상태 |
| 재생 | 재생 이벤트 기록 |
| 인기순위 | 주간 차트 스냅샷 |
| 지갑 | 유저별 포인트 잔액 |
| 포인트 이력 | 포인트 변경 이력 |
| 주문 | 결제 주문 |
| 결제 | PG사 결제 정보 |
| 플레이리스트 | 유저 플레이리스트 |
| 플레이 리스트 트랙 | 플레이리스트 수록 트랙 |
| 좋아요 | 트랙·앨범 좋아요 |
| 댓글 | 트랙 댓글 |
| 팔로우 | 아티스트 팔로우 |
| 알림 | 푸시·이메일·인앱 알림 |
| 검색기록 | 유저 검색 이력 |
| 추천 | AI 추천 결과 |

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

### 환경 설정

```bash
# 1. 레포지토리 클론
git clone https://github.com/{organization}/{repository}.git
cd {repository}

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

# 5. DB 초기화
mysql -u root -p music_service < src/main/resources/db/dummy/dummy_data.sql

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

---

## 브랜치 전략

```
main ← stage ← dev ← feature/{도메인}-{기능명}
```

| 브랜치                   | 용도 |
|-----------------------|---|
| `main`                | 프로덕션 배포 |
| `stage`               | 스테이징 배포 |
| `dev`                 | 개발 통합 |
| `feature/{도메인}-{기능명}` | 기능 개발 |

**PR 규칙**
- `feature` → `dev` : CI (빌드 + 테스트) 통과, 코드 리뷰 1인 이상 승인 필수
- `dev` → `stage` : 코드 리뷰 3인 이상 승인 필수
- `stage` → `main` : 코드 리뷰 전원 승인 필수

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

## 트러블슈팅

> 개발 중 발생한 문제와 해결 과정을 기록합니다.

<details>
<summary>작성 예시</summary>

### [문제 제목]

**문제 상황**
> 어떤 상황에서 어떤 문제가 발생했는지 기술

**원인 분석**
> 원인을 어떻게 파악했는지 기술

**해결 방법**
> 어떻게 해결했는지 기술

**참고 자료**
> 관련 링크

</details>

---

Made with 5️⃣ by Fivefy

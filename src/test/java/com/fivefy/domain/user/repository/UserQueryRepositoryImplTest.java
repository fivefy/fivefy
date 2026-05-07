package com.fivefy.domain.user.repository;

import com.fivefy.common.config.flyway.FlywayConfig;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// 여기서 직접 Flyway 자동 설정을 끄면 YAML 설정보다 우선합니다.
@Import({UserQueryRepositoryImplTest.QueryDslTestConfig.class, FlywayConfig.class})
class UserQueryRepositoryImplTest {

    @TestConfiguration
    static class QueryDslTestConfig {
        @Bean
        public JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User createUser(String email, String name) {
        User user = User.create(email, "encodedPassword", name);
        return userRepository.save(user);
    }

    private User createDeletedUser(String email, String name, LocalDateTime deletedAt) {
        User user = createUser(email, name);
        ReflectionTestUtils.setField(user, "status", UserStatus.DELETED);
        ReflectionTestUtils.setField(user, "deletedAt", deletedAt);
        return userRepository.saveAndFlush(user);
    }

    private void updateCreatedAt(User user, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE users SET created_at = ? WHERE id = ?",
                createdAt, user.getId()
        );
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("anonymizeDeletedUsers — 개인정보 익명화")
    class AnonymizeDeletedUsers {

        @Test
        @DisplayName("탈퇴 후 30일 경과 유저 익명화 성공")
        void anonymizeSuccess() {
            // given — 31일 전 탈퇴 유저
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            User deleted = createDeletedUser(
                    "target@test.com", "탈퇴예정",
                    LocalDateTime.now().minusDays(31)
            );

            // when
            int result = userRepository.anonymizeDeletedUsers(threshold);

            // then
            em.flush();
            em.clear();

            assertThat(result).isEqualTo(1);

            User anonymized = userRepository.findById(deleted.getId()).orElseThrow();
            assertThat(anonymized.getEmail()).isEqualTo("deleted_" + deleted.getId() + "@deleted.com");
            assertThat(anonymized.getName()).isEqualTo("탈퇴한 유저");
            assertThat(anonymized.getPassword()).isEqualTo("DELETED");
        }

        @Test
        @DisplayName("탈퇴 후 30일 미경과 유저는 익명화하지 않음")
        void skipRecentDeletedUser() {
            // given — 29일 전 탈퇴 유저
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            createDeletedUser(
                    "recent@test.com", "최근탈퇴",
                    LocalDateTime.now().minusDays(29)
            );

            // when
            int result = userRepository.anonymizeDeletedUsers(threshold);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("이미 익명화된 유저는 재처리하지 않음 (email LIKE 'deleted_%')")
        void skipAlreadyAnonymizedUser() {
            // given — 이미 익명화된 유저
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            User alreadyAnonymized = createDeletedUser(
                    "deleted_999@deleted.com", "탈퇴한 유저",
                    LocalDateTime.now().minusDays(31)
            );
            ReflectionTestUtils.setField(alreadyAnonymized, "password", "DELETED");
            userRepository.saveAndFlush(alreadyAnonymized);

            // when
            int result = userRepository.anonymizeDeletedUsers(threshold);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("ACTIVE 유저는 익명화 대상에서 제외")
        void skipActiveUser() {
            // given — ACTIVE 유저 (deletedAt 없음)
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            createUser("active@test.com", "활성유저");

            // when
            int result = userRepository.anonymizeDeletedUsers(threshold);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("복수 대상 — 경과 유저만 익명화")
        void anonymizeOnlyExpiredUsers() {
            // given
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            // 익명화 대상 2명
            createDeletedUser("expired1@test.com", "탈퇴1", LocalDateTime.now().minusDays(31));
            createDeletedUser("expired2@test.com", "탈퇴2", LocalDateTime.now().minusDays(45));

            // 익명화 제외 1명 (29일)
            createDeletedUser("recent@test.com", "최근탈퇴", LocalDateTime.now().minusDays(29));

            // when
            int result = userRepository.anonymizeDeletedUsers(threshold);

            // then
            assertThat(result).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("updateLastActiveAt — lastActiveAt 갱신")
    class UpdateLastActiveAt {

        @Test
        @DisplayName("userId 기준으로 lastActiveAt 갱신 성공")
        void updateSuccess() {
            // given
            User user = createUser("test@test.com", "테스트");
            LocalDateTime newLastActiveAt = LocalDateTime.now().minusHours(1);

            // when
            userRepository.updateLastActiveAt(user.getId(), newLastActiveAt);
            em.clear();

            // then
            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getLastActiveAt())
                    .isEqualToIgnoringNanos(newLastActiveAt);
        }

        @Test
        @DisplayName("존재하지 않는 userId는 업데이트 안 함")
        void skipNotFoundUser() {
            // when
            userRepository.updateLastActiveAt(999L, LocalDateTime.now());

            // then — 예외 없이 처리, 영향받은 행 없음
            assertThat(userRepository.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("여러 번 호출 시 최신 값으로 덮어쓰기")
        void overwriteWithLatestValue() {
            // given
            User user = createUser("test@test.com", "테스트");
            LocalDateTime first  = LocalDateTime.now().minusHours(2);
            LocalDateTime second = LocalDateTime.now().minusHours(1);

            // when
            userRepository.updateLastActiveAt(user.getId(), first);
            userRepository.updateLastActiveAt(user.getId(), second);
            em.clear();

            // then
            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getLastActiveAt())
                    .isEqualToIgnoringNanos(second);
        }
    }

    @Nested
    @DisplayName("suspendInactiveUsers — 미접속 유저 정지")
    class SuspendInactiveUsers {

        private LocalDateTime threshold;

        @BeforeEach
        void setUp() {
            threshold = LocalDateTime.now().minusDays(30);
        }

        @Test
        @DisplayName("30일 미접속 + 가입 30일 경과 ACTIVE 유저 → SUSPENDED 처리")
        void suspendSuccess() {
            // given — 31일 전 가입, 31일 미접속
            User target = createUser("target@test.com", "정지대상");
            ReflectionTestUtils.setField(target, "lastActiveAt", LocalDateTime.now().minusDays(31));
            userRepository.saveAndFlush(target);
            updateCreatedAt(target, LocalDateTime.now().minusDays(31));

            // when
            int result = userRepository.suspendInactiveUsers(
                    threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED);

            em.clear();

            // then
            assertThat(result).isEqualTo(1);
            User suspended = userRepository.findById(target.getId()).orElseThrow();
            assertThat(suspended.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("30일 미접속이지만 가입 30일 미경과 신규 유저는 정지 제외")
        void skipNewUser() {
            // given — 29일 전 가입, lastActiveAt 없음
            User newUser = createUser("new@test.com", "신규유저");
            updateCreatedAt(newUser, LocalDateTime.now().minusDays(29));

            // when
            int result = userRepository.suspendInactiveUsers(
                    threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("최근 접속 유저는 정지 제외 (lastActiveAt이 threshold 이후)")
        void skipRecentActiveUser() {
            // given — 31일 전 가입, 1일 전 접속
            User active = createUser("active@test.com", "활성유저");
            ReflectionTestUtils.setField(active, "lastActiveAt", LocalDateTime.now().minusDays(1));
            userRepository.saveAndFlush(active);
            updateCreatedAt(active, LocalDateTime.now().minusDays(31));

            // when
            int result = userRepository.suspendInactiveUsers(
                    threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED);

            // then
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("BANNED 유저는 정지 처리 안 함 (fromStatus=ACTIVE 조건)")
        void skipBannedUser() {
            // given
            User banned = createUser("banned@test.com", "제재유저");
            ReflectionTestUtils.setField(banned, "status", UserStatus.BANNED);
            userRepository.saveAndFlush(banned);
            updateCreatedAt(banned, LocalDateTime.now().minusDays(31));

            // when
            int result = userRepository.suspendInactiveUsers(
                    threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED);

            // then
            assertThat(result).isEqualTo(0);
            User still = userRepository.findById(banned.getId()).orElseThrow();
            assertThat(still.getStatus()).isEqualTo(UserStatus.BANNED);
        }

        @Test
        @DisplayName("복수 대상 — 조건 충족 유저만 정지")
        void suspendOnlyEligibleUsers() {
            // given
            // 정지 대상 2명
            User t1 = createUser("t1@test.com", "대상1");
            User t2 = createUser("t2@test.com", "대상2");
            ReflectionTestUtils.setField(t1, "lastActiveAt", LocalDateTime.now().minusDays(31));
            ReflectionTestUtils.setField(t2, "lastActiveAt", LocalDateTime.now().minusDays(31));
            userRepository.saveAndFlush(t1);
            userRepository.saveAndFlush(t2);
            updateCreatedAt(t1, LocalDateTime.now().minusDays(31));
            updateCreatedAt(t2, LocalDateTime.now().minusDays(31));

            // 제외 대상 — 신규 유저
            User newUser = createUser("new@test.com", "신규");
            updateCreatedAt(newUser, LocalDateTime.now().minusDays(10));

            // when
            int result = userRepository.suspendInactiveUsers(
                    threshold, UserStatus.ACTIVE, UserStatus.SUSPENDED);

            // then
            assertThat(result).isEqualTo(2);
        }
    }
}

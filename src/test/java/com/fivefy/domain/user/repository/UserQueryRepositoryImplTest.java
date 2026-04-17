package com.fivefy.domain.user.repository;

import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(UserQueryRepositoryImplTest.QueryDslTestConfig.class)
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

}
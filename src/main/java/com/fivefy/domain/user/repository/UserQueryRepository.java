package com.fivefy.domain.user.repository;

import com.fivefy.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public interface UserQueryRepository {

    // 탈퇴 후 30일 경과 유저 개인정보 익명화
    int anonymizeDeletedUsers(LocalDateTime threshold);

    // Redis → DB lastActiveAt 반영
    void updateLastActiveAt(Long userId, LocalDateTime lastActiveAt);

    // 30일 미접속 유저 SUSPENDED 처리
    int suspendInactiveUsers(LocalDateTime threshold, UserStatus fromStatus, UserStatus toStatus);
}

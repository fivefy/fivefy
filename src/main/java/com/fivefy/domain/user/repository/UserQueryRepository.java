package com.fivefy.domain.user.repository;

import java.time.LocalDateTime;

public interface UserQueryRepository {

    // 탈퇴 후 30일 경과 유저 개인정보 익명화
    int anonymizeDeletedUsers(LocalDateTime threshold);
}

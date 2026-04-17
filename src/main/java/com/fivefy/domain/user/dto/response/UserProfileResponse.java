package com.fivefy.domain.user.dto.response;

import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.enums.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String email,
        String name,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}

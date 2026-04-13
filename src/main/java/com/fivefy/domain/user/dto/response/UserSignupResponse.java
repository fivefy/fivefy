package com.fivefy.domain.user.dto.response;

import com.fivefy.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserSignupResponse(
        Long userId,
        String name,
        String email,
        LocalDateTime createdAt
) {
    public static UserSignupResponse from(User user) {
        return new UserSignupResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}

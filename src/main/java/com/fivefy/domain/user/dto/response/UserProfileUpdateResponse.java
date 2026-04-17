package com.fivefy.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileUpdateResponse(
        String name,
        LocalDateTime updatedAt
) {
    public static UserProfileUpdateResponse from(String name, LocalDateTime updatedAt) {
        return new UserProfileUpdateResponse(name, updatedAt);
    }
}

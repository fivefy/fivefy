package com.fivefy.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record UserLoginResponse(
        String accessToken,
        String refreshToken
) {
    public static UserLoginResponse of(String accessToken, String refreshToken) {
        return new UserLoginResponse(accessToken, refreshToken);
    }

    public static UserLoginResponse from(String accessToken) {
        return new UserLoginResponse(accessToken, null);
    }
}

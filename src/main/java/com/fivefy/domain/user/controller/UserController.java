package com.fivefy.domain.user.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.user.dto.request.UserLoginRequest;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserLoginResponse;
import com.fivefy.domain.user.dto.response.UserProfileResponse;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/users/signup")
    public ResponseEntity<BaseResponse<UserSignupResponse>> signupUser(@Valid @RequestBody UserSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.success(HttpStatus.CREATED, "회원가입 성공", userService.signupUser(request))
        );
    }

    @PostMapping("/users/login")
    public ResponseEntity<BaseResponse<UserLoginResponse>> loginUser(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse result = userService.loginUser(request);
        ResponseCookie cookie = buildRefreshTokenCookie(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(BaseResponse.success(HttpStatus.OK, "로그인 성공", UserLoginResponse.from(result.accessToken())));
    }

    @PostMapping("/users/reissue")
    public ResponseEntity<BaseResponse<UserLoginResponse>> reissueToken(@CookieValue(name = "refreshToken") String refreshToken) {
        UserLoginResponse result = userService.reissueToken(refreshToken);
        ResponseCookie cookie = buildRefreshTokenCookie(result.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(BaseResponse.success(HttpStatus.OK, "토큰 재발급 성공", UserLoginResponse.from(result.accessToken())));
    }

    @PostMapping("/users/logout")
    public ResponseEntity<BaseResponse<Void>> logoutUser(@AuthenticationPrincipal Long userId) {
        userService.logoutUser(userId);

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/users/reissue")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(BaseResponse.success(HttpStatus.OK, "로그아웃 성공", null));
    }

    @GetMapping("/users/me")
    public ResponseEntity<BaseResponse<UserProfileResponse>> getUserProfile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                BaseResponse.success(HttpStatus.OK, "내 프로필 조회 성공", userService.getUserProfile(userId))
        );
    }

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/users/reissue")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
    }
}

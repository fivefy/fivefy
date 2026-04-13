package com.fivefy.domain.user.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.domain.user.dto.request.UserLoginRequest;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserLoginResponse;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/users/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(BaseResponse.success(HttpStatus.OK, "로그인 성공", UserLoginResponse.from(result.accessToken())));
    }
}

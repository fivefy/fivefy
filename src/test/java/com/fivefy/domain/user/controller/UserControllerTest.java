package com.fivefy.domain.user.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.dto.request.UserLoginRequest;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserLoginResponse;
import com.fivefy.domain.user.dto.response.UserProfileResponse;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.enums.UserStatus;
import com.fivefy.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static com.fivefy.domain.user.enums.UserErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.cookies.CookieDocumentation.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@WebMvcTest(UserController.class)
class UserControllerTest extends RestDocsSupport {

    @MockitoBean private UserService userService;
    @MockitoBean private JwtUtil jwtUtil;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("회원가입 성공 시 201 반환")
        void signupSuccess() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("테스트", "test@test.com", "Test1234!");
            UserSignupResponse response = new UserSignupResponse(1L, "테스트", "test@test.com", LocalDateTime.now());
            given(userService.signupUser(any(UserSignupRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트"))
                    .andDo(document("user-signup",
                            requestFields(
                                    fieldWithPath("name").type(STRING).description("이름 (2~10자)"),
                                    fieldWithPath("email").type(STRING).description("이메일"),
                                    fieldWithPath("password").type(STRING).description("비밀번호 (8~20자, 영문+숫자+특수문자)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.userId").type(NUMBER).description("유저 ID"),
                                    fieldWithPath("data.name").type(STRING).description("이름"),
                                    fieldWithPath("data.email").type(STRING).description("이메일"),
                                    fieldWithPath("data.createdAt").type(STRING).description("가입 일시")
                            )
                    ));
        }

        @Test
        @DisplayName("이름 없이 회원가입 시 400 반환")
        void signupWithoutName() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("", "test@test.com", "Test1234!");

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("user-signup-validate",
                            requestFields(
                                    fieldWithPath("name").type(STRING).description("이름 (값 없음)"),
                                    fieldWithPath("email").type(STRING).description("이메일"),
                                    fieldWithPath("password").type(STRING).description("비밀번호 (8~20자, 영문+숫자+특수문자)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                                    fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                                    fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
                            )
                    ));
        }

        @Test
        @DisplayName("이메일 형식이 아니면 400 반환")
        void signupWithInvalidEmail() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("테스트", "invalid-email", "Test1234!");

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호 형식이 맞지 않으면 400 반환")
        void signupWithInvalidPassword() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("테스트", "test@test.com", "1234");

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 이메일 회원가입 시 409 반환")
        void signupWithDuplicateEmail() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("테스트", "test@test.com", "Test1234!");
            given(userService.signupUser(any()))
                    .willThrow(new BusinessException(ERR_USER_DUPLICATED_EMAIL));

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ERR_USER_DUPLICATED_EMAIL.getMessage()))
                    .andDo(document("user-signup-duplicate",
                            requestFields(
                                    fieldWithPath("name").type(STRING).description("이름 (2~10자)"),
                                    fieldWithPath("email").type(STRING).description("중복된 이메일"),
                                    fieldWithPath("password").type(STRING).description("비밀번호 (8~20자, 영문+숫자+특수문자)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("로그인 성공 시 200 반환 및 AT 응답")
        void loginSuccess() throws Exception {
            // given
            UserLoginRequest request = new UserLoginRequest("test@test.com", "Test1234!");
            UserLoginResponse result = UserLoginResponse.of("accessToken", "refreshToken");
            given(userService.loginUser(any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/api/users/login")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                    .andDo(document("user-login",
                            requestFields(
                                    fieldWithPath("email").type(STRING).description("이메일"),
                                    fieldWithPath("password").type(STRING).description("비밀번호")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.accessToken").type(STRING).description("액세스 토큰")
                            ),
                            responseCookies(
                                    cookieWithName("refreshToken").description("리프레시 토큰 (HttpOnly, Secure)")
                            ),
                            responseHeaders(
                                    headerWithName(HttpHeaders.CACHE_CONTROL).description("캐시 방지 헤더 (HTTP/1.1)"),
                                    headerWithName(HttpHeaders.PRAGMA).description("캐시 방지 헤더 (HTTP/1.0 호환)")
                            )
                    ));
        }

        @Test
        @DisplayName("로그인 성공 시 쿠키에 RT 설정")
        void loginSuccessWithRefreshTokenCookie() throws Exception {
            // given
            UserLoginRequest request = new UserLoginRequest("test@test.com", "Test1234!");
            UserLoginResponse result = UserLoginResponse.of("accessToken", "refreshToken");
            given(userService.loginUser(any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/api/users/login")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().value("refreshToken", "refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().secure("refreshToken", true));
        }

        @Test
        @DisplayName("로그인 성공 시 캐시 금지 헤더 포함")
        void loginSuccessWithCacheControlHeader() throws Exception {
            // given
            UserLoginRequest request = new UserLoginRequest("test@test.com", "Test1234!");
            UserLoginResponse result = UserLoginResponse.of("accessToken", "refreshToken");
            given(userService.loginUser(any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/api/users/login")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        }

        @Test
        @DisplayName("이메일 없이 로그인 시 400 반환")
        void loginWithoutEmail() throws Exception {
            // given
            UserLoginRequest request = new UserLoginRequest("", "Test1234!");

            // when & then
            mockMvc.perform(post("/api/users/login")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(document("user-login-validate",
                            requestFields(
                                    fieldWithPath("email").type(STRING).description("이메일 (값 없음)"),
                                    fieldWithPath("password").type(STRING).description("비밀번호")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지"),
                                    fieldWithPath("data").type(ARRAY).description("상세 에러 내역"),
                                    fieldWithPath("data[].field").type(STRING).description("에러가 발생한 필드명"),
                                    fieldWithPath("data[].message").type(STRING).description("에러 상세 사유")
                            )
                    ));
        }

        @Test
        @DisplayName("로그인 실패 시 401 반환")
        void loginFail() throws Exception {
            // given
            UserLoginRequest request = new UserLoginRequest("test@test.com", "wrongPassword1!");
            given(userService.loginUser(any()))
                    .willThrow(new BusinessException(ERR_USER_LOGIN_FAIL));

            // when & then
            mockMvc.perform(post("/api/users/login")
                            .with(csrf().asHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(ERR_USER_LOGIN_FAIL.getMessage()))
                    .andDo(document("user-login-fail",
                            requestFields(
                                    fieldWithPath("email").type(STRING).description("이메일"),
                                    fieldWithPath("password").type(STRING).description("잘못된 비밀번호")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("재발급 성공 — 200, 새 AT 바디 반환, 새 RT 쿠키 설정")
        void reissueSuccess() throws Exception {
            // given
            UserLoginResponse result = UserLoginResponse.of("new.access.token", "new.refresh.token");
            given(userService.reissueToken(any())).willReturn(result);

            // when & then
            mockMvc.perform(post("/api/users/reissue")
                            .with(csrf().asHeader())
                            .cookie(new Cookie("refreshToken", "valid.refresh.token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                    .andExpect(cookie().value("refreshToken", "new.refresh.token"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().secure("refreshToken", true))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andDo(document("user-reissue",
                            requestCookies(
                                    cookieWithName("refreshToken").description("기존 리프레시 토큰 (HttpOnly, Secure)")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("data.accessToken").type(STRING).description("새로운 액세스 토큰")
                            ),
                            responseCookies(
                                    cookieWithName("refreshToken").description("새로운 리프레시 토큰 (HttpOnly, Secure)")
                            ),
                            responseHeaders(
                                    headerWithName(HttpHeaders.CACHE_CONTROL).description("캐시 방지 헤더 (HTTP/1.1)"),
                                    headerWithName(HttpHeaders.PRAGMA).description("캐시 방지 헤더 (HTTP/1.0 호환)")
                            )
                    ));
        }

        @Test
        @DisplayName("RT 쿠키 없이 재발급 요청 시 401 반환")
        void reissueWithoutCookie() throws Exception {
            mockMvc.perform(post("/api/users/reissue")
                            .with(csrf().asHeader()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("인증 정보가 유효하지 않습니다"))
                    .andDo(document("user-reissue-missing-rt",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }

        @Test
        @DisplayName("유효하지 않은 RT로 재발급 시 401 반환")
        void reissueWithInvalidToken() throws Exception {
            // given
            given(userService.reissueToken(any()))
                    .willThrow(new BusinessException(ERR_USER_INVALID_RT));

            // when & then
            mockMvc.perform(post("/api/users/reissue")
                            .with(csrf().asHeader())
                            .cookie(new Cookie("refreshToken", "invalid.refresh.token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(ERR_USER_INVALID_RT.getMessage()))
                    .andDo(document("user-reissue-invalid-rt",
                            requestCookies(
                                    cookieWithName("refreshToken").description("유효하지 않은 리프레시 토큰")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("에러 메시지")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("로그아웃 API")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공 — 200, RT 쿠키 만료 처리")
        void logoutSuccess() throws Exception {
            mockMvc.perform(post("/api/users/logout")
                            .with(csrf().asHeader())
                            .requestAttr("userId", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 성공"))
                    .andExpect(cookie().maxAge("refreshToken", 0))
                    .andDo(document("user-logout",
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(STRING).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지")
                            ),
                            responseCookies(
                                    cookieWithName("refreshToken").description("만료된 리프레시 토큰 (maxAge=0)")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("프로필 조회 성공 시 200 반환")
        void getUserProfileSuccess() throws Exception {
            // given
            UserProfileResponse response = new UserProfileResponse(
                    "test@test.com", "테스트",
                    UserRole.USER, UserStatus.ACTIVE,
                    LocalDateTime.now()
            );
            given(userService.getUserProfile(any())).willReturn(response);

            //when & then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트"));
        }
    }
}

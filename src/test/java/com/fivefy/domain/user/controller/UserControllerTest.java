package com.fivefy.domain.user.controller;

import com.fivefy.common.config.security.JwtAuthenticationEntryPoint;
import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static com.fivefy.domain.user.enums.UserErrorCode.ERR_USER_DUPLICATED_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.name").value("테스트"));
        }

        @Test
        @DisplayName("이름 없이 회원가입 시 400 반환")
        void signupWithoutName() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("", "test@test.com", "Test1234!");

            // when & then
            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이메일 형식이 아니면 400 반환")
        void signupWithInvalidEmail() throws Exception {
            // given
            UserSignupRequest request = new UserSignupRequest("테스트", "invalid-email", "Test1234!");

            // when & then
            mockMvc.perform(post("/api/users/signup")
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ERR_USER_DUPLICATED_EMAIL.getMessage()));
        }
    }
}

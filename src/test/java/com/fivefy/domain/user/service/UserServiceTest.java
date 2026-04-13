package com.fivefy.domain.user.service;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.dto.request.UserLoginRequest;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserLoginResponse;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.repository.UserRepository;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static com.fivefy.domain.user.enums.UserErrorCode.ERR_USER_DUPLICATED_EMAIL;
import static com.fivefy.domain.user.enums.UserErrorCode.ERR_USER_LOGIN_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private WalletRepository walletRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        private UserSignupRequest request;

        @BeforeEach
        void setUp() {
            request = new UserSignupRequest("테스트", "test@test.com", "Test1234!");
        }

        @Test
        @DisplayName("회원가입 성공")
        void signupSuccess() {
            // given
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

            User user = User.create(request.email(), "encodedPassword", request.name());
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(user);

            // when
            UserSignupResponse response = userService.signupUser(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());
            verify(walletRepository, times(1)).save(any(Wallet.class));
        }

        @Test
        @DisplayName("중복 이메일 회원가입 시 예외 발생")
        void duplicateEmailException() {
            // given
            given(userRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signupUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_DUPLICATED_EMAIL.getMessage());

            verify(userRepository, never()).save(any(User.class));
            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        @DisplayName("비밀번호는 암호화되어 저장")
        void passwordEncode() {
            // given
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

            User user = User.create(request.email(), "encodedPassword", request.name());
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(user);

            // when
            userService.signupUser(request);

            // then
            verify(passwordEncoder, times(1)).encode("Test1234!");
            verify(userRepository).save(argThat(savedUser ->
                    savedUser.getPassword().equals("encodedPassword")
            ));
        }

        @Test
        @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 중복 이메일 예외로 변환")
        void dataIntegrityViolationExceptionConvertToDuplicateEmailException() {
            // given
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willThrow(DataIntegrityViolationException.class);

            // when & then
            assertThatThrownBy(() -> userService.signupUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_DUPLICATED_EMAIL.getMessage());

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private UserLoginRequest request;
        private User user;

        @BeforeEach
        void setUp() {
            request = new UserLoginRequest("test@test.com", "Test1234!");
            user = User.create("test@test.com", "encodedPassword", "테스트");
        }

        @Test
        @DisplayName("로그인 성공")
        void loginSuccess() {
            // given
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtUtil.createAccessToken(any(), any())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken()).willReturn("refreshToken");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            UserLoginResponse response = userService.loginUser(request);

            // then
            assertThat(response.accessToken()).isEqualTo("accessToken");
            assertThat(response.refreshToken()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("로그인 성공 시 RT가 Redis에 저장")
        void refreshTokenSaveToRedis() {
            // given
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtUtil.createAccessToken(any(), any())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken()).willReturn("refreshToken");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            userService.loginUser(request);

            // then
            verify(valueOperations).set(
                    eq("RT:" + user.getId()),
                    eq("refreshToken"),
                    eq(Duration.ofDays(7))
            );
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
        void userNotFoundEmailThrowException() {
            // given
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.loginUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_LOGIN_FAIL.getMessage());

            verify(jwtUtil, never()).createAccessToken(any(), any());
            verify(passwordEncoder, times(1)).matches(any(), any());
        }

        @Test
        @DisplayName("비밀번호 불일치 시 예외 발생")
        void passwordMismatch() {
            // given
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.loginUser(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_LOGIN_FAIL.getMessage());

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일도 더미 해시 비교를 수행")
        void userNotFoundEmailMatchesPassword() {
            // given
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());
            given(passwordEncoder.matches(any(), any())).willReturn(false);

            // when
            assertThatThrownBy(() -> userService.loginUser(request))
                    .isInstanceOf(BusinessException.class);

            // then — 이메일이 없어도 passwordEncoder.matches()가 반드시 1회 호출됨
            verify(passwordEncoder, times(1)).matches(any(), any());
        }
    }
}

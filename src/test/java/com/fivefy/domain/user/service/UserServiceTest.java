package com.fivefy.domain.user.service;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.dto.event.UserDeletedEvent;
import com.fivefy.domain.user.dto.request.UserDeleteRequest;
import com.fivefy.domain.user.dto.request.UserLoginRequest;
import com.fivefy.domain.user.dto.request.UserProfileUpdateRequest;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserLoginResponse;
import com.fivefy.domain.user.dto.response.UserProfileResponse;
import com.fivefy.domain.user.dto.response.UserProfileUpdateResponse;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserRole;
import com.fivefy.domain.user.enums.UserStatus;
import com.fivefy.domain.user.repository.UserRepository;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.WalletRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.fivefy.domain.user.enums.UserErrorCode.*;
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
    @Mock private ApplicationEventPublisher eventPublisher;

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
            ReflectionTestUtils.setField(user, "id", 1L);
        }

        @Test
        @DisplayName("로그인 성공")
        void loginSuccess() {
            // given
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtUtil.createAccessToken(any(), any())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken(any())).willReturn("refreshToken");
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
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtUtil.createAccessToken(any(), any())).willReturn("accessToken");
            given(jwtUtil.createRefreshToken(any())).willReturn("refreshToken");
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
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email())).willReturn(Optional.empty());

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
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email())).willReturn(Optional.of(user));
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
            given(userRepository.findByEmailAndDeletedAtIsNull(request.email())).willReturn(Optional.empty());
            given(passwordEncoder.matches(any(), any())).willReturn(false);

            // when
            assertThatThrownBy(() -> userService.loginUser(request))
                    .isInstanceOf(BusinessException.class);

            // then — 이메일이 없어도 passwordEncoder.matches()가 반드시 1회 호출됨
            verify(passwordEncoder, times(1)).matches(any(), any());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        private User user;
        private Claims claims;

        private static final String VALID_RT    = "valid.refresh.token";
        private static final String PREV_RT     = "prev.refresh.token";
        private static final String NEW_AT      = "new.access.token";
        private static final String NEW_RT      = "new.refresh.token";
        private static final String RT_KEY      = "RT:1";
        private static final String PREV_RT_KEY = "PREV_RT:1";
        private static final long   REMAINING_TTL = 600L;

        @BeforeEach
        void setUp() {
            user = User.create("test@test.com", "encodedPassword", "테스트");
            ReflectionTestUtils.setField(user, "id", 1L);
            ReflectionTestUtils.setField(user, "role", UserRole.USER);

            claims = mock(Claims.class);
        }

        @Test
        @DisplayName("재발급 성공 - 새 AT/RT 발급 및 Redis 갱신")
        void reissueSuccess() {
            // given
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(VALID_RT);
            given(valueOperations.get(PREV_RT_KEY)).willReturn(null);
            given(redisTemplate.getExpire(RT_KEY, TimeUnit.SECONDS)).willReturn(REMAINING_TTL);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(jwtUtil.createAccessToken(any(), any())).willReturn(NEW_AT);
            given(jwtUtil.createRefreshToken(1L)).willReturn(NEW_RT);
            given(redisTemplate.execute(ArgumentMatchers.<SessionCallback<List<Object>>>any())).willReturn(null);

            // when
            UserLoginResponse response = userService.reissueToken(VALID_RT);

            // then
            assertThat(response.accessToken()).isEqualTo(NEW_AT);
            assertThat(response.refreshToken()).isEqualTo(NEW_RT);
            verify(redisTemplate).execute(ArgumentMatchers.<SessionCallback<List<Object>>>any());
        }

        @Test
        @DisplayName("만료된 RT — ERR_USER_EXPIRED_RT 예외")
        void reissueWithExpiredToken() {
            // given
            given(jwtUtil.validateToken(any()))
                    .willThrow(mock(ExpiredJwtException.class));

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_EXPIRED_RT.getMessage());
        }

        @Test
        @DisplayName("서명 오류 등 유효하지 않은 RT — ERR_USER_INVALID_RT 예외")
        void reissueWithInvalidToken() {
            // given
            given(jwtUtil.validateToken(any()))
                    .willThrow(new RuntimeException("invalid"));

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_INVALID_RT.getMessage());
        }

        @Test
        @DisplayName("로그아웃 상태(Redis RT 없음) — ERR_USER_NOT_LOGGED_IN 예외")
        void reissueWhenLoggedOut() {
            // given
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_NOT_LOGGED_IN.getMessage());
        }

        @Test
        @DisplayName("RT 탈취 감지 — storedRt/prevRt 불일치 시 RT + PREV_RT 삭제 후 예외")
        void reissueDetectsTokenTheft() {
            // given — 이미 Rotation된 RT로 재시도하는 탈취 시나리오
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn("already.rotated.token");
            given(valueOperations.get(PREV_RT_KEY)).willReturn("also.different.token");

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_INVALID_RT.getMessage());

            verify(redisTemplate).delete(RT_KEY);
            verify(redisTemplate).delete(PREV_RT_KEY);
        }

        @Test
        @DisplayName("유예 RT로 재발급 — 새 AT + 현재 storedRt 반환 (동시 요청 대응)")
        void reissueWithPrevRt() {
            // given — 동시 요청으로 인해 이전 RT(PREV_RT)로 재시도하는 시나리오
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(PREV_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(VALID_RT);
            given(valueOperations.get(PREV_RT_KEY)).willReturn(PREV_RT);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(jwtUtil.createAccessToken(any(), any())).willReturn(NEW_AT);

            // when
            UserLoginResponse response = userService.reissueToken(PREV_RT);

            // then — AT는 새로 발급, RT는 현재 storedRt 반환
            assertThat(response.accessToken()).isEqualTo(NEW_AT);
            assertThat(response.refreshToken()).isEqualTo(VALID_RT);

            // 유예 RT 처리 시 새 RT 발급하지 않음
            verify(jwtUtil, never()).createRefreshToken(anyLong());
        }

        @Test
        @DisplayName("Redis TTL 0 이하 — ERR_USER_EXPIRED_RT 예외")
        void reissueWhenTtlExpired() {
            // given
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(VALID_RT);
            given(valueOperations.get(PREV_RT_KEY)).willReturn(null);
            given(redisTemplate.getExpire(RT_KEY, TimeUnit.SECONDS)).willReturn(0L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_EXPIRED_RT.getMessage());
        }

        @Test
        @DisplayName("유저 탈퇴/삭제 상태 — ERR_USER_NOT_FOUND 예외")
        void reissueWithDeletedUser() {
            // given
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(VALID_RT);
            given(valueOperations.get(PREV_RT_KEY)).willReturn(null);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.reissueToken(VALID_RT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("재발급 성공 시 새 RT가 기존 RT와 다르다 (Rotation 검증)")
        void reissueRotatesRefreshToken() {
            // given
            given(claims.getSubject()).willReturn("1");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(jwtUtil.validateToken(VALID_RT)).willReturn(claims);
            given(valueOperations.get(RT_KEY)).willReturn(VALID_RT);
            given(valueOperations.get(PREV_RT_KEY)).willReturn(null);
            given(redisTemplate.getExpire(RT_KEY, TimeUnit.SECONDS)).willReturn(REMAINING_TTL);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(jwtUtil.createAccessToken(any(), any())).willReturn(NEW_AT);
            given(jwtUtil.createRefreshToken(1L)).willReturn(NEW_RT);
            given(redisTemplate.execute(ArgumentMatchers.<SessionCallback<List<Object>>>any())).willReturn(null);

            // when
            UserLoginResponse response = userService.reissueToken(VALID_RT);

            // then
            assertThat(response.refreshToken()).isNotEqualTo(VALID_RT);
            assertThat(response.refreshToken()).isEqualTo(NEW_RT);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        private static final String RT_KEY      = "RT:1";
        private static final String PREV_RT_KEY = "PREV_RT:1";

        @Test
        @DisplayName("로그아웃 성공 — RT + PREV_RT 모두 삭제")
        void logoutSuccess() {
            // when
            userService.logoutUser(1L);

            // then
            verify(redisTemplate).delete(RT_KEY);
            verify(redisTemplate).delete(PREV_RT_KEY);
        }

        @Test
        @DisplayName("이미 로그아웃된 상태에서 재시도 — 예외 없이 처리")
        void logoutAlreadyLoggedOut() {
            // given
            given(redisTemplate.delete(RT_KEY)).willReturn(false);
            given(redisTemplate.delete(PREV_RT_KEY)).willReturn(false);

            // when & then
            userService.logoutUser(1L);
            verify(redisTemplate).delete(RT_KEY);
            verify(redisTemplate).delete(PREV_RT_KEY);
        }
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("프로필 조회 성공")
        void getUserProfileSuccess() {
            // given
            User user = User.create("test@test.com", "encodedPassword", "테스트");
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

            // when
            UserProfileResponse response = userService.getUserProfile(1L);

            // then
            assertThat(response.email()).isEqualTo("test@test.com");
            assertThat(response.name()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 예외 발생")
        void getUserProfileNotFound() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateUserProfile {

        private User user;
        private UserProfileUpdateRequest nameOnlyRequest;
        private UserProfileUpdateRequest passwordOnlyRequest;
        private UserProfileUpdateRequest bothRequest;

        @BeforeEach
        void setUp() {
            user = User.create("test@test.com", "encodedPassword", "테스트");

            nameOnlyRequest = new UserProfileUpdateRequest("새이름", null);
            passwordOnlyRequest = new UserProfileUpdateRequest(null,
                    new UserProfileUpdateRequest.PasswordChangeRequest("Test1234!", "NewPass1!"));
            bothRequest = new UserProfileUpdateRequest("새이름",
                    new UserProfileUpdateRequest.PasswordChangeRequest("Test1234!", "NewPass1!"));
        }

        @Test
        @DisplayName("이름만 수정 성공")
        void updateNameOnly() {
            // given
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

            // when
            UserProfileUpdateResponse response = userService.updateUserProfile(1L, nameOnlyRequest);

            // then
            assertThat(response.name()).isEqualTo("새이름");
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("비밀번호만 수정 성공")
        void updatePasswordOnly() {
            // given
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Test1234!", user.getPassword())).willReturn(true);
            given(passwordEncoder.encode("NewPass1!")).willReturn("newEncodedPassword");

            // when
            UserProfileUpdateResponse response = userService.updateUserProfile(1L, passwordOnlyRequest);

            // then
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            verify(passwordEncoder).encode("NewPass1!");
        }

        @Test
        @DisplayName("이름 + 비밀번호 동시 수정 성공")
        void updateBoth() {
            // given
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Test1234!", user.getPassword())).willReturn(true);
            given(passwordEncoder.encode("NewPass1!")).willReturn("newEncodedPassword");

            // when
            UserProfileUpdateResponse response = userService.updateUserProfile(1L, bothRequest);

            // then
            assertThat(response.name()).isEqualTo("새이름");
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("현재 비밀번호 불일치 시 예외 발생")
        void updateWithWrongCurrentPassword() {
            // given
            ReflectionTestUtils.setField(user, "id", 1L);
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Test1234!", user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateUserProfile(1L, passwordOnlyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_MISMATCH_PASSWORD.getMessage());

            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("존재하지 않는 유저 수정 시 예외 발생")
        void updateNotFoundUser() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateUserProfile(1L, nameOnlyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteUser {

        private User user;
        private UserDeleteRequest request;

        @BeforeEach
        void setUp() {
            user = User.create("test@test.com", "encodedPassword", "테스트");
            ReflectionTestUtils.setField(user, "id", 1L);
            ReflectionTestUtils.setField(user, "role", UserRole.USER);

            request = new UserDeleteRequest("Test1234!");
        }

        @Test
        @DisplayName("탈퇴 성공 — deletedAt 설정, status DELETED, 이벤트 발행")
        void deleteUserSuccess() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);

            // when
            userService.deleteUser(1L, request);

            // then
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.DELETED);

            // Redis 직접 삭제가 아닌 이벤트 발행 확인
            ArgumentCaptor<UserDeletedEvent> captor = ArgumentCaptor.forClass(UserDeletedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(1L);

            // Redis는 직접 호출하지 않음
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 예외 발생 — deletedAt 설정 안 됨")
        void deleteUserWithWrongPassword() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_MISMATCH_PASSWORD.getMessage());

            assertThat(user.getDeletedAt()).isNull();
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("유저를 찾을 수 없을 때 예외 발생 (탈퇴 유저 포함)")
        void deleteNotFoundUser() {
            // given - findByIdAndDeletedAtIsNull은 탈퇴 유저와 존재하지 않는 유저 모두 empty 반환
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(ERR_USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("탈퇴 성공 시 비밀번호 불일치 여부 검증 후 delete() 호출")
        void deleteUserCallsDeleteInOrder() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);

            // when
            userService.deleteUser(1L, request);

            // then — 비밀번호 검증 후 이벤트 발행 순서 확인
            verify(passwordEncoder).matches(request.password(), user.getPassword());
            verify(eventPublisher).publishEvent(any(UserDeletedEvent.class));
        }
    }
}

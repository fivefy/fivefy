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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.fivefy.domain.user.enums.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiS8GdKx2UY8ailqXF/w3vGN9VOGQ0y";
    private static final String RT_PREFIX = "RT:";
    private static final String PREV_RT_PREFIX = "PREV_RT:";
    private static final long PREV_RT_GRACE_PERIOD_SECONDS = 30L;

    /**
     회원가입
     1. 이메일 중복 검증
     2. 비밀번호 암호화
     3. User 생성 및 DB 저장
     4. Wallet 생성 및 DB 저장
     5. return
     */
    @Transactional
    public UserSignupResponse signupUser(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ERR_USER_DUPLICATED_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.email(), encodedPassword, request.name());
        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ERR_USER_DUPLICATED_EMAIL);
        }

        Wallet wallet = Wallet.create(savedUser.getId());
        walletRepository.save(wallet);

        return UserSignupResponse.from(savedUser);
    }

    /**
     로그인
     1. 이메일 & 비밀번호 검증
     2. 토큰 발급
     3. redis에 RT 저장
     4. return
     */
    public UserLoginResponse loginUser(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String targetPassword = (user != null) ? user.getPassword() : DUMMY_HASH;
        boolean isMatch = passwordEncoder.matches(request.password(), targetPassword);

        if (user == null || !isMatch) {
            throw new BusinessException(ERR_USER_LOGIN_FAIL);
        }

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken();

        redisTemplate.opsForValue().set(
                RT_PREFIX + user.getId(),
                refreshToken,
                Duration.ofDays(7)
        );

        return UserLoginResponse.of(accessToken, refreshToken);
    }

    /**
     토큰 재발급
     1. RT 쿠키 검증
     2. Redis에 저장된 RT와 일치 여부 확인
     3. 새로운 AT, RT 발급
     4. Redis에 RT 갱신
     */
    public UserLoginResponse reissueToken(String refreshToken) {
        Claims claims;
        try {
            // 전달받은 RT의 서명/만료 검증
            claims = jwtUtil.validateToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ERR_USER_EXPIRED_RT);
        } catch (Exception e) {
            throw new BusinessException(ERR_USER_INVALID_RT);
        }

        long userId = Long.parseLong(claims.getSubject());

        // Redis에 저장된 현재 RT와 유예 RT를 조회
        String storedRt = redisTemplate.opsForValue().get(RT_PREFIX + userId);
        String prevRt = redisTemplate.opsForValue().get(PREV_RT_PREFIX + userId);

        // 로그아웃 상태 체크
        if (storedRt == null) {
            throw new BusinessException(ERR_USER_NOT_LOGGED_IN);
        }

        // 두 토큰 중 어느 것과도 일치하지 않으면 탈취로 간주하고 모든 토큰 삭제 (로그아웃)
        if (!refreshToken.equals(storedRt) && !refreshToken.equals(prevRt)) {
            String tokenHash = DigestUtils.sha256Hex(refreshToken).substring(0, 16);
            log.warn("토큰 탈취 의심 - 사용자: {}, 토큰해시: {}", userId, tokenHash);
            redisTemplate.delete(RT_PREFIX + userId);
            redisTemplate.delete(PREV_RT_PREFIX + userId);
            throw new BusinessException(ERR_USER_INVALID_RT);
        }

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ERR_USER_NOT_FOUND)
        );
        // 유예 기간 RT로 요청이 오면 AT는 새로 발급, RT는 현재 최신 정보 반환 (동시성)
        if (refreshToken.equals(prevRt)) {
            log.info("유예 기간 내 토큰 재발급 요청 허용 - 사용자: {}", userId);
            return UserLoginResponse.of(
                    jwtUtil.createAccessToken(user.getId(), user.getRole()),
                    storedRt
            );
        }

        // 로그인 무한 동기화를 방지하기 위해 기존 RT의 남은 수명을 새로운 RT에 적용
        Long remainingTtl = redisTemplate.getExpire(RT_PREFIX + userId, TimeUnit.SECONDS);
        if (remainingTtl == null || remainingTtl <= 0) {
            throw new BusinessException(ERR_USER_EXPIRED_RT);
        }

        // 새로운 AT, RT 발급
        String newAccessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtUtil.createRefreshToken();

        // Redis 업데이트 (트랜잭션)
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(@NonNull RedisOperations operations) throws DataAccessException {
                operations.multi();
                // 재발급 이전 기존 RT를 유예 기간(30초) 동안 임시 저장하여 동시 요청 대비
                operations.opsForValue().set(
                        PREV_RT_PREFIX + userId,
                        storedRt,
                        Duration.ofSeconds(PREV_RT_GRACE_PERIOD_SECONDS)
                );
                // 새로운 RT를 Redis에 저장
                operations.opsForValue().set(
                        RT_PREFIX + userId,
                        newRefreshToken,
                        Duration.ofSeconds(remainingTtl)
                );
                return operations.exec();
            }
        });

        return UserLoginResponse.of(newAccessToken, newRefreshToken);
    }
}

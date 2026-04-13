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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.fivefy.domain.user.enums.UserErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;

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
    @Transactional
    public UserLoginResponse loginUser(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(
                () -> new BusinessException(ERR_USER_LOGIN_FAIL)
        );
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ERR_USER_LOGIN_FAIL);
        }

        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken();

        // TODO RT redis 저장

        return UserLoginResponse.of(accessToken, refreshToken);
    }
}

package com.fivefy.domain.user.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.user.dto.request.UserSignupRequest;
import com.fivefy.domain.user.dto.response.UserSignupResponse;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.repository.UserRepository;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.fivefy.domain.user.enums.UserErrorCode.ERR_USER_DUPLICATED_EMAIL;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;

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
        User savedUser = userRepository.save(user);
        Wallet wallet = Wallet.create(user.getId());
        walletRepository.save(wallet);

        return UserSignupResponse.from(savedUser);
    }
}

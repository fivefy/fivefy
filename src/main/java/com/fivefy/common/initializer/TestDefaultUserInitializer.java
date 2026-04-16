package com.fivefy.common.initializer;

import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.repository.UserRepository;
import com.fivefy.domain.wallet.entity.Wallet;
import com.fivefy.domain.wallet.entity.PointHistory;
import com.fivefy.domain.wallet.enums.PointType;
import com.fivefy.domain.wallet.enums.PointHistoryType;
import com.fivefy.domain.wallet.repository.WalletRepository;
import com.fivefy.domain.wallet.repository.PointHistoryRepository;
import com.fivefy.domain.subscription.entity.Subscription;
import com.fivefy.domain.subscription.enums.SubscriptionPlanType;
import com.fivefy.domain.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 더미데이터를 생성
 * 1. 유저
 * 2. 지갑
 * 3. 포인트
 * 4. 구독
 *
 * 설정 파일에
 * <pre>
 * {@code
 * app:
 *   add-wallet-dummy-test-data: true
 * }
 * </pre>
 *
 * 로 설정되어 있을 경우 test 데이터를 추가해 줍니다
 */
@Component
@ConditionalOnProperty(name = "app.add-wallet-dummy-test-data", havingValue = "true", matchIfMissing = false)
@Profile("!prod")
@RequiredArgsConstructor
public class TestDefaultUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 유저 1: 월간 패키지 구독 유저(한 달)
        initUser("test1@fivefy.com", "test1234", "테스트유저1",
                50000L, 0L, SubscriptionPlanType.MONTH, now
        );

        // 유저 2: 연간 구독 유저(일 년)
        initUser("test2@fivefy.com", "test1234", "테스트유저2",
                500000L, 0L, SubscriptionPlanType.YEAR, now
        );

        // 유저 3: 무료 유저(삼 일)
        initUser("test3@fivefy.com", "test1234", "테스트유저3",
                0L, 5000L, SubscriptionPlanType.FREE, now
        );
    }

    private void initUser(String email, String rawPassword, String name,
                          Long paidAmount, Long eventAmount,
                          SubscriptionPlanType subscriptionPlanType, LocalDateTime now
    ) {
        // 유저 생성
        User user = userRepository.save(
                User.create(email, passwordEncoder.encode(rawPassword), name)
        );

        // 지갑 생성 + 충전
        Wallet wallet = Wallet.create(user.getId());
        wallet.chargeBalance(paidAmount);
        if (eventAmount > 0) {
            wallet.chargeEventBalance(eventAmount);
        }
        walletRepository.save(wallet);

        // 유료 포인트 충전 이력
        pointHistoryRepository.save(PointHistory
                .create(wallet.getId(), PointType.PAID, PointHistoryType.CHARGE,
                paidAmount, paidAmount, "테스트 유료 포인트 충전"
                )
        );

        // 이벤트 포인트 충전 이력
        if (eventAmount > 0) {
            pointHistoryRepository.save(PointHistory
                    .create(wallet.getId(), PointType.FREE, PointHistoryType.CHARGE,
                    eventAmount, eventAmount, "테스트 이벤트 포인트 지급"
                    )
            );
        }

        /**
         * 구독 생성
         * 1 : 한 달
         * 2 : 일 년
         * 3 : 삼 일(무료)
         */
        LocalDateTime expiryDate = switch (subscriptionPlanType) {
            case MONTH -> now.plusMonths(1);
            case YEAR -> now.plusYears(1);
            case FREE -> now.plusDays(3);
            case RECURRING -> now.plusMonths(1);
        };
        LocalDateTime nextBillingDate = null;
        if (subscriptionPlanType != SubscriptionPlanType.FREE) {
            nextBillingDate = expiryDate;
        }
        subscriptionRepository.save(Subscription.create(
                user.getId(),
                null,
                subscriptionPlanType,
                now,
                expiryDate,
                nextBillingDate
                )
        );
    }
}

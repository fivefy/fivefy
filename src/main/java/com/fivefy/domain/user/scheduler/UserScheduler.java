package com.fivefy.domain.user.scheduler;

import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserRepository userRepository;

    private static final int ANONYMIZE_DAYS = 30;

    // 매일 새벽 4시 — 탈퇴 후 30일 경과 유저 개인정보 익명화
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "anonymizeDeletedUsers", lockAtMostFor = "1h", lockAtLeastFor = "1m")
    @Transactional
    public void anonymizeDeletedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(ANONYMIZE_DAYS);
        log.info("탈퇴 유저 개인정보 익명화 시작 — 기준일: {}", threshold);

        int anonymized = userRepository.anonymizeDeletedUsers(threshold);

        log.info("탈퇴 유저 개인정보 익명화 완료 — {}명 처리", anonymized);
    }
}

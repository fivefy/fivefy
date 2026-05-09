package com.fivefy.domain.payment.service;

import com.fivefy.domain.payment.entity.WebhookEvent;
import com.fivefy.domain.payment.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    /**
     * 웹훅 이벤트 저장 — REQUIRES_NEW 트랜잭션
     *
     * SELECT로 존재 여부 먼저 확인 후 INSERT
     * → 순차 중복: SELECT에서 이미 존재 확인 → false 반환
     * → 동시 중복: 둘 다 SELECT에서 없음 확인 → INSERT 시 unique 위반 → catch → false 반환
     *
     * @return true  : 최초 수신 (처리 계속)
     * @return false : 중복 수신 (처리 스킵)
     *
     * test에서 발생한 문제 해결하느라 추가한 속성 제거
     *             propagation = Propagation.REQUIRES_NEW, :
     *             isolation = Isolation.READ_COMMITTED
     */
    @Transactional
    public boolean saveIfNotDuplicate(String webhookEventId, String paymentId) {
        // 순차 중복 방어: 이미 존재하면 즉시 false 반환
        if (webhookEventRepository.existsByWebhookEventIdAndPaymentId(webhookEventId, paymentId)) {
            log.warn("[WebhookEvent] 이미 존재하는 webhookEventId={} — false 반환", webhookEventId);
            return false;
        }

        try {
            // 동시 중복 방어: INSERT 시 unique 위반 catch
            webhookEventRepository.saveAndFlush(WebhookEvent.create(webhookEventId, paymentId));

            return true;  // 최초 수신
        } catch (DataIntegrityViolationException e) {
            log.warn("[WebhookEvent] DataIntegrityViolationException 잡힘 — false 반환");

            return false; // 중복 수신
        }
        // 재시도하면 해결될 문제를 예외처리하면 그게 더 문제가 될 수 있다.
//        catch (Exception e) {
//            // 어떤 예외가 오는지 확인용
//            log.warn("[WebhookEvent] 예상 밖 예외 잡힘 — type={}, message={}", e.getClass().getName(), e.getMessage());
//
//            return false;
//        }
    }
}
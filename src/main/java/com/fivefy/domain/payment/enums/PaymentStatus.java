package com.fivefy.domain.payment.enums;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.payment.entity.Payment;

public enum PaymentStatus {
    /**
     * 결제 요청
     */
    REQUESTED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == COMPLETED || target == FAILED;
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
            if (target == COMPLETED) {
                payment.applyComplete();
            } else {
                payment.applyFail();
            }
        }
    },   

    /**
     * 보류
     * 현재 사용 위치는 0
     */
    HOLD {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
        }
    },        // 보류
    /**
     * 승인 : PG사가 결제를 승인했지만 아직 최종 확정 전 단계(잘 안씀)
     */
    APPROVED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
        }
    },
    /**
     * 결제 완료  : 서버에서 금액 검증, 웹훅 처리까지 모두 마친 최종 완료 상태
     */
    COMPLETED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == REFUNDED;
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
            payment.applyRefund();
        }
    },
    /**
     * 실패
     */
    FAILED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false; // 실패 후 전이 없음
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
        }
    },     

    /**
     * 취소
     */
    CANCELED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
        }
    },    

    /**
     * 환불
     */
    REFUNDED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false; // 환불 후 전이 없음
        }

        @Override
        public void transit(Payment payment, PaymentStatus target) {
            validate(target);
        }
    };



    public abstract boolean canTransitTo(
            PaymentStatus target
    );
    public abstract void transit(
            Payment payment, PaymentStatus target
    );

    // 전이 가능 여부 공통 검증 — 각 상태의 transit()에서 호출
    protected void validate(PaymentStatus target) {
        if (!canTransitTo(target)) {
            throw new BusinessException(PaymentErrorCode.ERR_PAYMENT_INVALID_TRANSITION);
        }
    }
}

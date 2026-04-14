package com.fivefy.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentVerifyRequest(
        @NotBlank(message = "결제 ID는 필수입니다")
        String paymentId,
        @NotNull(message = "결제 금액은 필수입니다")
        Long amount,
        @NotBlank(message = "멱등키는 필수입니다")
        String idempotencyKey
) {}
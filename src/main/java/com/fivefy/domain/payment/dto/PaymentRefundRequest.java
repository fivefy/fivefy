package com.fivefy.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRefundRequest(
        @NotNull(message = "결제 ID는 필수입니다")
        Long paymentId,   // Payment의 PK (id)
        @NotBlank(message = "환불 사유는 필수입니다")
        String reason
) {}
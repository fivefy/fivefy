package com.fivefy.domain.billingkey.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 빌링키 등록 요청
 * 프론트가 포트원 SDK로 카드 등록 후 받은 billingKeyId를 서버에 전달
 */
public record BillingKeyRegisterRequest(
        @NotBlank String billingKeyId
) {}

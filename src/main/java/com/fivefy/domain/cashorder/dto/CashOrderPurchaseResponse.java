package com.fivefy.domain.cashorder.dto;

/**
 * orderNumber만 프론트에 반환하면 프론트가 이 값을 포트원 SDK에 넘겨서 결제창을 띄운다.
 * 실제 결제 완료는 웹훅으로 처리
 * @param orderNumber
 */
public record CashOrderPurchaseResponse(
    String orderNumber
) {}
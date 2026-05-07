package com.fivefy.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendMessageRequest(
        Long sessionId,  // null이면 신규 세션
        @NotBlank(message = "메시지는 필수입니다")
        @Size(max = 1000, message = "메시지는 최대 1000자까지 작성 가능합니다")
        String message
) {
}

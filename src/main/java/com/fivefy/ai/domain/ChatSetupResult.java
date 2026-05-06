package com.fivefy.ai.domain;

import com.fivefy.domain.chat.entity.ChatSession;

public record ChatSetupResult(
        ChatSession session,
        boolean isNew
) {
}

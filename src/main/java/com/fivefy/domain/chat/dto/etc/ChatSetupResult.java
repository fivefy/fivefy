package com.fivefy.domain.chat.dto.etc;

import com.fivefy.domain.chat.entity.ChatSession;

public record ChatSetupResult(
        ChatSession session,
        boolean isNew
) {
}

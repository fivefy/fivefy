package com.fivefy.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserEmbedding {

    private final Long userId;
    private final float[] embedding;
    private final int basedOnCount;
    private final LocalDateTime computedAt;
}

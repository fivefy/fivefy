package com.fivefy.ai.dto.etc;

public record BatchResult(
        int processed,
        int skipped,
        int failed
) {
}

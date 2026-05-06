package com.fivefy.ai.dto.etc;

public record BuildResult(
        float[] vector,
        int sourceCount,
        boolean success
) {
    public static BuildResult empty() {
        return new BuildResult(null, 0, false);
    }

    public static BuildResult of(float[] v, int n) {
        return new BuildResult(v, n, true);
    }
}

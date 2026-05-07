package com.fivefy.ai.dto.etc;

import java.util.ArrayList;
import java.util.List;

public record TrackLyricsForEmbedding(
        Long trackId,
        String lyrics
) {
    public List<String> toChunks() {
        return toChunks(75);
    }

    public List<String> toChunks(int maxCharsPerChunk) {
        if (lyrics == null || lyrics.isBlank()) return List.of();

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int charsInCurrent = 0;

        for (String line : lyrics.split("\n")) {
            String trimmed = line.strip();

            // 빈 줄 = 청크 경계 후보
            if (trimmed.isEmpty()) {
                if (charsInCurrent > maxCharsPerChunk / 2) {
                    chunks.add(current.toString().strip());
                    current.setLength(0);
                    charsInCurrent = 0;
                }
                continue;
            }

            // 한 줄 추가했을 때 너무 길어지면 청크 종결
            if (charsInCurrent + trimmed.length() > maxCharsPerChunk && charsInCurrent > 0) {
                chunks.add(current.toString().strip());
                current.setLength(0);
                charsInCurrent = 0;
            }

            current.append(trimmed).append("\n");
            charsInCurrent += trimmed.length();
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().strip());
        }

        return chunks;
    }

    // 가사 미리보기 추출
    public String toSnippet() {
        if (lyrics == null || lyrics.isBlank()) return "";
        String oneLine = lyrics.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= 30 ? oneLine : oneLine.substring(0, 30) + "…";
    }
}

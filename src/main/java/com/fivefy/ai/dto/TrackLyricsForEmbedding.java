package com.fivefy.ai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 가사 임베딩 입력.
 *
 * 청크 분할 책임도 여기에 둠 (도메인 로직).
 */
public record TrackLyricsForEmbedding(
        Long trackId,
        String lyrics            // 가사 원문 (메모리에서만 사용, DB 저장 X)
) {
    /**
     * 가사를 청크로 분할.
     *
     * 전략:
     *  - 줄 단위로 자르되, 약 150 토큰(≈ 한국어 75자) 넘으면 청크 종결
     *  - 빈 줄은 자연스러운 청크 경계로 활용 (verse/chorus 구분)
     *  - 토큰 정확 계산은 비싸니 char count로 근사 (한국어 1자 ≈ 2토큰)
     *
     * 결과:
     *  - 짧은 가사 (~200자): 1~2 청크
     *  - 긴 가사 (~600자): 4~5 청크
     */
    public List<String> toChunks() {
        return toChunks(75);  // 한국어 75자 ≈ 150 토큰
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

    /**
     * 미리보기용 첫 30자 추출.
     * 줄바꿈을 공백으로 치환해서 한 줄로.
     */
    public String toSnippet() {
        if (lyrics == null || lyrics.isBlank()) return "";
        String oneLine = lyrics.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= 30 ? oneLine : oneLine.substring(0, 30) + "…";
    }
}

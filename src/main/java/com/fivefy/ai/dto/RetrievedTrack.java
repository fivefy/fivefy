package com.fivefy.ai.dto;

/**
 * RAG 컨텍스트로 LLM에 넘기는 형태 + UI 카드 표시용.
 */
public record RetrievedTrack(
        Long trackId,
        String title,
        String artist,
        String album,
        String genre,
        Integer releaseYear,
        String albumCoverUrl
) {
    /**
     * LLM 프롬프트에 넣을 한 줄 요약 (번호로 참조 가능하게).
     * "[3] Title: ..., Artist: ..., Year: ..."
     */
    public String toPromptLine(int index) {
        StringBuilder sb = new StringBuilder("[").append(index).append("] ");
        sb.append("Title: ").append(title);
        sb.append(", Artist: ").append(artist);
        if (genre != null && !genre.isBlank()) sb.append(", Genre: ").append(genre);
        if (releaseYear != null) sb.append(", Year: ").append(releaseYear);
        return sb.toString();
    }
}

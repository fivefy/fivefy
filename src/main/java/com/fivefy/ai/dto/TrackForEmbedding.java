package com.fivefy.ai.dto;

/**
 * 트랙 메타데이터를 임베딩 텍스트로 변환하는 record.
 * 기존 Track 엔티티에서 필요한 필드만 뽑아서 만든다.
 * 임베딩 품질 = 텍스트 품질이라 여기 신경 많이 써야 함.
 */
public record TrackForEmbedding(
        Long trackId,
        String title,
        String artist,
        String album,
        String genre,
        Integer releaseYear,
        String featuredArtists
) {
    /**
     * 임베딩 모델에 입력할 텍스트 생성.
     * 형식: "Title: ... | Artist: ... | Album: ... | Genre: ... | Year: ..."
     * - 자연어 문장보다 key:value 구조가 임베딩 품질이 더 좋다는 게 일반적 경험
     * - null/blank 필드는 제외
     */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, "Title", title);
        appendIfNotBlank(sb, "Artist", artist);
        appendIfNotBlank(sb, "Featuring", featuredArtists);
        appendIfNotBlank(sb, "Album", album);
        appendIfNotBlank(sb, "Genre", genre);
        if (releaseYear != null) {
            appendIfNotBlank(sb, "Year", releaseYear.toString());
        }
        return sb.toString();
    }

    private static void appendIfNotBlank(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(" | ");
        }
        sb.append(key).append(": ").append(value);
    }
}

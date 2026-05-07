package com.fivefy.ai.dto.etc;

public record TrackForEmbedding(
        Long trackId,
        String title,
        String artist,
        String album,
        String genre,
        Integer releaseYear,
        String featuredArtists
) {
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

package com.fivefy.ai.dto.etc;

public record RetrievedTrack(
        Long trackId,
        String title,
        String artist,
        String album,
        String genre,
        Integer releaseYear,
        String albumCoverUrl
) {
    public String toPromptLine(int index) {
        StringBuilder sb = new StringBuilder("[").append(index).append("] ");
        sb.append("Title: ").append(title);
        sb.append(", Artist: ").append(artist);
        if (genre != null && !genre.isBlank()) sb.append(", Genre: ").append(genre);
        if (releaseYear != null) sb.append(", Year: ").append(releaseYear);
        return sb.toString();
    }
}

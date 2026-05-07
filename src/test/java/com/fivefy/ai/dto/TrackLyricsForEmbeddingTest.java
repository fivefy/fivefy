package com.fivefy.ai.dto;

import com.fivefy.ai.dto.etc.TrackLyricsForEmbedding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackLyricsForEmbeddingTest {

    @Test
    @DisplayName("짧은 가사는 청크 1개")
    void shortLyricsSingleChunk() {
        TrackLyricsForEmbedding lyrics = new TrackLyricsForEmbedding(1L, """
                I love you
                Forever and always
                """);

        List<String> chunks = lyrics.toChunks(75);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("I love you");
        assertThat(chunks.get(0)).contains("Forever and always");
    }

    @Test
    @DisplayName("긴 가사는 75자 임계로 청크 분할")
    void longLyricsSplitsByLength() {
        // 한 줄에 25자, 6줄 → 150자 → 청크 ≥ 2개
        String line = "이 노래를 듣는 너에게 사랑해 정말로\n";
        TrackLyricsForEmbedding lyrics = new TrackLyricsForEmbedding(1L, line.repeat(6));

        List<String> chunks = lyrics.toChunks(75);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("빈 줄을 청크 경계로 활용 (verse/chorus 구분)")
    void blankLineAsChunkBoundary() {
        // 빈 줄을 사이에 두고 충분히 긴 두 섹션
        // toChunks의 로직: charsInCurrent > maxCharsPerChunk/2 일 때만 빈 줄에서 끊음
        String lyrics = """
                첫 번째 verse입니다 충분히 길게 작성된 텍스트입니다 정말로 정말로 정말로 더 길게!
                
                두 번째 verse입니다 이것도 충분한 길이를 가진 텍스트입니다
                """;
        TrackLyricsForEmbedding t = new TrackLyricsForEmbedding(1L, lyrics);

        List<String> chunks = t.toChunks(75);

        // 빈 줄을 기준으로 두 섹션이 분리됨
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).contains("첫 번째 verse");
        assertThat(chunks.get(1)).contains("두 번째 verse");
    }

    @Test
    @DisplayName("빈 가사는 빈 청크 리스트")
    void emptyLyricsReturnsEmpty() {
        assertThat(new TrackLyricsForEmbedding(1L, "").toChunks()).isEmpty();
        assertThat(new TrackLyricsForEmbedding(1L, null).toChunks()).isEmpty();
        assertThat(new TrackLyricsForEmbedding(1L, "   \n  \n").toChunks()).isEmpty();
    }

    @Test
    @DisplayName("snippet은 첫 30자 + ellipsis")
    void snippetTruncatesWithEllipsis() {
        TrackLyricsForEmbedding lyrics = new TrackLyricsForEmbedding(1L,
                "이별 후에 혼자 우는 밤은 너무 길고 너무 외롭고 너무 슬프다");

        String snippet = lyrics.toSnippet();

        assertThat(snippet).hasSizeLessThanOrEqualTo(31); // 30자 + …
        assertThat(snippet).startsWith("이별 후에");
        assertThat(snippet).endsWith("…");
    }

    @Test
    @DisplayName("줄바꿈이 snippet에서 공백으로 치환된다")
    void snippetReplacesNewlines() {
        TrackLyricsForEmbedding lyrics = new TrackLyricsForEmbedding(1L, "첫 줄\n둘째 줄\n세번째");

        String snippet = lyrics.toSnippet();

        assertThat(snippet).doesNotContain("\n");
        assertThat(snippet).contains("첫 줄 둘째 줄 세번째");
    }
}

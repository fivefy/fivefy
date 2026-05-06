package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.RetrievedTrack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagPromptBuilderTest {
    private final RagPromptBuilder builder = new RagPromptBuilder();

    @Test
    @DisplayName("검색 결과가 있으면 번호 매겨서 프롬프트에 주입한다")
    void buildsPromptWithNumberedTracks() {
        List<RetrievedTrack> tracks = List.of(
                new RetrievedTrack(1L, "Yesterday", "The Beatles",
                        "Help!", "Pop", 1965, "url"),
                new RetrievedTrack(2L, "Imagine", "John Lennon",
                        "Imagine", "Pop", 1971, "url")
        );

        String prompt = builder.build(tracks);

        // 번호 매겨진 형식
        assertThat(prompt).contains("[1] Title: Yesterday");
        assertThat(prompt).contains("[2] Title: Imagine");
        // 환각 방지 룰
        assertThat(prompt).contains("ONLY tracks from the \"Available tracks\" list");
        assertThat(prompt).contains("NEVER make up");
    }

    @Test
    @DisplayName("검색 결과가 비면 솔직히 답하라고 지시한다")
    void emptyTracksTellsToBeHonest() {
        String prompt = builder.build(List.of());

        assertThat(prompt).contains("No matching tracks");
        assertThat(prompt).contains("Apologize briefly");
        // 트랙 리스트 섹션 없음
        assertThat(prompt).doesNotContain("### Available tracks");
    }

    @Test
    @DisplayName("null 필드는 프롬프트 라인에서 제외된다")
    void promptLineSkipsNullFields() {
        RetrievedTrack track = new RetrievedTrack(1L, "Hello", "Adele",
                null, null, null, "url");

        String line = track.toPromptLine(1);

        assertThat(line).contains("Title: Hello");
        assertThat(line).contains("Artist: Adele");
        // null 필드는 안 들어감
        assertThat(line).doesNotContain("Genre");
        assertThat(line).doesNotContain("Year");
    }
}

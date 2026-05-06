package com.fivefy.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationExtractorTest {

    private final CitationExtractor extractor = new CitationExtractor();

    @Test
    @DisplayName("[N] 패턴을 등장 순서대로 추출")
    void extractsInOrder() {
        String text = "[3]을 듣다가 [1]도 좋아요. [3]은 특히...";

        List<Integer> result = extractor.extract(text);

        // [3]이 두 번 나와도 한 번만 (LinkedHashSet)
        assertThat(result).containsExactly(3, 1);
    }

    @Test
    @DisplayName("LLM이 한국어 응답 사이에 인용한 케이스")
    void koreanResponse() {
        String text = "잠 안 올 때는 차분한 곡들이 좋아요! [4] **Through the Night**는 IU의 부드러운 목소리가 마음을 진정시켜주고, [3] **Hymn for the Weekend**는 신비로운 분위기로 천천히 수면으로 이끌어 줄 거예요.";

        List<Integer> result = extractor.extract(text);

        assertThat(result).containsExactly(4, 3);
    }

    @Test
    @DisplayName("빈 문자열이나 null은 빈 리스트")
    void handlesEmptyInput() {
        assertThat(extractor.extract(null)).isEmpty();
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract("   ")).isEmpty();
    }

    @Test
    @DisplayName("인용이 전혀 없는 응답은 빈 리스트")
    void noCitations() {
        String text = "죄송하지만 추천드릴 곡이 없어요.";

        assertThat(extractor.extract(text)).isEmpty();
    }

    @Test
    @DisplayName("잘못된 패턴은 무시 - [a], [], [-1] 등")
    void ignoresInvalidPatterns() {
        String text = "[a]는 [b]가 [] [-1] 잘못된 패턴이고 [3]만 유효해요";

        List<Integer> result = extractor.extract(text);

        assertThat(result).containsExactly(3);
    }

    @Test
    @DisplayName("filterCited - 인용된 트랙만 등장 순서대로 반환")
    void filterCited_returnsOnlyCitedInOrder() {
        List<String> tracks = List.of("Bohemian Rhapsody", "Yesterday", "Hotel California",
                "Spring Day", "Through the Night", "Hello",
                "Hymn for the Weekend", "Happier Than Ever");

        // LLM이 [4]Through the Night, [2]Yesterday를 인용했다고 가정
        // 주의: tracks는 1-based이므로 [2] = "Yesterday" (index 1)
        String responseText = "[2]가 가장 슬프고, [5]도 잘 어울려요";

        List<String> cited = extractor.filterCited(tracks, responseText);

        // 응답에 등장한 순서: [2] = Yesterday, [5] = Through the Night
        assertThat(cited).containsExactly("Yesterday", "Through the Night");
    }

    @Test
    @DisplayName("filterCited - 인용 없으면 빈 리스트 (호출자가 fallback 결정)")
    void filterCited_returnsEmptyWhenNoCitation() {
        List<String> tracks = List.of("A", "B", "C");

        List<String> cited = extractor.filterCited(tracks, "추천할 곡이 없어요");

        assertThat(cited).isEmpty();
    }

    @Test
    @DisplayName("filterCited - 범위 밖 번호는 무시")
    void filterCited_ignoresOutOfRange() {
        List<String> tracks = List.of("A", "B", "C");

        // [99]는 범위 밖이라 무시되어야 함
        List<String> cited = extractor.filterCited(tracks, "[1]과 [99]를 들어보세요. [3]도 좋아요");

        assertThat(cited).containsExactly("A", "C");
    }

    @Test
    @DisplayName("filterCited - 빈 트랙 리스트")
    void filterCited_emptyTracks() {
        assertThat(extractor.filterCited(List.<String>of(), "[1]을 들어봐요")).isEmpty();
    }
}

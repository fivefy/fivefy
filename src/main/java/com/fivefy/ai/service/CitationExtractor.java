package com.fivefy.ai.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 응답에서 트랙 인용 번호를 추출.
 *
 * RagPromptBuilder가 LLM에게 [숫자] 형식으로 인용하도록 강제하는데,
 * 이 클래스는 그 숫자를 다시 파싱해서 *실제로 인용된 트랙만* 식별.
 *
 * 사용처:
 *   - ChatService에서 응답 완료 후 인용된 트랙만 TRACKS 이벤트로 노출
 */
@Component
public class CitationExtractor {
    /**
     * [숫자] 패턴.
     *
     * 매칭 예시:
     *   [1], [12], [4]번 → 모두 매칭
     *
     * 비매칭 예시:
     *   [a], [], [1.5], [-1] → 매칭 안 됨 (숫자만, 공백 X)
     *   "abc[3]def" → 매칭됨 (앞뒤 어떤 문자든 OK)
     */
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    /**
     * 응답 텍스트에서 인용된 번호 추출.
     *
     * @return 등장 순서대로 정렬된 distinct 번호 (1-based)
     *         e.g. "[3]을 듣다가 [1]도 좋아요. [3]은 특히..." → [3, 1]
     */
    public List<Integer> extract(String text) {
        if (text == null || text.isBlank()) return List.of();

        // LinkedHashSet: 등장 순서 유지하면서 중복 제거
        LinkedHashSet<Integer> citations = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                citations.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                // \d+ 매칭이라 발생할 일 없지만 방어
            }
        }
        return citations.stream().toList();
    }

    /**
     * 후보 트랙 리스트와 응답 텍스트를 받아 *인용된 트랙만* 반환.
     *
     * @param tracks 후보 트랙 (1-based 번호 부여 순서대로)
     * @param responseText LLM 응답
     * @return 인용된 트랙 (응답에 등장한 순서)
     */
    public <T> List<T> filterCited(List<T> tracks, String responseText) {
        if (tracks.isEmpty()) return List.of();

        List<Integer> citations = extract(responseText);
        if (citations.isEmpty()) {
            // LLM이 번호 인용을 안 한 경우 — 안전하게 빈 결과
            // (RagPromptBuilder에서 번호 인용을 강제하지만 LLM이 가끔 어김)
            return List.of();
        }

        return citations.stream()
                .filter(n -> n >= 1 && n <= tracks.size())  // out-of-range 방어
                .map(n -> tracks.get(n - 1))                 // 1-based → 0-based
                .toList();
    }
}

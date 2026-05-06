package com.fivefy.ai.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitationExtractor {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

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

    public <T> List<T> filterCited(List<T> tracks, String responseText) {
        if (tracks.isEmpty()) return List.of();

        List<Integer> citations = extract(responseText);
        if (citations.isEmpty()) {
            // LLM이 번호 인용을 안 한 경우 — 안전하게 빈 결과
            return List.of();
        }

        return citations.stream()
                .filter(n -> n >= 1 && n <= tracks.size())  // out-of-range 방어
                .map(n -> tracks.get(n - 1))                 // 1-based → 0-based
                .toList();
    }
}

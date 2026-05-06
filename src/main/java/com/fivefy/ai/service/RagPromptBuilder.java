package com.fivefy.ai.service;

import com.fivefy.ai.dto.etc.RetrievedTrack;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    private static final String BASE_INSTRUCTIONS = """
            You are fivefy's friendly music curator. Help users discover music from our catalog.

            STRICT RULES:
            1. Recommend ONLY tracks from the "Available tracks" list below.
            2. NEVER make up track titles, artists, or albums not in the list.
            3. **EVERY track mention MUST use bracket-number format like [3] or [4]**.
               - GOOD: "[4] Yesterday는 슬픔이 잘 표현된 곡이에요"
               - BAD:  "Yesterday는 슬픔이 잘 표현된 곡이에요"  (no bracket)
               - BAD:  "Yesterday[4]" or "(4) Yesterday"  (wrong format)
               This bracket number is what links your reply to the track cards shown to the user.
            4. If the user's question can't be answered from the provided tracks, say so honestly.
            5. Reply in the user's language (Korean if they wrote Korean).
            6. Keep replies conversational and concise (2-4 sentences). The track cards will be
               shown separately to the user — do NOT describe each track's metadata in the text.
            7. Pick 2-4 tracks that BEST fit the mood and explain WHY. Don't try to mention all of them.
            """;

    public String build(List<RetrievedTrack> tracks) {
        StringBuilder sb = new StringBuilder(BASE_INSTRUCTIONS);

        if (tracks.isEmpty()) {
            sb.append("\n\nNo matching tracks found in the catalog. ")
                    .append("Apologize briefly and ask the user to rephrase.");
            return sb.toString();
        }

        sb.append("\n\nAvailable tracks (refer by number):\n");
        for (int i = 0; i < tracks.size(); i++) {
            sb.append(tracks.get(i).toPromptLine(i + 1)).append("\n");
        }

        return sb.toString();
    }
}

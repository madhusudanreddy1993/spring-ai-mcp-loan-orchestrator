package com.example.loanmcp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JsonParserUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Safely parses LLM response text into a typed object.
     * Strips markdown code fences that models sometimes emit (```json ... ```).
     * Falls back gracefully on malformed output rather than crashing.
     */
    public <T> Optional<T> parse(String raw, Class<T> type) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            // Strip markdown code fences common in LLM outputs
            String cleaned = raw
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("```", "")
                    .trim();

            // Extract first JSON object if surrounded by prose
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start == -1 || end == -1) return Optional.empty();

            return Optional.of(mapper.readValue(cleaned.substring(start, end + 1), type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

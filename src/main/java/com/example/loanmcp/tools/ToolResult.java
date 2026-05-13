package com.example.loanmcp.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Generic wrapper for all tool responses.
 * Enables uniform handling of tool outcomes in the AI orchestration layer.
 * The LLM receives a consistent JSON shape regardless of which tool was called.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult<T> {

    private final String toolName;
    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final long durationMs;
    private final Instant timestamp;

    public static <T> ToolResult<T> success(String toolName, T data, long durationMs) {
        return ToolResult.<T>builder()
                .toolName(toolName)
                .success(true)
                .data(data)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ToolResult<T> failure(String toolName, String errorMessage, long durationMs) {
        return ToolResult.<T>builder()
                .toolName(toolName)
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .timestamp(Instant.now())
                .build();
    }
}

package com.avadhoot.workforge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Global response envelope: { success, data, message, timestamp, traceId }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp,
        String traceId
) {
    private static String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "n/a";
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), currentTraceId());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), currentTraceId());
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, Instant.now(), currentTraceId());
    }

    public static <T> ApiResponse<T> error(T data, String message) {
        return new ApiResponse<>(false, data, message, Instant.now(), currentTraceId());
    }
}

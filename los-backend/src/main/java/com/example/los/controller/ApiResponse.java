package com.example.los.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * ApiResponse<T> — wrapper chuẩn cho tất cả HTTP response.
 *
 * Cấu trúc:
 * {
 *   "success": true,
 *   "message": "...",
 *   "data": { ... }       ← null nếu không có data (JsonInclude.NON_NULL bỏ qua)
 * }
 *
 * Dùng static factory methods để giữ immutability:
 *   ApiResponse.success(data)
 *   ApiResponse.success(message)
 *   ApiResponse.success(message, data)
 *   ApiResponse.error(message)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String  message;
    private final T       data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // ─── Factory methods ────────────────────────────────────────

    /** Thành công, có data, không có message */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /** Thành công, chỉ có message (Void) */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    /** Thành công, có cả message lẫn data */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** Lỗi, chỉ có message */
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    /** Lỗi, có cả message và data */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}

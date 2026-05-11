package com.rental.crm.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 공통 API 응답 래퍼 — 07 API 명세서 §0-3.
 *
 * @param code    SUCCESS / 에러 코드 (§0-4)
 * @param message 사용자 표시 메시지
 * @param data    실제 페이로드 (null 가능)
 * @param errors  필드별 검증 오류 (실패 시에만)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        List<FieldError> errors
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("SUCCESS", "OK", data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>("SUCCESS", message, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> errors) {
        return new ApiResponse<>(code, message, null, errors);
    }

    public record FieldError(String field, String reason) {}
}

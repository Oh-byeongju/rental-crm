package com.rental.domain.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 코드 카탈로그 — 07 API 명세서 §0-4.
 * 신규 코드 추가 시 본 파일 + 07 API 명세서 §0-4 동기화 필수.
 */
public enum ErrorCode {

    VALIDATION_FAILED        (HttpStatus.BAD_REQUEST,             "입력값 검증 실패"),
    INVALID_REQUEST          (HttpStatus.BAD_REQUEST,             "잘못된 요청"),

    UNAUTHORIZED             (HttpStatus.UNAUTHORIZED,            "인증이 필요합니다"),
    LOGIN_FAILED             (HttpStatus.UNAUTHORIZED,            "이메일 또는 비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED           (HttpStatus.UNAUTHORIZED,            "5회 연속 실패로 계정이 잠금되었습니다"),

    FORBIDDEN                (HttpStatus.FORBIDDEN,                "권한이 부족합니다"),
    NOT_FOUND                (HttpStatus.NOT_FOUND,                "리소스를 찾을 수 없습니다"),

    CONFLICT                 (HttpStatus.CONFLICT,                 "상태 충돌"),
    ALREADY_EXISTS           (HttpStatus.CONFLICT,                 "이미 존재합니다"),
    BATCH_ALREADY_RUN        (HttpStatus.CONFLICT,                 "이미 실행된 배치입니다"),

    BUSINESS_RULE            (HttpStatus.UNPROCESSABLE_ENTITY,     "비즈니스 규칙 위반"),

    INTERNAL_ERROR           (HttpStatus.INTERNAL_SERVER_ERROR,    "서버 오류"),
    EXTERNAL_SYSTEM_ERROR    (HttpStatus.SERVICE_UNAVAILABLE,      "외부 시스템 오류");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}

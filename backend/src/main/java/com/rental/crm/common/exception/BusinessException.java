package com.rental.crm.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외. 발생 시 {@link com.rental.crm.common.exception.GlobalExceptionHandler}
 * 가 HTTP 상태와 응답 코드로 변환.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

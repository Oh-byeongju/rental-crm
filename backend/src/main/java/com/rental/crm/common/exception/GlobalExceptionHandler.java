package com.rental.crm.common.exception;

import com.rental.crm.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        var code = ex.getErrorCode();
        log.warn("BusinessException: {} - {}", code.name(), ex.getMessage());
        return ResponseEntity
                .status(code.status())
                .body(ApiResponse.error(code.name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED.name(),
                        ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(cv -> new ApiResponse.FieldError(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_FAILED.name(),
                        ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        errors));
    }

    /**
     * 매핑되지 않은 URL 접근 → 404.
     * Spring Boot 가 정적 리소스로 fallback 시도하다 못 찾으면 본 예외 발생.
     * GlobalExceptionHandler 의 catch-all (Exception.class) 가 잡아서 500 으로 반환하는 것 방지.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ErrorCode.NOT_FOUND.name(),
                        "요청하신 경로를 찾을 수 없습니다: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.name(),
                        ErrorCode.INTERNAL_ERROR.defaultMessage()));
    }
}

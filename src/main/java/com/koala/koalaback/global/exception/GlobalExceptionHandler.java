package com.koala.koalaback.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest req) {
        ErrorCode ec = e.getErrorCode();
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), e.getMessage()));
    }

    // @Valid Body 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        ErrorCode ec = ErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), fieldErrors));
    }

    // PathVariable/RequestParam 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), e.getMessage()));
    }

    // DB 제약 조건 위반 (Check constraint, Unique constraint 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String userMessage;

        if (msg.contains("ck_skus_prices")) {
            userMessage = "가격 오류: 판매가는 정가보다 클 수 없습니다. (판매가 ≤ 정가)";
        } else if (msg.contains("Duplicate entry") || msg.contains("unique")) {
            userMessage = "이미 사용 중인 값입니다. (슬러그 또는 코드 중복)";
        } else {
            userMessage = "데이터 저장 중 오류가 발생했습니다. 입력값을 확인해 주세요.";
        }

        log.warn("[DataIntegrity] {} {} — {}", req.getMethod(), req.getRequestURI(), msg);
        ErrorCode ec = ErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), userMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("[500] {} {} — {}: {}", req.getMethod(), req.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage(), e);
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI()));
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        Object rejected = fe.getRejectedValue();
        return new ErrorResponse.FieldError(
                fe.getField(),
                rejected,
                fe.getDefaultMessage()
        );
    }
}
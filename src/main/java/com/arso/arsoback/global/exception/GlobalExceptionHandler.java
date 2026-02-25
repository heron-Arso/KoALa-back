package com.arso.arsoback.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(e.getErrorCode(), request.getRequestURI());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(body);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(body);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleSpringErrorResponseException(ErrorResponseException e, HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        ErrorCode code = switch (status) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            default -> ErrorCode.BAD_REQUEST;
        };

        String detail = null;
        if (e.getBody() != null) {
            detail = e.getBody().getDetail(); // ✅ 여기서 reason/detail 가져옴
        }
        if (detail == null || detail.isBlank()) {
            detail = code.getMessage();
        }

        ErrorResponse body = ErrorResponse.of(code, request.getRequestURI(), detail);
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        // TODO: 로그는 logback-spring.xml + logger로 남기면 됨 (지금은 MVP라 단순 처리)
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()).body(body);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(
                fe.getField(),
                fe.getRejectedValue(),
                Objects.toString(fe.getDefaultMessage(), "Invalid value")
        );
    }
}
package com.arso.arsoback.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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

        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), fieldErrors));
    }

    // PathVariable/RequestParam 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, req.getRequestURI(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
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
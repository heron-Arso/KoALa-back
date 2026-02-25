package com.arso.arsoback.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "Bad request."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COMMON-400-VALID", "Validation error."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "Resource not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "Method not allowed."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "Internal server error."),

    // Domain examples (필요할 때 추가)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-404", "User not found."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-404", "Product not found.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
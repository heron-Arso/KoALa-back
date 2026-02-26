package com.arso.arsoback.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다."),

    // SKU
    SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "SKU_404", "SKU를 찾을 수 없습니다."),

    // ORDER
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT, "ORDER_409", "이미 결제 완료된 주문입니다."),

    // PAYMENT
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_400", "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "PAYMENT_502", "결제사 검증에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
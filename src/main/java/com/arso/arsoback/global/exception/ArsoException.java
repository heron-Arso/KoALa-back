package com.arso.arsoback.global.exception;

public class ArsoException extends RuntimeException {

    private final ErrorCode errorCode;

    public ArsoException(ErrorCode errorCode) {
        super(errorCode.getMessage());  // ✅ getter 스타일로 수정
        this.errorCode = errorCode;
    }

    public ArsoException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {   // ✅ getter 스타일로 통일
        return errorCode;
    }
}
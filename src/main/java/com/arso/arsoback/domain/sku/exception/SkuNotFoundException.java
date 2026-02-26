package com.arso.arsoback.domain.sku.exception;

import com.arso.arsoback.global.exception.ArsoException;
import com.arso.arsoback.global.exception.ErrorCode;

public class SkuNotFoundException extends ArsoException {
    public SkuNotFoundException() {
        super(ErrorCode.SKU_NOT_FOUND);
    }
}
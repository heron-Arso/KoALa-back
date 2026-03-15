package com.koala.koalaback.domain.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    String getProviderCode();

    PaymentConfirmResult confirm(String paymentKey, String orderId, BigDecimal amount);

    PaymentCancelResult cancel(String pgTransactionId, BigDecimal cancelAmount, String reason);

    record PaymentConfirmResult(
            boolean success,
            String pgTransactionId,
            String approvalNo,
            BigDecimal approvedAmount,
            String rawResponse,
            String failureCode,
            String failureMessage
    ) {}

    record PaymentCancelResult(
            boolean success,
            BigDecimal cancelledAmount,
            String rawResponse,
            String failureCode,
            String failureMessage
    ) {}
}
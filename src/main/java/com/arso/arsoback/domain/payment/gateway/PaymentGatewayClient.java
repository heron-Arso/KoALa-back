package com.arso.arsoback.domain.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGatewayClient {

    // 결제사에 paymentKey로 조회해서 실제 결제금액을 받아온다고 가정
    VerifiedPayment verify(String paymentKey);

    record VerifiedPayment(
            String paymentKey,
            BigDecimal amount,
            boolean success
    ) {}
}
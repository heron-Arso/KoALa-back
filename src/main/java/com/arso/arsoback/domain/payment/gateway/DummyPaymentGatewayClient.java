package com.arso.arsoback.domain.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DummyPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public VerifiedPayment verify(String paymentKey) {
        // ✅ 지금은 PG 연동 안 하니까 "항상 성공" 가짜 구현
        // 나중에 토스/포트원 붙일 때 여기만 교체하면 된다.
        return new VerifiedPayment(paymentKey, new BigDecimal("10000.00"), true);
    }
}
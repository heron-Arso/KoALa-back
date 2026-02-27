package com.arso.arsoback.domain.payment.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DummyPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public VerifiedPayment verify(String paymentKey) {
        // 형식: DUMMY_15000 또는 DUMMY_15000.50
        // 파싱 실패하면 success=false 로 처리
        try {
            BigDecimal amount = parseAmount(paymentKey);
            return new VerifiedPayment(paymentKey, amount, true);
        } catch (Exception e) {
            return new VerifiedPayment(paymentKey, null, false);
        }
    }

    private BigDecimal parseAmount(String paymentKey) {
        String[] parts = paymentKey.split("_");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid dummy paymentKey");
        }
        return new BigDecimal(parts[1]);
    }
}
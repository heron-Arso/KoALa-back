package com.koala.koalaback.domain.payment.provider;

/**
 * KakaoPay 직접 연동 Provider — 미구현 (NOT a Spring bean)
 *
 * 현재 KakaoPay 결제는 TossPayments easyPay 옵션을 통해 처리됩니다.
 * ({@link TossPaymentProvider} 가 결제 승인·취소를 담당)
 *
 * KakaoPay 직접 API 연동이 필요해질 경우:
 *   1. {@link PaymentProvider} 인터페이스를 구현하세요.
 *   2. {@code @Component} 또는 {@code @Service} 애노테이션을 추가하세요.
 *   3. {@code PaymentService} 의 provider 선택 로직에 providerCode 를 등록하세요.
 *
 * @see TossPaymentProvider
 * @see PaymentProvider
 */
// TODO: KakaoPay 직접 연동 시 PaymentProvider 구현 및 @Component 추가
public class KakaoPayProvider {
    // 미구현 — KakaoPay 는 현재 Toss easyPay 를 통해 처리됨
}

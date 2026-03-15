package com.koala.koalaback.api.payment;

import com.koala.koalaback.domain.payment.service.PaymentService;
import com.koala.koalaback.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/toss")
    public ApiResponse<Void> tossWebhook(@RequestBody String payload) {
        paymentService.handleWebhook("TOSS", payload);
        return ApiResponse.ok();
    }

    @PostMapping("/kakaopay")
    public ApiResponse<Void> kakaoPayWebhook(@RequestBody String payload) {
        paymentService.handleWebhook("KAKAOPAY", payload);
        return ApiResponse.ok();
    }
}
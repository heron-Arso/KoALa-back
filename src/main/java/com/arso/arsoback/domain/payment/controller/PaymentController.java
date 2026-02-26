package com.arso.arsoback.domain.payment.controller;

import com.arso.arsoback.domain.payment.dto.PaymentConfirmRequest;
import com.arso.arsoback.domain.payment.dto.PaymentConfirmResponse;
import com.arso.arsoback.domain.payment.service.PaymentService;
import com.arso.arsoback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ApiResponse<PaymentConfirmResponse> confirm(@Valid @RequestBody PaymentConfirmRequest request) {
        return ApiResponse.ok(paymentService.confirm(request));
    }
}
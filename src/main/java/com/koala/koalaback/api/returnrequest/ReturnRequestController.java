package com.koala.koalaback.api.returnrequest;

import com.koala.koalaback.domain.returnrequest.dto.ReturnRequestDto;
import com.koala.koalaback.domain.returnrequest.service.ReturnRequestService;
import com.koala.koalaback.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    /** 반품/교환 신청 */
    @PostMapping
    public ApiResponse<ReturnRequestDto.ReturnResponse> createReturn(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReturnRequestDto.CreateRequest req) {
        return ApiResponse.ok(returnRequestService.createReturnRequest(userId, req));
    }

    /** 내 반품/교환 목록 */
    @GetMapping
    public ApiResponse<List<ReturnRequestDto.ReturnResponse>> getMyReturns(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(returnRequestService.getMyReturnRequests(userId));
    }

    /** 특정 주문의 반품 상태 */
    @GetMapping("/order/{orderNo}")
    public ApiResponse<ReturnRequestDto.ReturnResponse> getReturnByOrder(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        return ApiResponse.ok(returnRequestService.getMyReturnByOrderNo(userId, orderNo));
    }
}

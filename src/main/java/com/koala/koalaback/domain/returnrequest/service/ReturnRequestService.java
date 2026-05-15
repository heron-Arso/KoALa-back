package com.koala.koalaback.domain.returnrequest.service;

import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.repository.OrderRepository;
import com.koala.koalaback.domain.payment.dto.PaymentDto;
import com.koala.koalaback.domain.payment.repository.PaymentRepository;
import com.koala.koalaback.domain.payment.service.PaymentService;
import com.koala.koalaback.domain.returnrequest.dto.ReturnRequestDto;
import com.koala.koalaback.domain.returnrequest.entity.ReturnRequest;
import com.koala.koalaback.domain.returnrequest.repository.ReturnRequestRepository;
import com.koala.koalaback.domain.sku.service.StockService;
import com.koala.koalaback.domain.user.entity.User;
import com.koala.koalaback.domain.user.service.UserService;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.global.response.PageResponse;
import com.koala.koalaback.global.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final CodeGenerator codeGenerator;

    /** 사용자 — 반품/교환 신청 */
    @Transactional
    public ReturnRequestDto.ReturnResponse createReturnRequest(Long userId, ReturnRequestDto.CreateRequest req) {
        Order order = orderRepository.findByOrderNoAndUserId(req.getOrderNo(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 배송완료 상태만 반품 신청 가능
        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_NOT_ALLOWED);
        }

        // 이미 진행 중인 반품 요청이 있는지 확인 (REJECTED 제외)
        boolean alreadyExists = returnRequestRepository
                .existsByOrderIdAndStatusNot(order.getId(), "REJECTED");
        if (alreadyExists) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_ALREADY_EXISTS);
        }

        User user = userService.getUserById(userId);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNo(codeGenerator.generateReturnNo())
                .order(order)
                .user(user)
                .returnType(req.getReturnType())
                .reason(req.getReason())
                .reasonDetail(req.getReasonDetail())
                .build();

        returnRequestRepository.save(returnRequest);
        log.info("Return request created: returnNo={}, orderNo={}, userId={}",
                returnRequest.getReturnNo(), req.getOrderNo(), userId);

        return ReturnRequestDto.ReturnResponse.from(returnRequest);
    }

    /** 사용자 — 내 반품 목록 */
    public List<ReturnRequestDto.ReturnResponse> getMyReturnRequests(Long userId) {
        return returnRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReturnRequestDto.ReturnResponse::from)
                .toList();
    }

    /** 사용자 — 특정 주문의 반품 상태 확인 */
    public ReturnRequestDto.ReturnResponse getMyReturnByOrderNo(Long userId, String orderNo) {
        Order order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return returnRequestRepository.findTopByOrderIdOrderByCreatedAtDesc(order.getId())
                .map(ReturnRequestDto.ReturnResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
    }

    /** 관리자 — 전체 반품 목록 */
    public PageResponse<ReturnRequestDto.ReturnResponse> getAdminReturnRequests(String status, Pageable pageable) {
        return PageResponse.of(
                returnRequestRepository.findByStatusFilter(status, pageable)
                        .map(ReturnRequestDto.ReturnResponse::from)
        );
    }

    /** 관리자 — 반품 상세 */
    public ReturnRequestDto.ReturnResponse getAdminReturnDetail(String returnNo) {
        return ReturnRequestDto.ReturnResponse.from(getByReturnNo(returnNo));
    }

    /** 관리자 — 승인 또는 거절 처리 */
    @Transactional
    public ReturnRequestDto.ReturnResponse processReturnRequest(String returnNo, ReturnRequestDto.AdminProcessRequest req) {
        ReturnRequest returnRequest = getByReturnNo(returnNo);

        if (!"REQUESTED".equals(returnRequest.getStatus())) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_NOT_ALLOWED);
        }

        if ("APPROVE".equals(req.getAction())) {
            // 환불 금액: 명시하면 그 금액, 없으면 전액
            BigDecimal refundAmt = req.getRefundAmount() != null
                    ? req.getRefundAmount()
                    : returnRequest.getOrder().getTotalAmount();

            returnRequest.approve(refundAmt, req.getAdminMemo());

            // 반품(RETURN) 승인 시 재고 복구 — 교환(EXCHANGE)은 교환 완료 처리 시점에 별도 처리
            if ("RETURN".equals(returnRequest.getReturnType())) {
                returnRequest.getOrder().getOrderItems().forEach(item -> {
                    if (item.getSku() != null) {
                        stockService.restoreByReturn(item.getSku().getId(), item.getQuantity(), item.getId());
                    }
                });
                log.info("Stock restored on return approval: returnNo={}", returnNo);
            }

            // CAPTURED 결제 자동 환불
            paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(returnRequest.getOrder().getId())
                    .filter(p -> "CAPTURED".equals(p.getStatus()))
                    .ifPresent(p -> {
                        try {
                            paymentService.cancel(p.getPaymentNo(),
                                    new PaymentDto.CancelRequest("반품 승인 환불", refundAmt));
                            log.info("Return refund success: paymentNo={}, amount={}",
                                    p.getPaymentNo(), refundAmt);
                        } catch (Exception e) {
                            log.error("Return refund FAILED — manual action required: paymentNo={}, returnNo={}, error={}",
                                    p.getPaymentNo(), returnNo, e.getMessage());
                            paymentService.recordRefundFailure(p.getPaymentNo(),
                                    "반품 승인 환불 실패 — 수동처리 필요: " + e.getMessage());
                        }
                    });

            log.info("Return approved: returnNo={}, refundAmt={}", returnNo, refundAmt);

        } else if ("REJECT".equals(req.getAction())) {
            returnRequest.reject(req.getAdminMemo());
            log.info("Return rejected: returnNo={}", returnNo);

        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return ReturnRequestDto.ReturnResponse.from(returnRequest);
    }

    /** 관리자 — 완료 처리 (교환 완료 등) */
    @Transactional
    public ReturnRequestDto.ReturnResponse completeReturnRequest(String returnNo) {
        ReturnRequest returnRequest = getByReturnNo(returnNo);
        if (!"APPROVED".equals(returnRequest.getStatus())) {
            throw new BusinessException(ErrorCode.RETURN_REQUEST_NOT_ALLOWED);
        }
        // 교환(EXCHANGE) 완료 처리 시 재고 복구 — 반품은 APPROVE 시점에 이미 처리됨
        if ("EXCHANGE".equals(returnRequest.getReturnType())) {
            returnRequest.getOrder().getOrderItems().forEach(item -> {
                if (item.getSku() != null) {
                    stockService.restoreByReturn(item.getSku().getId(), item.getQuantity(), item.getId());
                }
            });
            log.info("Stock restored on exchange completion: returnNo={}", returnNo);
        }
        returnRequest.complete();
        log.info("Return completed: returnNo={}", returnNo);
        return ReturnRequestDto.ReturnResponse.from(returnRequest);
    }

    private ReturnRequest getByReturnNo(String returnNo) {
        return returnRequestRepository.findByReturnNo(returnNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
    }
}

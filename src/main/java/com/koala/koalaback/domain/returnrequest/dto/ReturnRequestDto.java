package com.koala.koalaback.domain.returnrequest.dto;

import com.koala.koalaback.domain.returnrequest.entity.ReturnRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReturnRequestDto {

    @Getter
    public static class CreateRequest {
        @NotBlank
        private String orderNo;      // 반품 대상 주문번호

        @NotBlank
        private String returnType;   // RETURN | EXCHANGE

        @NotBlank
        private String reason;       // SIMPLE_CHANGE | DEFECT | WRONG_DELIVERY | OTHER

        private String reasonDetail; // 상세 사유 (선택)
    }

    /** 관리자 처리 (승인/거절) */
    @Getter
    public static class AdminProcessRequest {
        @NotBlank
        private String action;       // APPROVE | REJECT

        private BigDecimal refundAmount; // 승인 시 환불 금액 (null = 전액)
        private String adminMemo;        // 처리 메모
    }

    @Getter
    @Builder
    public static class ReturnResponse {
        private Long   id;
        private String returnNo;
        private String orderNo;
        private String returnType;
        private String reason;
        private String reasonDetail;
        private String status;
        private BigDecimal refundAmount;
        private String adminMemo;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;

        // 관리자용 추가 필드
        private Long   userId;
        private String ordererName;
        private String ordererPhone;

        public static ReturnResponse from(ReturnRequest r) {
            return ReturnResponse.builder()
                    .id(r.getId())
                    .returnNo(r.getReturnNo())
                    .orderNo(r.getOrder().getOrderNo())
                    .returnType(r.getReturnType())
                    .reason(r.getReason())
                    .reasonDetail(r.getReasonDetail())
                    .status(r.getStatus())
                    .refundAmount(r.getRefundAmount())
                    .adminMemo(r.getAdminMemo())
                    .processedAt(r.getProcessedAt())
                    .createdAt(r.getCreatedAt())
                    .userId(r.getUser().getId())
                    .ordererName(r.getOrder().getOrdererName())
                    .ordererPhone(r.getOrder().getOrdererPhone())
                    .build();
        }
    }
}

package com.koala.koalaback.domain.payment.entity;

import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, unique = true, length = 40)
    private String paymentNo;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 30)
    private String method;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 13, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, precision = 13, scale = 2)
    private BigDecimal approvedAmount;

    @Column(nullable = false, precision = 13, scale = 2)
    private BigDecimal cancelledAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 100)
    private String pgTransactionId;

    @Column(length = 100)
    private String approvalNo;

    @Column(length = 100)
    private String failureCode;

    @Column(length = 255)
    private String failureMessage;

    private LocalDateTime approvedAt;
    private LocalDateTime failedAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "JSON")
    private String rawResponseJson;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentEvent> events = new ArrayList<>();

    @Builder
    public Payment(Order order, String paymentNo, String provider,
                   String method, BigDecimal requestedAmount, String currency) {
        this.order = order;
        this.paymentNo = paymentNo;
        this.provider = provider;
        this.method = method;
        this.status = "READY";
        this.requestedAmount = requestedAmount;
        this.approvedAmount = BigDecimal.ZERO;
        this.cancelledAmount = BigDecimal.ZERO;
        this.currency = currency != null ? currency : "KRW";
    }

    public void markCaptured(String pgTransactionId, String approvalNo,
                             BigDecimal approvedAmount, String rawResponseJson) {
        this.status = "CAPTURED";
        this.pgTransactionId = pgTransactionId;
        this.approvalNo = approvalNo;
        this.approvedAmount = approvedAmount;
        this.rawResponseJson = rawResponseJson;
        this.approvedAt = LocalDateTime.now();
    }

    public void markFailed(String failureCode, String failureMessage) {
        this.status = "FAILED";
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.failedAt = LocalDateTime.now();
    }

    public void markCancelled(BigDecimal cancelAmount) {
        this.cancelledAmount = this.cancelledAmount.add(cancelAmount);
        this.status = this.cancelledAmount.compareTo(this.approvedAmount) >= 0
                ? "CANCELLED" : "PARTIAL_REFUNDED";
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isCaptured() { return "CAPTURED".equals(this.status); }
}
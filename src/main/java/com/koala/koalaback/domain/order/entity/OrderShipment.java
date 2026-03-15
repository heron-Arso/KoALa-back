package com.koala.koalaback.domain.order.entity;

import com.koala.koalaback.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_shipments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderShipment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 30)
    private String recipientPhone;

    @Column(nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(length = 255)
    private String deliveryRequest;

    @Column(length = 50)
    private String carrierCode;

    @Column(length = 100)
    private String trackingNo;

    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @Builder
    public OrderShipment(Order order, String recipientName, String recipientPhone,
                         String zipCode, String address1, String address2,
                         String deliveryRequest) {
        this.order = order;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address1 = address1;
        this.address2 = address2;
        this.deliveryRequest = deliveryRequest;
    }

    public void registerTracking(String carrierCode, String trackingNo) {
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.shippedAt = LocalDateTime.now();
    }

    public void markDelivered() { this.deliveredAt = LocalDateTime.now(); }
}
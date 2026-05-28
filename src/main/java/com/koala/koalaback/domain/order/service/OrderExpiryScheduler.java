package com.koala.koalaback.domain.order.service;

import com.koala.koalaback.domain.order.entity.Order;
import com.koala.koalaback.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 미결제 주문 만료 자동취소 스케줄러.
 *
 * <p>주문 생성 시 재고가 즉시 차감되므로, 결제하지 않고 방치된 {@code PENDING_PAYMENT}
 * 주문은 재고를 계속 점유한다(특히 한정판 고갈 위험). 일정 시간(기본 30분) 이상 미결제
 * 상태인 주문을 주기적으로 자동 취소하고 재고를 복구한다.
 *
 * <p>실제 취소/복구는 {@link OrderService#expirePendingOrder(Long)} 가 주문별 독립
 * 트랜잭션으로 처리한다(별도 빈 호출이라 @Transactional 정상 적용).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /** 미결제 주문 만료 기준 (분). 기본 30분 */
    @Value("${order.pending-timeout-minutes:30}")
    private long timeoutMinutes;

    /** 미결제 만료 주문 스캔 — 기본 5분 주기(이전 실행 종료 후 대기, 중첩 방지), 부팅 1분 후 시작 */
    @Scheduled(
            fixedDelayString = "${order.expiry-scan-interval-ms:300000}",
            initialDelayString = "${order.expiry-scan-initial-delay-ms:60000}")
    public void releaseExpiredPendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> expired =
                orderRepository.findByOrderStatusAndCreatedAtBefore("PENDING_PAYMENT", threshold);
        if (expired.isEmpty()) return;

        log.info("[OrderExpiry] 만료 미결제 주문 {}건 처리 시작 (기준 {}분 경과)", expired.size(), timeoutMinutes);
        int success = 0;
        for (Order o : expired) {
            try {
                orderService.expirePendingOrder(o.getId());
                success++;
            } catch (Exception e) {
                log.error("[OrderExpiry] 주문 {} 만료 처리 실패: {}", o.getOrderNo(), e.getMessage(), e);
            }
        }
        log.info("[OrderExpiry] 처리 완료: {}/{}건 취소", success, expired.size());
    }
}

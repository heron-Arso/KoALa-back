package com.koala.koalaback.domain.payment.repository;

import com.koala.koalaback.domain.payment.dto.DailyRevenueProjection;
import com.koala.koalaback.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT COALESCE(SUM(p.approvedAmount), 0) FROM Payment p WHERE p.approvedAt >= :from AND p.status = 'CAPTURED'")
    BigDecimal sumApprovedAmountAfter(@Param("from") LocalDateTime from);

    @Query("SELECT COALESCE(SUM(p.approvedAmount), 0) FROM Payment p WHERE p.status = 'CAPTURED'")
    BigDecimal sumTotalApprovedAmount();

    /**
     * 네이티브 쿼리 + 인터페이스 프로젝션 사용 (Hibernate 7 JPQL new 생성자 표현식 비호환 우회)
     * order_count alias → Spring Data가 getOrderCount() 로 자동 매핑
     */
    @Query(value = """
        SELECT DATE_FORMAT(p.approved_at, '%Y-%m-%d')  AS date,
               COALESCE(SUM(p.approved_amount), 0)     AS revenue,
               COUNT(*)                                 AS order_count
        FROM payments p
        WHERE p.approved_at >= :from
          AND p.status = 'CAPTURED'
        GROUP BY DATE_FORMAT(p.approved_at, '%Y-%m-%d')
        ORDER BY DATE_FORMAT(p.approved_at, '%Y-%m-%d') ASC
        """, nativeQuery = true)
    List<DailyRevenueProjection> findDailyRevenueSince(@Param("from") LocalDateTime from);
}
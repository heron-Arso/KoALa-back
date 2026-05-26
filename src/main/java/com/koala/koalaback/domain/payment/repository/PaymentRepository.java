package com.koala.koalaback.domain.payment.repository;

import com.koala.koalaback.domain.payment.dto.DailyRevenueDto;
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

    @Query("""
        SELECT new com.koala.koalaback.domain.payment.dto.DailyRevenueDto(
            FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m-%d'),
            COALESCE(SUM(p.approvedAmount), 0),
            COUNT(p)
        )
        FROM Payment p
        WHERE p.approvedAt >= :from AND p.status = 'CAPTURED'
        GROUP BY FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m-%d') ASC
        """)
    List<DailyRevenueDto> findDailyRevenueSince(@Param("from") LocalDateTime from);
}
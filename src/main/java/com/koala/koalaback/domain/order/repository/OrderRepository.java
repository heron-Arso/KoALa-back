package com.koala.koalaback.domain.order.repository;

import com.koala.koalaback.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByOrderNoAndUserId(String orderNo, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 회원ID / 주문자명 / 전화번호 중 하나라도 일치하면 반환 */
    @Query("SELECT o FROM Order o WHERE " +
           "(:userId   IS NULL OR o.user.id      = :userId) AND " +
           "(:name     IS NULL OR o.ordererName  LIKE %:name%) AND " +
           "(:phone    IS NULL OR o.ordererPhone LIKE %:phone%) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(
            @Param("userId") Long userId,
            @Param("name")   String name,
            @Param("phone")  String phone,
            Pageable pageable);
}
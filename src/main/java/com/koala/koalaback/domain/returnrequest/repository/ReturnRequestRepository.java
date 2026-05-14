package com.koala.koalaback.domain.returnrequest.repository;

import com.koala.koalaback.domain.returnrequest.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByReturnNo(String returnNo);

    /** 특정 주문에 대한 반품 요청 존재 여부 */
    boolean existsByOrderIdAndStatusNot(Long orderId, String status);

    /** 내 반품 목록 */
    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 관리자 전체 목록 */
    Page<ReturnRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 특정 주문의 반품 요청 조회 */
    Optional<ReturnRequest> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    /** 관리자 검색 — 상태 필터 */
    @Query("SELECT r FROM ReturnRequest r WHERE (:status IS NULL OR r.status = :status) ORDER BY r.createdAt DESC")
    Page<ReturnRequest> findByStatusFilter(@Param("status") String status, Pageable pageable);
}

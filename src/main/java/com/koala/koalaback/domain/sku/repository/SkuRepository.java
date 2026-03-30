package com.koala.koalaback.domain.sku.repository;

import com.koala.koalaback.domain.sku.entity.Sku;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkuRepository extends JpaRepository<Sku, Long> {

    Optional<Sku> findBySkuCode(String skuCode);

    Optional<Sku> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Sku> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<Sku> findByArtistIdAndStatusAndDeletedAtIsNull(Long artistId, String status, Pageable pageable);

    /** 장르 필터 */
    @Query("SELECT s FROM Sku s WHERE s.genre = :genre AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    Page<Sku> findActiveByGenre(@Param("genre") String genre, Pageable pageable);

    /** 키워드 검색 (name, description) */
    @Query("""
        SELECT s FROM Sku s
        WHERE s.status = 'ACTIVE'
          AND s.deletedAt IS NULL
          AND (s.name LIKE %:keyword% OR s.description LIKE %:keyword%)
        """)
    Page<Sku> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** 한정판 목록 */
    @Query("SELECT s FROM Sku s WHERE s.isLimitedEdition = true AND s.status = 'ACTIVE' AND s.deletedAt IS NULL")
    Page<Sku> findLimitedEditions(Pageable pageable);

    /** 장르별 상품 수 (ACTIVE, 미삭제) */
    @Query("SELECT s.genre, COUNT(s) FROM Sku s WHERE s.status = 'ACTIVE' AND s.deletedAt IS NULL GROUP BY s.genre")
    List<Object[]> countByGenre();

    /** 전체 ACTIVE 상품 수 */
    long countByStatusAndDeletedAtIsNull(String status);

    /** 어드민 전체 조회 (삭제 포함 옵션) */
    Page<Sku> findByDeletedAtIsNull(Pageable pageable);

    /** skuCode 목록으로 일괄 조회 — 장바구니/주문 검증용 */
    @Query("SELECT s FROM Sku s WHERE s.skuCode IN :skuCodes")
    List<Sku> findAllBySkuCodeIn(@Param("skuCodes") List<String> skuCodes);
}


package com.koala.koalaback.domain.sku.repository;

import com.koala.koalaback.domain.sku.entity.SkuMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkuMediaRepository extends JpaRepository<SkuMedia, Long> {

    /** 360도 프레임 — angle_degree 오름차순 */
    List<SkuMedia> findBySkuIdAndMediaRoleOrderByAngleDegreeAsc(Long skuId, String mediaRole);

    /** 전체 미디어 — role, sortOrder 순 */
    List<SkuMedia> findBySkuIdOrderByMediaRoleAscSortOrderAsc(Long skuId);

    /** 대표 이미지 단건 */
    Optional<SkuMedia> findBySkuIdAndIsPrimaryTrue(Long skuId);

    /** role 내 sortOrder 순 */
    List<SkuMedia> findBySkuIdAndMediaRoleOrderBySortOrderAsc(Long skuId, String mediaRole);

    /** 특정 role 전체 삭제 — 360도 덮어쓰기 시 사용 */
    @Modifying
    @Query("DELETE FROM SkuMedia m WHERE m.sku.id = :skuId AND m.mediaRole = :mediaRole")
    void deleteBySkuIdAndMediaRole(@Param("skuId") Long skuId,
                                   @Param("mediaRole") String mediaRole);

    /** SKU 삭제 시 전체 미디어 삭제 */
    @Modifying
    @Query("DELETE FROM SkuMedia m WHERE m.sku.id = :skuId")
    void deleteAllBySkuId(@Param("skuId") Long skuId);
}


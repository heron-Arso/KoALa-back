package com.koala.koalaback.domain.sku.service;

import com.koala.koalaback.domain.sku.entity.Sku;
import com.koala.koalaback.domain.sku.entity.SkuStockLedger;
import com.koala.koalaback.domain.sku.repository.SkuRepository;
import com.koala.koalaback.domain.sku.repository.SkuStockLedgerRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.infra.redis.StockCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final SkuStockLedgerRepository stockLedgerRepository;
    private final SkuRepository skuRepository;
    private final StockCacheService stockCacheService;

    /**
     * 재고 조회 — Redis 캐시 우선, 없으면 DB 집계
     */
    @Transactional(readOnly = true)
    public int getStock(Long skuId) {
        return stockCacheService.getOrLoad(
                skuId,
                () -> stockLedgerRepository.sumDeltaBySkuId(skuId)
        );
    }

    /**
     * 초기 재고 설정
     */
    @Transactional
    public void initialize(Sku sku, int quantity, String memo) {
        record(sku, quantity, "INITIAL", null, null, memo);
        stockCacheService.evict(sku.getId());
    }

    /**
     * 주문 시 재고 차감 — 부족하면 예외
     */
    @Transactional
    public void deduct(Long skuId, int quantity, String refType, Long refId) {
        Sku sku = getSkuOrThrow(skuId);
        int current = getStock(skuId);
        if (current < quantity) {
            throw new BusinessException(ErrorCode.SKU_OUT_OF_STOCK);
        }
        record(sku, -quantity, "PURCHASE", refType, refId, null);
        stockCacheService.evict(skuId);

        if (current - quantity == 0) {
            sku.markOutOfStock();
        }
    }

    /**
     * 주문 취소 시 재고 복원
     */
    @Transactional
    public void restore(Long skuId, int quantity, String refType, Long refId) {
        Sku sku = getSkuOrThrow(skuId);
        record(sku, quantity, "CANCEL_RESTORE", refType, refId, null);
        stockCacheService.evict(skuId);

        if ("OUT_OF_STOCK".equals(sku.getStatus())) {
            sku.markActive();
        }
    }

    /**
     * 반품 시 재고 복원
     */
    @Transactional
    public void restoreByReturn(Long skuId, int quantity, Long orderItemId) {
        Sku sku = getSkuOrThrow(skuId);
        record(sku, quantity, "RETURN", "order_items", orderItemId, null);
        stockCacheService.evict(skuId);

        if ("OUT_OF_STOCK".equals(sku.getStatus())) {
            sku.markActive();
        }
    }

    /**
     * 어드민 수동 재고 조정
     */
    @Transactional
    public void adminAdjust(Long skuId, int delta, String memo) {
        Sku sku = getSkuOrThrow(skuId);
        record(sku, delta, "ADMIN_ADJUST", null, null, memo);
        stockCacheService.evict(skuId);

        int newStock = getStock(skuId);
        if (newStock <= 0) {
            sku.markOutOfStock();
        } else if ("OUT_OF_STOCK".equals(sku.getStatus())) {
            sku.markActive();
        }
        log.info("Admin stock adjust: skuId={}, delta={}, newStock={}",
                skuId, delta, newStock);
    }

    // ── Private helpers ───────────────────────────────────

    private Sku getSkuOrThrow(Long skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKU_NOT_FOUND));
    }

    private void record(Sku sku, int delta, String reason,
                        String refType, Long refId, String memo) {
        stockLedgerRepository.save(SkuStockLedger.builder()
                .sku(sku)
                .delta(delta)
                .reason(reason)
                .refType(refType)
                .refId(refId)
                .memo(memo)
                .build());
    }
}
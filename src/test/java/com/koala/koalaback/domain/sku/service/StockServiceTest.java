package com.koala.koalaback.domain.sku.service;

import com.koala.koalaback.domain.sku.entity.Sku;
import com.koala.koalaback.domain.sku.repository.SkuRepository;
import com.koala.koalaback.domain.sku.repository.SkuStockLedgerRepository;
import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import com.koala.koalaback.infra.redis.StockCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock private SkuStockLedgerRepository stockLedgerRepository;
    @Mock private SkuRepository skuRepository;
    @Mock private StockCacheService stockCacheService;

    @Test
    @DisplayName("재고 차감 성공")
    void deduct_success() {
        // given
        Long skuId = 1L;
        Sku sku = mock(Sku.class);
        given(sku.getStatus()).willReturn("ACTIVE");

        given(skuRepository.findById(skuId)).willReturn(Optional.of(sku));
        given(stockCacheService.getOrLoad(any(), any())).willReturn(10);
        given(stockLedgerRepository.save(any())).willReturn(null);

        // when
        stockService.deduct(skuId, 3, "order_items", null);

        // then
        then(stockLedgerRepository).should().save(any());
        then(stockCacheService).should().evict(skuId);
    }

    @Test
    @DisplayName("재고 차감 실패 — 재고 부족")
    void deduct_fail_out_of_stock() {
        // given
        Long skuId = 1L;
        Sku sku = mock(Sku.class);

        given(skuRepository.findById(skuId)).willReturn(Optional.of(sku));
        given(stockCacheService.getOrLoad(any(), any())).willReturn(2);

        // when & then
        assertThatThrownBy(() -> stockService.deduct(skuId, 5, "order_items", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SKU_OUT_OF_STOCK));
    }

    @Test
    @DisplayName("재고 복원 성공")
    void restore_success() {
        // given
        Long skuId = 1L;
        Sku sku = mock(Sku.class);
        given(sku.getStatus()).willReturn("ACTIVE");

        given(skuRepository.findById(skuId)).willReturn(Optional.of(sku));
        given(stockLedgerRepository.save(any())).willReturn(null);

        // when
        stockService.restore(skuId, 3, "order_items", 1L);

        // then
        then(stockLedgerRepository).should().save(any());
        then(stockCacheService).should().evict(skuId);
    }
}
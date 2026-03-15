package com.koala.koalaback.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 재고 캐시 저장
     */
    public void set(Long skuId, int stock) {
        try {
            redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + skuId, stock, TTL);
            log.debug("Stock cache set: skuId={}, stock={}", skuId, stock);
        } catch (Exception e) {
            log.warn("Stock cache set failed: skuId={}", skuId, e);
        }
    }

    /**
     * 재고 캐시 조회 — 없으면 null 반환
     */
    public Integer get(Long skuId) {
        try {
            Object val = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + skuId);
            return val != null ? (Integer) val : null;
        } catch (Exception e) {
            log.warn("Stock cache get failed: skuId={}", skuId, e);
            return null;
        }
    }

    /**
     * 재고 캐시 삭제 — 재고 변동 시 호출
     */
    public void evict(Long skuId) {
        try {
            redisTemplate.delete(STOCK_KEY_PREFIX + skuId);
            log.debug("Stock cache evict: skuId={}", skuId);
        } catch (Exception e) {
            log.warn("Stock cache evict failed: skuId={}", skuId, e);
        }
    }

    /**
     * 캐시 미스 시 DB 조회 후 캐시 저장 패턴
     * StockService.getStock()에서 호출
     */
    public int getOrLoad(Long skuId, java.util.function.Supplier<Integer> loader) {
        Integer cached = get(skuId);
        if (cached != null) {
            log.debug("Stock cache hit: skuId={}, stock={}", skuId, cached);
            return cached;
        }
        int stock = loader.get();
        set(skuId, stock);
        log.debug("Stock cache miss → loaded: skuId={}, stock={}", skuId, stock);
        return stock;
    }
}
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

    public void set(Long skuId, int stock) {
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + skuId, stock, TTL);
    }

    public Integer get(Long skuId) {
        Object val = redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + skuId);
        return val != null ? (Integer) val : null;
    }

    public void evict(Long skuId) {
        redisTemplate.delete(STOCK_KEY_PREFIX + skuId);
    }
}
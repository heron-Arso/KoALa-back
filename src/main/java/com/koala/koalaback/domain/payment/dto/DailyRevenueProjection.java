package com.koala.koalaback.domain.payment.dto;

import java.math.BigDecimal;

/**
 * 일별 매출 집계 네이티브 쿼리 프로젝션
 * 컬럼 alias → getter 매핑: date, revenue, order_count → getOrderCount()
 */
public interface DailyRevenueProjection {
    String getDate();
    BigDecimal getRevenue();
    Long getOrderCount();
}

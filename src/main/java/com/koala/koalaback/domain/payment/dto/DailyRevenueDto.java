package com.koala.koalaback.domain.payment.dto;

import java.math.BigDecimal;

/**
 * 일별 매출 집계 DTO — JPQL new 생성자 표현식용 (Hibernate 7 호환)
 */
public class DailyRevenueDto {

    private String date;
    private BigDecimal revenue;
    private long orderCount;

    public DailyRevenueDto(String date, BigDecimal revenue, long orderCount) {
        this.date = date;
        this.revenue = revenue;
        this.orderCount = orderCount;
    }

    public String getDate()         { return date; }
    public BigDecimal getRevenue()  { return revenue; }
    public long getOrderCount()     { return orderCount; }
}

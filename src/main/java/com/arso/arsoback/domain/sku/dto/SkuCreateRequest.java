package com.arso.arsoback.domain.sku.dto;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class SkuCreateRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
}
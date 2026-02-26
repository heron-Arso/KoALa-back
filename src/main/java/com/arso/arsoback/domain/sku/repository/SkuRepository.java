package com.arso.arsoback.domain.sku.repository;

import com.arso.arsoback.domain.sku.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {

}

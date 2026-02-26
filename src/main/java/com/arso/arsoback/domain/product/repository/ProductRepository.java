package com.arso.arsoback.domain.product.repository;

import com.arso.arsoback.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
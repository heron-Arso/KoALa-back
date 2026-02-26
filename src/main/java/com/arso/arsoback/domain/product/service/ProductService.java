package com.arso.arsoback.domain.product.service;

import com.arso.arsoback.domain.product.dto.ProductCreateRequest;
import com.arso.arsoback.domain.product.dto.ProductResponse;
import com.arso.arsoback.domain.product.dto.ProductUpdateRequest;
import com.arso.arsoback.domain.product.entity.Product;
import com.arso.arsoback.domain.product.mapper.ProductMapper;
import com.arso.arsoback.domain.product.repository.ProductRepository;
import com.arso.arsoback.global.exception.BusinessException;
import com.arso.arsoback.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductResponse create(ProductCreateRequest req) {
        Product saved = productRepository.save(productMapper.toEntity(req));
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse update(Long id, ProductUpdateRequest req) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        p.update(req.name(), req.artistName(), req.price(), req.limited(), req.thumbnailUrl());
        return productMapper.toResponse(p);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.deleteById(id);
    }
}
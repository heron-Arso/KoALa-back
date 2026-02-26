package com.arso.arsoback.domain.product.controller;

import com.arso.arsoback.domain.product.dto.ProductCreateRequest;
import com.arso.arsoback.domain.product.dto.ProductResponse;
import com.arso.arsoback.domain.product.dto.ProductUpdateRequest;
import com.arso.arsoback.domain.product.service.ProductService;
import com.arso.arsoback.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest req) {
        return ApiResponse.ok(productService.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list() {
        return ApiResponse.ok(productService.list());
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest req) {
        return ApiResponse.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }
}
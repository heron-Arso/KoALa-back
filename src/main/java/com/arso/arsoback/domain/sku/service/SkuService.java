package com.arso.arsoback.domain.sku.service;

import com.arso.arsoback.domain.sku.dto.SkuCreateRequest;
import com.arso.arsoback.domain.sku.dto.SkuResponse;
import com.arso.arsoback.domain.sku.dto.SkuUpdateRequest;
import com.arso.arsoback.domain.sku.entity.Sku;
import com.arso.arsoback.domain.sku.exception.SkuNotFoundException;
import com.arso.arsoback.domain.sku.repository.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkuService {

    private final SkuRepository skuRepository;

    @Transactional
    public SkuResponse create(SkuCreateRequest request) {
        Sku sku = new Sku(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        Sku saved = skuRepository.save(sku);
        return SkuResponse.from(saved);
    }

    public SkuResponse get(Long id) {
        Sku sku = skuRepository.findById(id)
                .orElseThrow(SkuNotFoundException::new);
        return SkuResponse.from(sku);
    }

    public List<SkuResponse> getAll() {
        return skuRepository.findAll()
                .stream()
                .map(SkuResponse::from)
                .toList();
    }

    @Transactional
    public SkuResponse update(Long id, SkuUpdateRequest request) {
        Sku sku = skuRepository.findById(id)
                .orElseThrow(SkuNotFoundException::new);

        sku.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        return SkuResponse.from(sku);
    }

    @Transactional
    public void delete(Long id) {
        if (!skuRepository.existsById(id)) {
            throw new SkuNotFoundException();
        }
        skuRepository.deleteById(id);
    }
}
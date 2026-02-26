package com.arso.arsoback.domain.product.mapper;

import com.arso.arsoback.domain.product.dto.ProductCreateRequest;
import com.arso.arsoback.domain.product.dto.ProductResponse;
import com.arso.arsoback.domain.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductCreateRequest req);

    ProductResponse toResponse(Product product);
}

package com.app.store.mapper;

import com.app.store.dto.request.ProductRequest;
import com.app.store.dto.response.ProductResponse;
import com.app.store.model.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "readableId", ignore = true)
    @Mapping(target = "unitCount", ignore = true)
    @Mapping(target = "instituteId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "pricing", source = "pricing")
    Product toEntity(ProductRequest request);

    @Mapping(target = "activePrice", source = "pricing.activePrice")
    @Mapping(target = "mrp", source = "pricing.mrp")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "readableId", ignore = true)
    @Mapping(target = "unitCount", ignore = true)
    @Mapping(target = "instituteId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}

package com.app.store.dto.response;

import com.app.store.model.entity.Product;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String productTitle;
    private String description;
    private String imageUrl;
    private Integer totalStock;
    private Double activePrice;
    
    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productTitle(product.getProductTitle())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .totalStock(product.getTotalStock())
                .activePrice(product.getPricing().getActivePrice())
                .build();
    }
}

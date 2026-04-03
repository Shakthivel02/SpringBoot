package com.app.store.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String productTitle;
    private String description;
    private String imageUrl;
    private Integer totalStock;
    private Double activePrice;
    private Double mrp;
    private String readableId;
}

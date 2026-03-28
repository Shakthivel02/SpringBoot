package com.app.store.dto.request;

import com.app.store.model.entity.Pricing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "Title is required")
    private String productTitle;
    
    private String description;
    private String brandName;
    private String imageUrl;
    
    @NotNull(message = "Total stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer totalStock;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotNull(message = "Pricing is required")
    private Pricing pricing;
}

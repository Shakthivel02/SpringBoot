package com.app.store.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_product_readable_id", columnList = "readableId", unique = true)
})
@Data
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String readableId;
    private String productTitle;
    private String description;
    private String brandName;
    private String imageUrl;
    
    private Integer unitCount;
    private Integer totalStock;
    private String status; 
    private String instituteId;
    
    @Embedded
    private Pricing pricing;
}

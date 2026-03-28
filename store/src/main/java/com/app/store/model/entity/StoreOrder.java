package com.app.store.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StoreOrder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String studentId;
    private String instituteId;
    private Long productId;
    
    private String status; // pending_parent, pending_admin, approved, rejected, delivered
    private Integer costCoins;
    private Integer quantity;
    
    private String parentRejectionReason;
}

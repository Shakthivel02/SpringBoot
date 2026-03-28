package com.app.store.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String transactionType; // store_purchase, refund
    private String status; // pending, completed, refunded
    
    private Integer coinsChange;
    private Integer previousCoins;
    private Integer newCoins;
    
    private String studentId;
    private Long storeOrderId;
    
    @PrePersist
    @PreUpdate
    public void calculateNewCoins() {
        if (previousCoins != null && coinsChange != null) {
            this.newCoins = this.previousCoins + this.coinsChange;
        }
    }
}

package com.app.store.model.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDateTime;

@Embeddable
@Data
public class CartItem {
    private Long productId;
    private Integer quantity;
    private LocalDateTime addedAt;
}

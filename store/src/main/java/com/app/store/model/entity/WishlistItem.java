package com.app.store.model.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDateTime;

@Embeddable
@Data
public class WishlistItem {
    private Long productId;
    private LocalDateTime addedAt;
}

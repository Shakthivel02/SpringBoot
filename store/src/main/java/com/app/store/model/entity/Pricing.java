package com.app.store.model.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalDateTime;

@Embeddable
@Data
public class Pricing {
    private Double mrp;
    private Double salePrice;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;

    public Double getActivePrice() {
        Double activePrice = this.mrp;
        LocalDateTime now = LocalDateTime.now();
        if (this.salePrice != null && this.saleStartDate != null && this.saleEndDate != null) {
            if (now.isAfter(this.saleStartDate) && now.isBefore(this.saleEndDate)) {
                activePrice = this.salePrice;
            }
        }
        return activePrice;
    }
}

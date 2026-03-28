package com.app.store.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoreOrderResponse {
    private Long orderId;
    private String status;
    private Long transactionId;
    private Integer coinsDeducted;
}

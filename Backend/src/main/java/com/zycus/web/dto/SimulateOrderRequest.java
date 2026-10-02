package com.zycus.web.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SimulateOrderRequest {
    @Min(value = 1, message = "Order quantity must be at least 1")
    private Integer quantity = 1;
}

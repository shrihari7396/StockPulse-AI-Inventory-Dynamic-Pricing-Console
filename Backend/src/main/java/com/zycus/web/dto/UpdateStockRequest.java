package com.zycus.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @NotNull(message = "stockLevel is required")
    @Min(value = 0, message = "stockLevel cannot be negative")
    private Integer stockLevel;
}

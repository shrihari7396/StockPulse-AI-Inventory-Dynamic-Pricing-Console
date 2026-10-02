package com.zycus.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

import com.zycus.domain.model.Category;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product ID is required (e.g. PRD-009)")
    private String id;

    @NotBlank(message = "SKU is required (e.g. SKU-ELEC-004)")
    private String sku;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Category is required (ELECTRONICS, APPAREL, HOME)")
    private Category category;

    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.01", message = "Current price must be positive")
    private BigDecimal currentPrice;

    @NotNull(message = "Stock level is required")
    @Min(value = 0, message = "Stock level cannot be negative")
    private Integer stockLevel;

    @NotNull(message = "Reorder threshold is required")
    @Min(value = 1, message = "Reorder threshold must be at least 1")
    private Integer reorderThreshold;

    private Integer demandVelocity = 0;

    // Sprint 2 Extension Placeholders
    private BigDecimal costPrice;
    private String supplierId;
    private BigDecimal competitorPrice;
}

package com.zycus.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "sku", length = 64, nullable = false, unique = true)
    private String sku;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32, nullable = false)
    private Category category;

    @Column(name = "current_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold;

    @Column(name = "demand_velocity", nullable = false)
    private Integer demandVelocity; // Orders in last 24 hours

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ProductStatus status;

    // --- Sprint 2 Extensibility Placeholders ---
    @Column(name = "cost_price", precision = 10, scale = 2, nullable = true)
    private BigDecimal costPrice; // Base wholesale cost for margin calculations

    @Column(name = "supplier_id", length = 64, nullable = true)
    private String supplierId; // Reference to external supplier replenishment catalog

    @Column(name = "competitor_price", precision = 10, scale = 2, nullable = true)
    private BigDecimal competitorPrice; // Tracked market baseline

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = (stockLevel != null && stockLevel <= 0) ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return stockLevel != null && reorderThreshold != null && stockLevel < reorderThreshold;
    }

    public boolean isOutOfStock() {
        return stockLevel != null && stockLevel <= 0;
    }
}

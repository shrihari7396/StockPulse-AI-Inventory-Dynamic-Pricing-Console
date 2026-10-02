package com.zycus.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_level", nullable = false)
    private Integer stockLevel;

    @Column(name = "demand_velocity", nullable = false)
    private Integer demandVelocity;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType; // e.g. "ORDER_SIMULATED", "STOCK_ADJUSTED", "REORDER_ACCEPTED"

    @Column(name = "notes", length = 512)
    private String notes;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

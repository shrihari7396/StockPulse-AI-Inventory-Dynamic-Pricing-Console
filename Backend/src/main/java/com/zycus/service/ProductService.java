package com.zycus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zycus.agentic.event.DemandSpikeEvent;
import com.zycus.agentic.event.StockDepletedEvent;
import com.zycus.domain.model.*;
import com.zycus.domain.repository.InventorySnapshotRepository;
import com.zycus.domain.repository.ProductRepository;
import com.zycus.web.dto.CreateProductRequest;
import com.zycus.web.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${stockpulse.triggers.demand-spike-multiplier:2.5}")
    private double demandSpikeMultiplier;

    @Value("${stockpulse.triggers.demand-spike-absolute-threshold:10}")
    private int demandSpikeAbsoluteThreshold;

    @Transactional(readOnly = true)
    public List<Product> getProducts(ProductStatus status, Category category) {
        if (status != null && category != null) {
            return productRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .id(request.getId())
                .sku(request.getSku())
                .name(request.getName())
                .category(request.getCategory())
                .currentPrice(request.getCurrentPrice())
                .stockLevel(request.getStockLevel())
                .reorderThreshold(request.getReorderThreshold())
                .demandVelocity(request.getDemandVelocity() != null ? request.getDemandVelocity() : 0)
                .costPrice(request.getCostPrice())
                .supplierId(request.getSupplierId())
                .competitorPrice(request.getCompetitorPrice())
                .status((request.getStockLevel() <= 0) ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);

        // Check initial stock trigger
        if (saved.getStockLevel() < saved.getReorderThreshold()) {
            eventPublisher.publishEvent(new StockDepletedEvent(saved.getId(), saved.getStockLevel(), saved.getReorderThreshold()));
        }

        return saved;
    }

    @Transactional
    public Product updateStock(String id, int newStockLevel) {
        Product product = getProductById(id);
        int oldStock = product.getStockLevel();
        product.setStockLevel(newStockLevel);

        if (newStockLevel <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product saved = productRepository.save(product);

        // Record audit snapshot
        inventorySnapshotRepository.save(InventorySnapshot.builder()
                .product(saved)
                .stockLevel(newStockLevel)
                .demandVelocity(saved.getDemandVelocity())
                .eventType("STOCK_PATCHED")
                .notes(String.format("Stock manually adjusted from %d to %d", oldStock, newStockLevel))
                .timestamp(LocalDateTime.now())
                .build());

        // Agentic Trigger A: Check low inventory trigger
        if (newStockLevel < saved.getReorderThreshold()) {
            log.info("Stock for SKU {} ({}) is below reorder threshold ({}). Firing agentic Trigger A.",
                    saved.getSku(), newStockLevel, saved.getReorderThreshold());
            eventPublisher.publishEvent(new StockDepletedEvent(saved.getId(), newStockLevel, saved.getReorderThreshold()));
        }

        return saved;
    }

    @Transactional
    public Product simulateOrder(String id, int orderQuantity) {
        Product product = getProductById(id);

        int currentStock = product.getStockLevel();
        if (currentStock < orderQuantity) {
            throw new IllegalArgumentException(String.format("Insufficient stock for SKU %s. Requested: %d, Available: %d",
                    product.getSku(), orderQuantity, currentStock));
        }

        // 1. Decrement stock
        int updatedStock = currentStock - orderQuantity;
        product.setStockLevel(updatedStock);

        // 2. Increment demand velocity
        int updatedVelocity = product.getDemandVelocity() + orderQuantity;
        product.setDemandVelocity(updatedVelocity);

        if (updatedStock <= 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);

        // 3. Record audit snapshot
        inventorySnapshotRepository.save(InventorySnapshot.builder()
                .product(saved)
                .stockLevel(updatedStock)
                .demandVelocity(updatedVelocity)
                .eventType("ORDER_SIMULATED")
                .notes(String.format("Order for %d unit(s) placed. Stock: %d -> %d, Velocity: %d -> %d",
                        orderQuantity, currentStock, updatedStock, product.getDemandVelocity() - orderQuantity, updatedVelocity))
                .timestamp(LocalDateTime.now())
                .build());

        // 4. Trigger A: Check low inventory
        if (updatedStock < saved.getReorderThreshold()) {
            log.info("Simulated order caused low stock for SKU {} (stock: {}, threshold: {}). Publishing StockDepletedEvent.",
                    saved.getSku(), updatedStock, saved.getReorderThreshold());
            eventPublisher.publishEvent(new StockDepletedEvent(saved.getId(), updatedStock, saved.getReorderThreshold()));
        }

        // 5. Trigger B: Check viral demand spike
        Double categoryAvg = productRepository.findAverageDemandVelocityByCategory(saved.getCategory());
        double catAvgVal = categoryAvg != null ? categoryAvg : 1.0;
        boolean spikeByMultiplier = (catAvgVal > 0) && (updatedVelocity >= (demandSpikeMultiplier * catAvgVal));
        boolean spikeByAbsolute = updatedVelocity >= demandSpikeAbsoluteThreshold;

        if (spikeByMultiplier || spikeByAbsolute) {
            log.info("Simulated order triggered DEMAND_SPIKE for SKU {} (Velocity: {}, CatAvg: {}, MultSpike: {}, AbsSpike: {}). Publishing DemandSpikeEvent.",
                    saved.getSku(), updatedVelocity, catAvgVal, spikeByMultiplier, spikeByAbsolute);
            eventPublisher.publishEvent(new DemandSpikeEvent(saved.getId(), updatedVelocity, catAvgVal));
        }

        return saved;
    }
}

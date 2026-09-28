package org.zycus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zycus.domain.model.*;
import org.zycus.domain.repository.InventorySnapshotRepository;
import org.zycus.domain.repository.ProductRepository;
import org.zycus.domain.repository.ReorderSuggestionRepository;
import org.zycus.web.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReorderSuggestionService {

    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final ProductRepository productRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;

    @Transactional(readOnly = true)
    public List<ReorderSuggestion> search(String productId, SuggestionStatus status, TriggerReason triggerReason) {
        return reorderSuggestionRepository.search(productId, status, triggerReason);
    }

    @Transactional(readOnly = true)
    public ReorderSuggestion getById(Long id) {
        return reorderSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found with id: " + id));
    }

    @Transactional
    public ReorderSuggestion updateStatus(Long id, SuggestionStatus newStatus) {
        ReorderSuggestion suggestion = getById(id);

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            log.warn("Reorder suggestion {} is already finalized as {}", id, suggestion.getStatus());
            return suggestion;
        }

        suggestion.setStatus(newStatus);
        Product product = suggestion.getProduct();

        if (newStatus == SuggestionStatus.ACCEPTED) {
            int oldStock = product.getStockLevel();
            int addedQty = suggestion.getRecommendedQuantity();
            int newStock = oldStock + addedQty;
            product.setStockLevel(newStock);

            log.info("Reorder suggestion {} ACCEPTED. Simulated inbound shipment received for SKU {}: {} -> {} units",
                    id, product.getSku(), oldStock, newStock);

            // If product was OUT_OF_STOCK, it can now return to ACTIVE (or remain in PRICE_REVIEW_PENDING if price is undergoing review)
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
                product.setStatus(ProductStatus.ACTIVE);
            }

            productRepository.save(product);

            // Record snapshot
            inventorySnapshotRepository.save(InventorySnapshot.builder()
                    .product(product)
                    .stockLevel(newStock)
                    .demandVelocity(product.getDemandVelocity())
                    .eventType("REORDER_ACCEPTED_INBOUND")
                    .notes(String.format("Accepted Reorder Suggestion #%d: +%d units received.", id, addedQty))
                    .timestamp(LocalDateTime.now())
                    .build());
        } else {
            log.info("Reorder suggestion {} REJECTED for SKU {}", id, product.getSku());
        }

        return reorderSuggestionRepository.save(suggestion);
    }
}

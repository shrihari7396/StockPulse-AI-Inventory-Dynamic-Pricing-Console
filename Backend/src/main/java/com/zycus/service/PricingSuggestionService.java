package com.zycus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zycus.domain.model.PricingSuggestion;
import com.zycus.domain.model.Product;
import com.zycus.domain.model.ProductStatus;
import com.zycus.domain.model.SuggestionStatus;
import com.zycus.domain.model.TriggerReason;
import com.zycus.domain.repository.PricingSuggestionRepository;
import com.zycus.domain.repository.ProductRepository;
import com.zycus.web.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingSuggestionService {

    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<PricingSuggestion> search(String productId, SuggestionStatus status, TriggerReason triggerReason) {
        return pricingSuggestionRepository.search(productId, status, triggerReason);
    }

    @Transactional(readOnly = true)
    public PricingSuggestion getById(Long id) {
        return pricingSuggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found with id: " + id));
    }

    @Transactional
    public PricingSuggestion updateStatus(Long id, SuggestionStatus newStatus) {
        PricingSuggestion suggestion = getById(id);

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            log.warn("Pricing suggestion {} is already finalized as {}", id, suggestion.getStatus());
            return suggestion;
        }

        suggestion.setStatus(newStatus);
        Product product = suggestion.getProduct();

        if (newStatus == SuggestionStatus.ACCEPTED) {
            BigDecimal oldPrice = product.getCurrentPrice();
            BigDecimal newPrice = suggestion.getRecommendedPrice();
            product.setCurrentPrice(newPrice);
            log.info("Pricing suggestion {} ACCEPTED. Product {} price updated: ${} -> ${}",
                    id, product.getSku(), oldPrice, newPrice);

            // Revert status to ACTIVE if it was pending price review and has stock
            if (product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING && product.getStockLevel() > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            productRepository.save(product);
        } else if (newStatus == SuggestionStatus.REJECTED) {
            log.info("Pricing suggestion {} REJECTED for SKU {}", id, product.getSku());
            // If rejected and no other pending pricing review, check if it can return to ACTIVE
            if (product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING && product.getStockLevel() > 0) {
                product.setStatus(ProductStatus.ACTIVE);
                productRepository.save(product);
            }
        }

        return pricingSuggestionRepository.save(suggestion);
    }
}

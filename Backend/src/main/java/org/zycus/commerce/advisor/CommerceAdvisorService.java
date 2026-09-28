package org.zycus.commerce.advisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zycus.commerce.context.CommerceContext;
import org.zycus.commerce.strategy.*;
import org.zycus.domain.model.*;
import org.zycus.domain.repository.PricingSuggestionRepository;
import org.zycus.domain.repository.ProductRepository;
import org.zycus.domain.repository.ReorderSuggestionRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceAdvisorService {

    private final StrategyRegistry strategyRegistry;
    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    public CommerceContext buildContext(Product product, TriggerReason triggerReason, String note) {
        Double avgVelocity = productRepository.findAverageDemandVelocityByCategory(product.getCategory());
        return CommerceContext.builder()
                .categoryAverageVelocity(avgVelocity != null ? avgVelocity : 0.0)
                .triggerReason(triggerReason)
                .peerCount(productRepository.findByCategory(product.getCategory()).size())
                .note(note)
                .build();
    }

    @Transactional
    public PricingSuggestion generatePricingSuggestion(Product product, TriggerReason triggerReason, String note) {
        CommerceContext context = buildContext(product, triggerReason, note);
        PricingStrategy strategy = strategyRegistry.getActivePricingStrategy();
        log.info("Generating pricing suggestion for SKU {} using strategy '{}'", product.getSku(), strategy.getStrategyName());

        PricingSuggestionResult result = strategy.evaluate(product, context);

        PricingSuggestion suggestion = PricingSuggestion.builder()
                .product(product)
                .currentPrice(product.getCurrentPrice())
                .recommendedPrice(result.getRecommendedPrice())
                .changeDirection(result.getChangeDirection())
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .strategyUsed(result.getStrategyUsed())
                .createdAt(LocalDateTime.now())
                .build();

        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);

        // Transition product status to PRICE_REVIEW_PENDING if active
        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
            productRepository.save(product);
        }

        return saved;
    }

    @Transactional
    public ReorderSuggestion generateReorderSuggestion(Product product, TriggerReason triggerReason, String note) {
        CommerceContext context = buildContext(product, triggerReason, note);
        ReorderStrategy strategy = strategyRegistry.getActiveReorderStrategy();
        log.info("Generating reorder suggestion for SKU {} using strategy '{}'", product.getSku(), strategy.getStrategyName());

        ReorderSuggestionResult result = strategy.evaluate(product, context);

        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .product(product)
                .currentStock(product.getStockLevel())
                .recommendedQuantity(result.getRecommendedQuantity())
                .suggestedLeadTimeDays(result.getSuggestedLeadTimeDays())
                .confidence(result.getConfidence())
                .reasoning(result.getReasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .strategyUsed(result.getStrategyUsed())
                .createdAt(LocalDateTime.now())
                .build();

        return reorderSuggestionRepository.save(suggestion);
    }
}

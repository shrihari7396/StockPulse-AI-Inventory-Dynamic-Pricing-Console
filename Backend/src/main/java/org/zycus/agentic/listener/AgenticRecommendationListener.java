package org.zycus.agentic.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zycus.agentic.event.DemandSpikeEvent;
import org.zycus.agentic.event.StockDepletedEvent;
import org.zycus.commerce.advisor.CommerceAdvisorService;
import org.zycus.commerce.context.CommerceContext;
import org.zycus.commerce.strategy.RuleBasedPricingStrategy;
import org.zycus.commerce.strategy.RuleBasedReorderStrategy;
import org.zycus.domain.model.*;
import org.zycus.domain.repository.PricingSuggestionRepository;
import org.zycus.domain.repository.ProductRepository;
import org.zycus.domain.repository.ReorderSuggestionRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgenticRecommendationListener {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final CommerceAdvisorService commerceAdvisorService;
    private final RuleBasedPricingStrategy ruleBasedPricingStrategy;
    private final RuleBasedReorderStrategy ruleBasedReorderStrategy;

    @Async
    @EventListener
    @Transactional
    public void onStockDepleted(StockDepletedEvent event) {
        log.info("Agentic Loop [Trigger A - INVENTORY_LOW]: Received stock alert for SKU '{}' (Stock: {}, Threshold: {})",
                event.getProductId(), event.getCurrentStock(), event.getThreshold());

        Product product = productRepository.findById(event.getProductId()).orElse(null);
        if (product == null) {
            log.warn("Product with ID '{}' not found for stock alert. Aborting loop.", event.getProductId());
            return;
        }

        // Deduplication Guard: Skip if pending suggestions for this SKU and trigger already exist
        boolean pricingPending = pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING
        );
        boolean reorderPending = reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), TriggerReason.INVENTORY_LOW, SuggestionStatus.PENDING
        );

        if (pricingPending && reorderPending) {
            log.info("Agentic Loop: Suggestions already queued for SKU '{}' under INVENTORY_LOW. Skipping duplicate run.", product.getSku());
            return;
        }

        try {
            if (!pricingPending) {
                commerceAdvisorService.generatePricingSuggestion(product, TriggerReason.INVENTORY_LOW,
                        "Agentic Trigger: Automated inventory depletion below safety threshold.");
            }
            if (!reorderPending) {
                commerceAdvisorService.generateReorderSuggestion(product, TriggerReason.INVENTORY_LOW,
                        "Agentic Trigger: Automated stock replenishment request.");
            }
            log.info("Agentic Loop: Successfully generated and queued suggestions for SKU '{}'", product.getSku());
        } catch (Exception ex) {
            log.error("Agentic Loop: Primary suggestion path failed for SKU '{}'. Activating fail-safe rule baseline. Error: {}",
                    product.getSku(), ex.getMessage());
            executeFailsafe(product, TriggerReason.INVENTORY_LOW, pricingPending, reorderPending);
        }
    }

    @Async
    @EventListener
    @Transactional
    public void onDemandSpike(DemandSpikeEvent event) {
        log.info("Agentic Loop [Trigger B - DEMAND_SPIKE]: Received viral surge alert for SKU '{}' (Velocity: {}/24h, Category Avg: {})",
                event.getProductId(), event.getCurrentVelocity(), event.getCategoryAverage());

        Product product = productRepository.findById(event.getProductId()).orElse(null);
        if (product == null) {
            log.warn("Product with ID '{}' not found for demand spike. Aborting loop.", event.getProductId());
            return;
        }

        // Deduplication Guard
        boolean pricingPending = pricingSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), TriggerReason.DEMAND_SPIKE, SuggestionStatus.PENDING
        );
        boolean reorderPending = reorderSuggestionRepository.existsByProductIdAndTriggerReasonAndStatus(
                product.getId(), TriggerReason.DEMAND_SPIKE, SuggestionStatus.PENDING
        );

        if (pricingPending && reorderPending) {
            log.info("Agentic Loop: Suggestions already queued for SKU '{}' under DEMAND_SPIKE. Skipping duplicate run.", product.getSku());
            return;
        }

        try {
            if (!pricingPending) {
                commerceAdvisorService.generatePricingSuggestion(product, TriggerReason.DEMAND_SPIKE,
                        "Agentic Trigger: Automated demand velocity surge alert.");
            }
            if (!reorderPending) {
                commerceAdvisorService.generateReorderSuggestion(product, TriggerReason.DEMAND_SPIKE,
                        "Agentic Trigger: Surge buffer replenishment request.");
            }
            log.info("Agentic Loop: Successfully generated and queued surge suggestions for SKU '{}'", product.getSku());
        } catch (Exception ex) {
            log.error("Agentic Loop: Primary surge path failed for SKU '{}'. Activating fail-safe rule baseline. Error: {}",
                    product.getSku(), ex.getMessage());
            executeFailsafe(product, TriggerReason.DEMAND_SPIKE, pricingPending, reorderPending);
        }
    }

    private void executeFailsafe(Product product, TriggerReason trigger, boolean skipPricing, boolean skipReorder) {
        CommerceContext context = commerceAdvisorService.buildContext(product, trigger, "Failsafe execution");
        if (!skipPricing) {
            var res = ruleBasedPricingStrategy.evaluate(product, context);
            PricingSuggestion ps = PricingSuggestion.builder()
                    .product(product)
                    .currentPrice(product.getCurrentPrice())
                    .recommendedPrice(res.getRecommendedPrice())
                    .changeDirection(res.getChangeDirection())
                    .confidence(res.getConfidence())
                    .reasoning("[Agentic Failsafe Baseline]: " + res.getReasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(trigger)
                    .strategyUsed("FAILSAFE_RULE_BASED")
                    .createdAt(LocalDateTime.now())
                    .build();
            pricingSuggestionRepository.save(ps);
        }
        if (!skipReorder) {
            var res = ruleBasedReorderStrategy.evaluate(product, context);
            ReorderSuggestion rs = ReorderSuggestion.builder()
                    .product(product)
                    .currentStock(product.getStockLevel())
                    .recommendedQuantity(res.getRecommendedQuantity())
                    .suggestedLeadTimeDays(res.getSuggestedLeadTimeDays())
                    .confidence(res.getConfidence())
                    .reasoning("[Agentic Failsafe Baseline]: " + res.getReasoning())
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(trigger)
                    .strategyUsed("FAILSAFE_RULE_BASED")
                    .createdAt(LocalDateTime.now())
                    .build();
            reorderSuggestionRepository.save(rs);
        }
    }
}

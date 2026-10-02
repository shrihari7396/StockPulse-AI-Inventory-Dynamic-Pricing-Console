package com.zycus.commerce.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.ChangeDirection;
import com.zycus.domain.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sprint 2 Seam: Competitor-Aware Pricing Strategy
 *
 * Demonstrates pluggability: implements PricingStrategy without touching any existing core logic.
 * Dynamically factors in competitorPrice and costPrice margin floors.
 */
@Component("competitorAwareStrategy")
@RequiredArgsConstructor
@Slf4j
public class CompetitorAwareStrategy implements PricingStrategy {

    public static final String STRATEGY_NAME = "COMPETITOR_AWARE";

    private final RuleBasedPricingStrategy fallbackStrategy;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public PricingSuggestionResult evaluate(Product product, CommerceContext context) {
        BigDecimal competitorPrice = product.getCompetitorPrice();
        BigDecimal costPrice = product.getCostPrice();
        BigDecimal currentPrice = product.getCurrentPrice();

        // If no competitor price tracked, fall back to standard rule engine
        if (competitorPrice == null) {
            log.info("SKU {} has no competitorPrice tracked. Falling back to rule-based strategy.", product.getSku());
            return fallbackStrategy.evaluate(product, context);
        }

        // Safety margin floor: Minimum 15% gross margin above wholesale cost
        BigDecimal marginFloor = (costPrice != null)
                ? costPrice.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP)
                : currentPrice.multiply(BigDecimal.valueOf(0.70)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal recommendedPrice;
        String reasoning;

        if (competitorPrice.compareTo(currentPrice) < 0) {
            // Competitor is cheaper: attempt to match or undercut by 1%, respecting margin floor
            BigDecimal undercut = competitorPrice.multiply(BigDecimal.valueOf(0.99)).setScale(2, RoundingMode.HALF_UP);
            if (undercut.compareTo(marginFloor) >= 0) {
                recommendedPrice = undercut;
                reasoning = String.format("Competitor-Aware Engine: Competitor offers $%s (below our $%s). " +
                                "Recommended tactical undercut to $%s while preserving 15%% margin floor ($%s).",
                        competitorPrice, currentPrice, recommendedPrice, marginFloor);
            } else {
                recommendedPrice = marginFloor;
                reasoning = String.format("Competitor-Aware Engine: Competitor price $%s is below margin floor. " +
                                "Clamped recommended price to safe margin floor of $%s (Cost: $%s).",
                        competitorPrice, marginFloor, costPrice);
            }
        } else {
            // Competitor is more expensive: capture extra margin by pricing 2% below competitor
            recommendedPrice = competitorPrice.multiply(BigDecimal.valueOf(0.98)).setScale(2, RoundingMode.HALF_UP);
            reasoning = String.format("Competitor-Aware Engine: Market leader priced at $%s. " +
                            "Recommended positioning at $%s (2%% below market) to maximize unit volume and margin.",
                    competitorPrice, recommendedPrice);
        }

        ChangeDirection direction = recommendedPrice.compareTo(currentPrice) > 0 ? ChangeDirection.INCREASE :
                (recommendedPrice.compareTo(currentPrice) < 0 ? ChangeDirection.DECREASE : ChangeDirection.HOLD);

        return PricingSuggestionResult.builder()
                .recommendedPrice(recommendedPrice)
                .changeDirection(direction)
                .confidence(0.93)
                .reasoning(reasoning)
                .strategyUsed(STRATEGY_NAME)
                .isFallback(false)
                .build();
    }
}

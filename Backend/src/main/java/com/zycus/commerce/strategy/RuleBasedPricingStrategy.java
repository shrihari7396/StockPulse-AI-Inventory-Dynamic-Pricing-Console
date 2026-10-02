package com.zycus.commerce.strategy;

import org.springframework.stereotype.Component;

import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.ChangeDirection;
import com.zycus.domain.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedPricingStrategy")
public class RuleBasedPricingStrategy implements PricingStrategy {

    public static final String STRATEGY_NAME = "RULE_BASED";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public PricingSuggestionResult evaluate(Product product, CommerceContext context) {
        BigDecimal currentPrice = product.getCurrentPrice();
        int stock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;
        int velocity = product.getDemandVelocity() != null ? product.getDemandVelocity() : 0;
        double categoryAvg = (context != null && context.getCategoryAverageVelocity() != null)
                ? context.getCategoryAverageVelocity()
                : 0.0;

        BigDecimal recommendedPrice;
        ChangeDirection direction;
        double confidence;
        String reasoning;

        if (stock < threshold) {
            // Low stock rule: +10% price increase to protect remaining inventory
            recommendedPrice = currentPrice.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP);
            direction = ChangeDirection.INCREASE;
            confidence = 0.88;
            reasoning = String.format(
                    "Rule-Based Signal: Stock level (%d units) is critically below reorder threshold (%d units). " +
                    "Recommended 10.0%% price increase from $%s to $%s to moderate sell-through and protect remaining inventory.",
                    stock, threshold, currentPrice, recommendedPrice
            );
        } else if (categoryAvg > 0 && velocity > (2.0 * categoryAvg)) {
            // Demand surge rule: +5% price increase to capture consumer surplus
            recommendedPrice = currentPrice.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
            direction = ChangeDirection.INCREASE;
            confidence = 0.82;
            reasoning = String.format(
                    "Rule-Based Signal: Demand velocity (%d orders/24h) is >2x category average (%.1f). " +
                    "Recommended 5.0%% price increase from $%s to $%s to capture momentum margin.",
                    velocity, categoryAvg, currentPrice, recommendedPrice
            );
        } else {
            // Balanced equilibrium rule: HOLD
            recommendedPrice = currentPrice;
            direction = ChangeDirection.HOLD;
            confidence = 0.90;
            reasoning = String.format(
                    "Rule-Based Signal: Stock (%d units) and velocity (%d/24h) are stable relative to category baseline (%.1f). " +
                    "Recommended holding current price at $%s.",
                    stock, velocity, categoryAvg, currentPrice
            );
        }

        return PricingSuggestionResult.builder()
                .recommendedPrice(recommendedPrice)
                .changeDirection(direction)
                .confidence(confidence)
                .reasoning(reasoning)
                .strategyUsed(STRATEGY_NAME)
                .isFallback(false)
                .build();
    }
}

package com.zycus.commerce.strategy;

import org.springframework.stereotype.Component;

import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.Product;

@Component("ruleBasedReorderStrategy")
public class RuleBasedReorderStrategy implements ReorderStrategy {

    public static final String STRATEGY_NAME = "RULE_BASED";

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public ReorderSuggestionResult evaluate(Product product, CommerceContext context) {
        int stock = product.getStockLevel() != null ? product.getStockLevel() : 0;
        int threshold = product.getReorderThreshold() != null ? product.getReorderThreshold() : 0;

        // Baseline formula: (reorder threshold * 3) - current stock, minimum 1
        int rawQty = (threshold * 3) - stock;
        int recommendedQty = Math.max(1, rawQty);

        // Suggested lead time by category
        int leadTimeDays = switch (product.getCategory()) {
            case ELECTRONICS -> 7;
            case APPAREL -> 5;
            case HOME -> 4;
        };

        double confidence = 0.85;
        String reasoning = String.format(
                "Rule-Based Signal: Reorder baseline calculation [(threshold %d * 3) - stock %d] recommends replenishing %d units " +
                "to restore 3x buffer capacity. Standard %s lead time is %d business days.",
                threshold, stock, recommendedQty, product.getCategory(), leadTimeDays
        );

        return ReorderSuggestionResult.builder()
                .recommendedQuantity(recommendedQty)
                .suggestedLeadTimeDays(leadTimeDays)
                .confidence(confidence)
                .reasoning(reasoning)
                .strategyUsed(STRATEGY_NAME)
                .isFallback(false)
                .build();
    }
}

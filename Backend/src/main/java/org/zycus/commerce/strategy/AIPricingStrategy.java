package org.zycus.commerce.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zycus.commerce.ai.AIAdvisorOrchestrator;
import org.zycus.commerce.context.CommerceContext;
import org.zycus.domain.model.Product;

@Component("aiPricingStrategy")
@RequiredArgsConstructor
public class AIPricingStrategy implements PricingStrategy {

    public static final String STRATEGY_NAME = "AI";

    private final AIAdvisorOrchestrator orchestrator;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public PricingSuggestionResult evaluate(Product product, CommerceContext context) {
        return orchestrator.consult(product, context).pricing;
    }
}

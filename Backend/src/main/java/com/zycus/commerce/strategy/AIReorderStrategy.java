package com.zycus.commerce.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.zycus.commerce.ai.AIAdvisorOrchestrator;
import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.Product;

@Component("aiReorderStrategy")
@RequiredArgsConstructor
public class AIReorderStrategy implements ReorderStrategy {

    public static final String STRATEGY_NAME = "AI";

    private final AIAdvisorOrchestrator orchestrator;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public ReorderSuggestionResult evaluate(Product product, CommerceContext context) {
        return orchestrator.consult(product, context).reorder;
    }
}

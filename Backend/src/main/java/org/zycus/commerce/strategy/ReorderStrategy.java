package org.zycus.commerce.strategy;

import org.zycus.commerce.context.CommerceContext;
import org.zycus.domain.model.Product;

public interface ReorderStrategy {
    ReorderSuggestionResult evaluate(Product product, CommerceContext context);
    String getStrategyName();
}

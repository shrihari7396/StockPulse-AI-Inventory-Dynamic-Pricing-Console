package org.zycus.commerce.strategy;

import org.zycus.commerce.context.CommerceContext;
import org.zycus.domain.model.Product;

public interface PricingStrategy {
    PricingSuggestionResult evaluate(Product product, CommerceContext context);
    String getStrategyName();
}

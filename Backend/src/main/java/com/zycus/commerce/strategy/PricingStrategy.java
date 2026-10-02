package com.zycus.commerce.strategy;

import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.Product;

public interface PricingStrategy {
    PricingSuggestionResult evaluate(Product product, CommerceContext context);
    String getStrategyName();
}

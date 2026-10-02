package com.zycus.commerce.strategy;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

import com.zycus.domain.model.ChangeDirection;

@Getter
@Builder
public class PricingSuggestionResult {
    private final BigDecimal recommendedPrice;
    private final ChangeDirection changeDirection;
    private final Double confidence;
    private final String reasoning;
    private final String strategyUsed;
    private final boolean isFallback;
}

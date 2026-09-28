package org.zycus.commerce.strategy;

import lombok.Builder;
import lombok.Getter;
import org.zycus.domain.model.ChangeDirection;

import java.math.BigDecimal;

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

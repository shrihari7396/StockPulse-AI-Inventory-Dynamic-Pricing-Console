package org.zycus.commerce.strategy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReorderSuggestionResult {
    private final Integer recommendedQuantity;
    private final Integer suggestedLeadTimeDays;
    private final Double confidence;
    private final String reasoning;
    private final String strategyUsed;
    private final boolean isFallback;
}

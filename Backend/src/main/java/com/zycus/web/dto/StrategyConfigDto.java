package com.zycus.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyConfigDto {
    private String activePricingStrategy;
    private String activeReorderStrategy;
    private Set<String> availablePricingStrategies;
    private Set<String> availableReorderStrategies;
}

package org.zycus.web.dto;

import lombok.Data;

@Data
public class UpdateStrategyConfigRequest {
    private String pricingStrategy;
    private String reorderStrategy;
}

package com.zycus.commerce.ai;

import org.springframework.stereotype.Component;

import com.zycus.commerce.context.CommerceContext;
import com.zycus.domain.model.Product;

@Component
public class PromptBuilder {

    /**
     * Dedicated Prompt 1: Low Stock Trigger
     * Focuses on scarcity, sell-through preservation vs clearance risk, and buffer replenishment.
     */
    public String buildInventoryLowPrompt(Product product, CommerceContext context) {
        double categoryAvg = context != null && context.getCategoryAverageVelocity() != null
                ? context.getCategoryAverageVelocity() : 2.5;

        return """
            You are the StockPulse AI Commerce Advisor for ShopStream.
            A CRITICAL INVENTORY DEPLETION event has triggered for the following SKU:
            
            [PRODUCT CONTEXT]
            - SKU: %s
            - Name: %s
            - Category: %s
            - Current Retail Price: $%s
            - Current Stock Level: %d units
            - Reorder Threshold: %d units
            - 24h Demand Velocity: %d orders
            - Category Peer Average Velocity: %.1f orders/24h
            - Trigger Situation: INVENTORY_LOW (Current stock is below safety buffer)
            
            [MERCHANDISING TRADEOFF ANALYSIS REQUIRED]
            Evaluate the strategic merchandising dilemma:
            1. Stock Preservation vs Clearance:
               If velocity is high (%d/24h), the product risks immediate stockout. Raising price (+8%% to +20%%) protects remaining margin and dampens sell-through until replenishment arrives.
               If velocity were near-zero, a clearance discount would be favored to liberate working capital.
            2. Replenishment Calculation:
               Estimate days of inventory cover (Stock / Velocity). Recommend a reorder lot size to establish a 3x buffer above threshold, factoring in category supplier lead time.
            
            [RESPONSE FORMAT]
            Respond in STRICT JSON ONLY without markdown fences or additional commentary:
            {
              "pricing": {
                "recommendedPrice": 27.99,
                "changeDirection": "INCREASE",
                "confidence": 0.92,
                "reasoning": "Detailed commercial explanation for merchandising team"
              },
              "reorder": {
                "recommendedQuantity": 35,
                "suggestedLeadTimeDays": 5,
                "confidence": 0.90,
                "reasoning": "Replenishment urgency and safety stock rationale"
              }
            }
            """.formatted(
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getCurrentPrice(),
                product.getStockLevel(),
                product.getReorderThreshold(),
                product.getDemandVelocity(),
                categoryAvg,
                product.getDemandVelocity()
        );
    }

    /**
     * Dedicated Prompt 2: Demand Velocity Spike Trigger
     * Focuses on viral momentum, consumer willingness to pay, and surge replenishment.
     */
    public String buildDemandSpikePrompt(Product product, CommerceContext context) {
        double categoryAvg = context != null && context.getCategoryAverageVelocity() != null
                ? context.getCategoryAverageVelocity() : 2.5;

        double spikeRatio = categoryAvg > 0 ? (double) product.getDemandVelocity() / categoryAvg : 3.0;

        return """
            You are the StockPulse AI Commerce Advisor for ShopStream.
            A VIRAL DEMAND VELOCITY SURGE has triggered for the following trending SKU:
            
            [PRODUCT CONTEXT]
            - SKU: %s
            - Name: %s
            - Category: %s
            - Current Retail Price: $%s
            - Current Stock Level: %d units
            - Reorder Threshold: %d units
            - 24h Demand Velocity: %d orders
            - Category Peer Average Velocity: %.1f orders/24h
            - Surge Ratio: %.1fx above category benchmark
            - Trigger Situation: DEMAND_SPIKE (Viral social or organic conversion surge)
            
            [MERCHANDISING TRADEOFF ANALYSIS REQUIRED]
            Evaluate the viral surge dynamics:
            1. Capitalizing on Consumer Surplus:
               Demand velocity has surged to %d orders/24h (%.1fx peer average). High conversion momentum indicates low price elasticity. Recommend a tactical price optimization (+5%% to +15%%) to capture incremental margin without extinguishing buyer excitement.
            2. Proactive Surge Replenishment:
               Current inventory will be depleted rapidly if viral velocity sustains. Recommend an aggressive replenishment quantity to avoid losing trending ranking.
            
            [RESPONSE FORMAT]
            Respond in STRICT JSON ONLY without markdown fences or additional commentary:
            {
              "pricing": {
                "recommendedPrice": 59.99,
                "changeDirection": "INCREASE",
                "confidence": 0.89,
                "reasoning": "Detailed commercial explanation explaining surge monetization"
              },
              "reorder": {
                "recommendedQuantity": 50,
                "suggestedLeadTimeDays": 6,
                "confidence": 0.87,
                "reasoning": "Surge buffer replenishment rationale"
              }
            }
            """.formatted(
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getCurrentPrice(),
                product.getStockLevel(),
                product.getReorderThreshold(),
                product.getDemandVelocity(),
                categoryAvg,
                spikeRatio,
                product.getDemandVelocity(),
                spikeRatio
        );
    }

    /**
     * General / Manual Prompt for On-Demand Requests
     */
    public String buildManualPrompt(Product product, CommerceContext context) {
        double categoryAvg = context != null && context.getCategoryAverageVelocity() != null
                ? context.getCategoryAverageVelocity() : 2.5;

        return """
            You are the StockPulse AI Commerce Advisor for ShopStream.
            A merchandiser has requested an on-demand pricing and replenishment review:
            
            [PRODUCT CONTEXT]
            - SKU: %s
            - Name: %s
            - Category: %s
            - Current Retail Price: $%s
            - Current Stock Level: %d units
            - Reorder Threshold: %d units
            - 24h Demand Velocity: %d orders
            - Category Peer Average Velocity: %.1f orders/24h
            - Trigger Situation: MANUAL_REVIEW
            
            Evaluate inventory health, margin opportunity, and return optimal pricing & replenishment recommendations.
            
            [RESPONSE FORMAT]
            Respond in STRICT JSON ONLY:
            {
              "pricing": {
                "recommendedPrice": %s,
                "changeDirection": "HOLD",
                "confidence": 0.85,
                "reasoning": "Comprehensive commercial reasoning"
              },
              "reorder": {
                "recommendedQuantity": 25,
                "suggestedLeadTimeDays": 5,
                "confidence": 0.85,
                "reasoning": "Replenishment advice"
              }
            }
            """.formatted(
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getCurrentPrice(),
                product.getStockLevel(),
                product.getReorderThreshold(),
                product.getDemandVelocity(),
                categoryAvg,
                product.getCurrentPrice()
        );
    }
}

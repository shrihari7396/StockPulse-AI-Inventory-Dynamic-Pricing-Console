package org.zycus.commerce.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zycus.commerce.context.CommerceContext;
import org.zycus.commerce.strategy.*;
import org.zycus.domain.model.ChangeDirection;
import org.zycus.domain.model.Product;
import org.zycus.domain.model.TriggerReason;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAdvisorOrchestrator {

    private final LLMGateway llmGateway;
    private final PromptBuilder promptBuilder;
    private final BoundsValidator boundsValidator;
    private final RuleBasedPricingStrategy ruleBasedPricingStrategy;
    private final RuleBasedReorderStrategy ruleBasedReorderStrategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class AIAdviceBundle {
        public final PricingSuggestionResult pricing;
        public final ReorderSuggestionResult reorder;

        public AIAdviceBundle(PricingSuggestionResult pricing, ReorderSuggestionResult reorder) {
            this.pricing = pricing;
            this.reorder = reorder;
        }
    }

    public AIAdviceBundle consult(Product product, CommerceContext context) {
        TriggerReason trigger = context != null && context.getTriggerReason() != null
                ? context.getTriggerReason()
                : TriggerReason.MANUAL;

        // Select the appropriate prompt based on the trigger
        String prompt = switch (trigger) {
            case INVENTORY_LOW -> promptBuilder.buildInventoryLowPrompt(product, context);
            case DEMAND_SPIKE -> promptBuilder.buildDemandSpikePrompt(product, context);
            default -> promptBuilder.buildManualPrompt(product, context);
        };

        try {
            log.info("Invoking AI Advisor for SKU '{}' with trigger '{}'", product.getSku(), trigger);
            String rawJson = llmGateway.callLLM(prompt);
            return parseAndValidate(product, rawJson);
        } catch (Exception ex) {
            log.error("AI Advisor consultation failed for SKU {}. Falling back to Rule-Based strategies. Error: {}",
                    product.getSku(), ex.getMessage());

            // Resilient fallback to deterministic rule-based engines
            PricingSuggestionResult rulePricing = ruleBasedPricingStrategy.evaluate(product, context);
            ReorderSuggestionResult ruleReorder = ruleBasedReorderStrategy.evaluate(product, context);

            PricingSuggestionResult fallbackPricing = PricingSuggestionResult.builder()
                    .recommendedPrice(rulePricing.getRecommendedPrice())
                    .changeDirection(rulePricing.getChangeDirection())
                    .confidence(rulePricing.getConfidence())
                    .reasoning("[Fallback to Rule-Based Engine]: " + rulePricing.getReasoning())
                    .strategyUsed("AI_FALLBACK_RULE")
                    .isFallback(true)
                    .build();

            ReorderSuggestionResult fallbackReorder = ReorderSuggestionResult.builder()
                    .recommendedQuantity(ruleReorder.getRecommendedQuantity())
                    .suggestedLeadTimeDays(ruleReorder.getSuggestedLeadTimeDays())
                    .confidence(ruleReorder.getConfidence())
                    .reasoning("[Fallback to Rule-Based Engine]: " + ruleReorder.getReasoning())
                    .strategyUsed("AI_FALLBACK_RULE")
                    .isFallback(true)
                    .build();

            return new AIAdviceBundle(fallbackPricing, fallbackReorder);
        }
    }

    private AIAdviceBundle parseAndValidate(Product product, String rawJson) throws Exception {
        // Strip markdown fences if present
        String cleanJson = rawJson.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        JsonNode root = objectMapper.readTree(cleanJson);

        // Pricing extraction
        JsonNode pricingNode = root.path("pricing");
        BigDecimal rawPrice = pricingNode.has("recommendedPrice")
                ? new BigDecimal(pricingNode.get("recommendedPrice").asText())
                : product.getCurrentPrice();

        ChangeDirection rawDirection = ChangeDirection.HOLD;
        if (pricingNode.has("changeDirection")) {
            try {
                rawDirection = ChangeDirection.valueOf(pricingNode.get("changeDirection").asText().toUpperCase());
            } catch (Exception ignored) {}
        }

        Double rawPricingConfidence = pricingNode.has("confidence") ? pricingNode.get("confidence").asDouble() : 0.85;
        String pricingReasoning = pricingNode.has("reasoning") ? pricingNode.get("reasoning").asText() : "AI price recommendation";

        BoundsValidator.ValidatedPricing valPricing = boundsValidator.validatePricing(
                product, rawPrice, rawDirection, rawPricingConfidence, pricingReasoning
        );

        // Reorder extraction
        JsonNode reorderNode = root.path("reorder");
        Integer rawQty = reorderNode.has("recommendedQuantity")
                ? reorderNode.get("recommendedQuantity").asInt()
                : Math.max(1, (product.getReorderThreshold() * 3) - product.getStockLevel());
        Integer rawLeadTime = reorderNode.has("suggestedLeadTimeDays")
                ? reorderNode.get("suggestedLeadTimeDays").asInt()
                : 5;
        Double rawReorderConfidence = reorderNode.has("confidence") ? reorderNode.get("confidence").asDouble() : 0.85;
        String reorderReasoning = reorderNode.has("reasoning") ? reorderNode.get("reasoning").asText() : "AI replenishment recommendation";

        BoundsValidator.ValidatedReorder valReorder = boundsValidator.validateReorder(
                product, rawQty, rawLeadTime, rawReorderConfidence, reorderReasoning
        );

        PricingSuggestionResult pricingResult = PricingSuggestionResult.builder()
                .recommendedPrice(valPricing.price)
                .changeDirection(valPricing.direction)
                .confidence(valPricing.confidence)
                .reasoning(valPricing.reasoning)
                .strategyUsed("AI_ADVISOR")
                .isFallback(valPricing.wasClamped)
                .build();

        ReorderSuggestionResult reorderResult = ReorderSuggestionResult.builder()
                .recommendedQuantity(valReorder.quantity)
                .suggestedLeadTimeDays(valReorder.leadTimeDays)
                .confidence(valReorder.confidence)
                .reasoning(valReorder.reasoning)
                .strategyUsed("AI_ADVISOR")
                .isFallback(valReorder.wasClamped)
                .build();

        return new AIAdviceBundle(pricingResult, reorderResult);
    }
}

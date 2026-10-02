package com.zycus.commerce.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.zycus.domain.model.ChangeDirection;
import com.zycus.domain.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class BoundsValidator {

    private static final BigDecimal MIN_PRICE_FACTOR = BigDecimal.valueOf(0.40); // Max 60% discount
    private static final BigDecimal MAX_PRICE_FACTOR = BigDecimal.valueOf(3.00); // Max 300% increase
    private static final int MAX_REORDER_QUANTITY = 50_000;

    public static class ValidatedPricing {
        public final BigDecimal price;
        public final ChangeDirection direction;
        public final Double confidence;
        public final String reasoning;
        public final boolean wasClamped;

        public ValidatedPricing(BigDecimal price, ChangeDirection direction, Double confidence, String reasoning, boolean wasClamped) {
            this.price = price;
            this.direction = direction;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.wasClamped = wasClamped;
        }
    }

    public static class ValidatedReorder {
        public final Integer quantity;
        public final Integer leadTimeDays;
        public final Double confidence;
        public final String reasoning;
        public final boolean wasClamped;

        public ValidatedReorder(Integer quantity, Integer leadTimeDays, Double confidence, String reasoning, boolean wasClamped) {
            this.quantity = quantity;
            this.leadTimeDays = leadTimeDays;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.wasClamped = wasClamped;
        }
    }

    public ValidatedPricing validatePricing(Product product, BigDecimal rawPrice, ChangeDirection rawDirection, Double rawConfidence, String reasoning) {
        BigDecimal current = product.getCurrentPrice();
        BigDecimal minAllowable = current.multiply(MIN_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxAllowable = current.multiply(MAX_PRICE_FACTOR).setScale(2, RoundingMode.HALF_UP);

        BigDecimal price = rawPrice != null ? rawPrice.setScale(2, RoundingMode.HALF_UP) : current;
        boolean clamped = false;
        String note = "";

        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(minAllowable) < 0) {
            log.warn("Price {} for SKU {} is below safety floor {}. Clamping.", price, product.getSku(), minAllowable);
            price = minAllowable;
            clamped = true;
            note = " [Safety Guard: Clamped to minimum allowable -60% bound]";
        } else if (price.compareTo(maxAllowable) > 0) {
            log.warn("Price {} for SKU {} is above safety ceiling {}. Clamping.", price, product.getSku(), maxAllowable);
            price = maxAllowable;
            clamped = true;
            note = " [Safety Guard: Clamped to maximum allowable +300% bound]";
        }

        // Recompute direction based on final price
        ChangeDirection direction = rawDirection;
        int cmp = price.compareTo(current);
        if (cmp > 0) {
            direction = ChangeDirection.INCREASE;
        } else if (cmp < 0) {
            direction = ChangeDirection.DECREASE;
        } else {
            direction = ChangeDirection.HOLD;
        }

        double confidence = rawConfidence != null ? Math.min(0.99, Math.max(0.10, rawConfidence)) : 0.85;
        String finalReasoning = (reasoning != null ? reasoning.trim() : "AI recommended adjustment.") + note;

        return new ValidatedPricing(price, direction, confidence, finalReasoning, clamped);
    }

    public ValidatedReorder validateReorder(Product product, Integer rawQty, Integer rawLeadTime, Double rawConfidence, String reasoning) {
        int qty = rawQty != null ? rawQty : Math.max(1, (product.getReorderThreshold() * 3) - product.getStockLevel());
        boolean clamped = false;
        String note = "";

        if (qty < 1) {
            qty = 1;
            clamped = true;
            note = " [Safety Guard: Clamped to min 1 unit]";
        } else if (qty > MAX_REORDER_QUANTITY) {
            qty = MAX_REORDER_QUANTITY;
            clamped = true;
            note = " [Safety Guard: Clamped to max batch cap]";
        }

        int leadTime = (rawLeadTime != null && rawLeadTime >= 1 && rawLeadTime <= 90) ? rawLeadTime : 5;
        double confidence = rawConfidence != null ? Math.min(0.99, Math.max(0.10, rawConfidence)) : 0.85;
        String finalReasoning = (reasoning != null ? reasoning.trim() : "AI recommended replenishment.") + note;

        return new ValidatedReorder(qty, leadTime, confidence, finalReasoning, clamped);
    }
}

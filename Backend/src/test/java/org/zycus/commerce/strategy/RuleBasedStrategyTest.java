package org.zycus.commerce.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zycus.commerce.context.CommerceContext;
import org.zycus.domain.model.Category;
import org.zycus.domain.model.ChangeDirection;
import org.zycus.domain.model.Product;
import org.zycus.domain.model.ProductStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedStrategyTest {

    private RuleBasedPricingStrategy pricingStrategy;
    private RuleBasedReorderStrategy reorderStrategy;

    @BeforeEach
    void setUp() {
        pricingStrategy = new RuleBasedPricingStrategy();
        reorderStrategy = new RuleBasedReorderStrategy();
    }

    @Test
    @DisplayName("Low stock should trigger +10% price increase recommendation")
    void testLowStockPriceIncrease() {
        Product product = Product.builder()
                .id("PRD-TEST-1")
                .sku("SKU-TEST-1")
                .name("Test SKU")
                .category(Category.ELECTRONICS)
                .currentPrice(new BigDecimal("100.00"))
                .stockLevel(5) // Below threshold 10
                .reorderThreshold(10)
                .demandVelocity(1)
                .status(ProductStatus.ACTIVE)
                .build();

        CommerceContext context = CommerceContext.builder()
                .categoryAverageVelocity(2.0)
                .build();

        PricingSuggestionResult result = pricingStrategy.evaluate(product, context);

        assertEquals(new BigDecimal("110.00"), result.getRecommendedPrice());
        assertEquals(ChangeDirection.INCREASE, result.getChangeDirection());
        assertTrue(result.getConfidence() >= 0.85);
        assertTrue(result.getReasoning().contains("critically below reorder threshold"));
    }

    @Test
    @DisplayName("Demand surge (>2x category average) should trigger +5% price increase")
    void testDemandSurgePriceIncrease() {
        Product product = Product.builder()
                .id("PRD-TEST-2")
                .sku("SKU-TEST-2")
                .name("Viral Item")
                .category(Category.APPAREL)
                .currentPrice(new BigDecimal("50.00"))
                .stockLevel(50) // Healthy stock
                .reorderThreshold(15)
                .demandVelocity(10) // 10 is > 2 * 3.0
                .status(ProductStatus.ACTIVE)
                .build();

        CommerceContext context = CommerceContext.builder()
                .categoryAverageVelocity(3.0)
                .build();

        PricingSuggestionResult result = pricingStrategy.evaluate(product, context);

        assertEquals(new BigDecimal("52.50"), result.getRecommendedPrice());
        assertEquals(ChangeDirection.INCREASE, result.getChangeDirection());
        assertTrue(result.getReasoning().contains(">2x category average"));
    }

    @Test
    @DisplayName("Normal inventory and demand should recommend HOLD")
    void testEquilibriumHold() {
        Product product = Product.builder()
                .id("PRD-TEST-3")
                .sku("SKU-TEST-3")
                .name("Stable Item")
                .category(Category.HOME)
                .currentPrice(new BigDecimal("40.00"))
                .stockLevel(30)
                .reorderThreshold(10)
                .demandVelocity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        CommerceContext context = CommerceContext.builder()
                .categoryAverageVelocity(2.5)
                .build();

        PricingSuggestionResult result = pricingStrategy.evaluate(product, context);

        assertEquals(new BigDecimal("40.00"), result.getRecommendedPrice());
        assertEquals(ChangeDirection.HOLD, result.getChangeDirection());
    }

    @Test
    @DisplayName("Reorder calculation: (threshold * 3) - stock, min 1")
    void testReorderQuantityCalculation() {
        Product product = Product.builder()
                .id("PRD-TEST-4")
                .category(Category.APPAREL)
                .stockLevel(8)
                .reorderThreshold(15)
                .build();

        CommerceContext context = CommerceContext.builder().build();
        ReorderSuggestionResult result = reorderStrategy.evaluate(product, context);

        // Expected: (15 * 3) - 8 = 37
        assertEquals(37, result.getRecommendedQuantity());
        assertEquals(5, result.getSuggestedLeadTimeDays()); // Apparel default
    }
}

package org.zycus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.zycus.domain.model.Product;
import org.zycus.domain.model.ProductStatus;
import org.zycus.domain.model.SuggestionStatus;
import org.zycus.domain.repository.PricingSuggestionRepository;
import org.zycus.domain.repository.ProductRepository;
import org.zycus.domain.repository.ReorderSuggestionRepository;
import org.zycus.service.PricingSuggestionService;
import org.zycus.service.ProductService;
import org.zycus.service.ReorderSuggestionService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AgenticLoopIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private PricingSuggestionService pricingSuggestionService;

    @Autowired
    private ReorderSuggestionService reorderSuggestionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Test
    @Transactional
    @DisplayName("Simulated order should decrement stock, increment velocity, and record inventory audit")
    void testSimulateOrder() {
        Product prd1 = productService.getProductById("PRD-001");
        int originalStock = prd1.getStockLevel();
        int originalVelocity = prd1.getDemandVelocity();

        Product updated = productService.simulateOrder("PRD-001", 2);

        assertEquals(originalStock - 2, updated.getStockLevel());
        assertEquals(originalVelocity + 2, updated.getDemandVelocity());
    }

    @Test
    @Transactional
    @DisplayName("Accepting pricing suggestion should atomically update currentPrice and revert status")
    void testAcceptPricingSuggestion() {
        // PRD-003 was seeded with pending suggestions
        var suggestions = pricingSuggestionService.search("PRD-003", SuggestionStatus.PENDING, null);
        assertFalse(suggestions.isEmpty(), "PRD-003 should have initial pending pricing suggestion");

        var suggestion = suggestions.get(0);
        BigDecimal targetPrice = suggestion.getRecommendedPrice();

        var accepted = pricingSuggestionService.updateStatus(suggestion.getId(), SuggestionStatus.ACCEPTED);

        assertEquals(SuggestionStatus.ACCEPTED, accepted.getStatus());
        Product product = productService.getProductById("PRD-003");
        assertEquals(targetPrice, product.getCurrentPrice());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    @Transactional
    @DisplayName("Accepting reorder suggestion should increment stockLevel by recommendedQuantity")
    void testAcceptReorderSuggestion() {
        var suggestions = reorderSuggestionService.search("PRD-003", SuggestionStatus.PENDING, null);
        assertFalse(suggestions.isEmpty(), "PRD-003 should have initial pending reorder suggestion");

        var suggestion = suggestions.get(0);
        int incomingQty = suggestion.getRecommendedQuantity();

        Product productBefore = productService.getProductById("PRD-003");
        int stockBefore = productBefore.getStockLevel();

        var accepted = reorderSuggestionService.updateStatus(suggestion.getId(), SuggestionStatus.ACCEPTED);

        assertEquals(SuggestionStatus.ACCEPTED, accepted.getStatus());
        Product productAfter = productService.getProductById("PRD-003");
        assertEquals(stockBefore + incomingQty, productAfter.getStockLevel());
    }
}

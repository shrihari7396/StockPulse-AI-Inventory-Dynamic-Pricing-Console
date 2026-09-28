package org.zycus.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.zycus.domain.model.*;
import org.zycus.domain.repository.PricingSuggestionRepository;
import org.zycus.domain.repository.ProductRepository;
import org.zycus.domain.repository.ReorderSuggestionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Products already initialized ({} found).", productRepository.count());
            return;
        }

        log.info("Seeding initial Addendum A catalogue products and sprint 2 placeholders...");

        List<Product> seedProducts = List.of(
                Product.builder()
                        .id("PRD-001")
                        .sku("SKU-ELEC-001")
                        .name("Wireless Earbuds Pro")
                        .category(Category.ELECTRONICS)
                        .currentPrice(new BigDecimal("79.99"))
                        .stockLevel(45)
                        .reorderThreshold(20)
                        .demandVelocity(3)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("35.00"))
                        .supplierId("SUP-TECH-01")
                        .competitorPrice(new BigDecimal("84.50"))
                        .build(),

                Product.builder()
                        .id("PRD-002")
                        .sku("SKU-ELEC-002")
                        .name("USB-C Hub 7-Port")
                        .category(Category.ELECTRONICS)
                        .currentPrice(new BigDecimal("34.99"))
                        .stockLevel(120)
                        .reorderThreshold(30)
                        .demandVelocity(1)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("14.50"))
                        .supplierId("SUP-TECH-02")
                        .competitorPrice(new BigDecimal("36.00"))
                        .build(),

                Product.builder()
                        .id("PRD-003")
                        .sku("SKU-APP-001")
                        .name("Organic Cotton T-Shirt")
                        .category(Category.APPAREL)
                        .currentPrice(new BigDecimal("24.99"))
                        .stockLevel(8)
                        .reorderThreshold(15)
                        .demandVelocity(12)
                        .status(ProductStatus.PRICE_REVIEW_PENDING)
                        .costPrice(new BigDecimal("9.20"))
                        .supplierId("SUP-TEXTILE-01")
                        .competitorPrice(new BigDecimal("26.00"))
                        .build(),

                Product.builder()
                        .id("PRD-004")
                        .sku("SKU-APP-002")
                        .name("Running Shorts — Navy")
                        .category(Category.APPAREL)
                        .currentPrice(new BigDecimal("39.99"))
                        .stockLevel(55)
                        .reorderThreshold(20)
                        .demandVelocity(2)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("16.00"))
                        .supplierId("SUP-TEXTILE-02")
                        .competitorPrice(new BigDecimal("42.00"))
                        .build(),

                Product.builder()
                        .id("PRD-005")
                        .sku("SKU-HOME-001")
                        .name("Ceramic Pour-Over Set")
                        .category(Category.HOME)
                        .currentPrice(new BigDecimal("49.99"))
                        .stockLevel(22)
                        .reorderThreshold(10)
                        .demandVelocity(4)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("21.50"))
                        .supplierId("SUP-CRAFT-01")
                        .competitorPrice(new BigDecimal("52.00"))
                        .build(),

                Product.builder()
                        .id("PRD-006")
                        .sku("SKU-HOME-002")
                        .name("LED Desk Lamp — Dimmable")
                        .category(Category.HOME)
                        .currentPrice(new BigDecimal("59.99"))
                        .stockLevel(0)
                        .reorderThreshold(15)
                        .demandVelocity(0)
                        .status(ProductStatus.OUT_OF_STOCK)
                        .costPrice(new BigDecimal("24.00"))
                        .supplierId("SUP-LIGHT-01")
                        .competitorPrice(new BigDecimal("61.00"))
                        .build(),

                Product.builder()
                        .id("PRD-007")
                        .sku("SKU-ELEC-003")
                        .name("Portable Charger 20K")
                        .category(Category.ELECTRONICS)
                        .currentPrice(new BigDecimal("44.99"))
                        .stockLevel(18)
                        .reorderThreshold(25)
                        .demandVelocity(8)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("19.00"))
                        .supplierId("SUP-TECH-01")
                        .competitorPrice(new BigDecimal("45.00"))
                        .build(),

                Product.builder()
                        .id("PRD-008")
                        .sku("SKU-APP-003")
                        .name("Hoodie — Heather Grey")
                        .category(Category.APPAREL)
                        .currentPrice(new BigDecimal("54.99"))
                        .stockLevel(11)
                        .reorderThreshold(12)
                        .demandVelocity(15)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(new BigDecimal("22.00"))
                        .supplierId("SUP-TEXTILE-01")
                        .competitorPrice(new BigDecimal("59.99"))
                        .build()
        );

        productRepository.saveAll(seedProducts);
        log.info("Successfully seeded {} products into database.", seedProducts.size());

        // Preload an initial pending suggestion for PRD-003 (T-Shirt is low stock at seed time)
        Product prd003 = productRepository.findById("PRD-003").orElse(null);
        if (prd003 != null) {
            PricingSuggestion initialPricing = PricingSuggestion.builder()
                    .product(prd003)
                    .currentPrice(prd003.getCurrentPrice())
                    .recommendedPrice(new BigDecimal("27.49"))
                    .changeDirection(ChangeDirection.INCREASE)
                    .confidence(0.92)
                    .reasoning("Stock (8 units) is critically below reorder threshold (15) with high demand velocity (12 orders/24h). Recommend +10% price protection to preserve margin while reorder is underway.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(TriggerReason.INVENTORY_LOW)
                    .strategyUsed("AI_ADVISOR_INITIAL")
                    .createdAt(LocalDateTime.now())
                    .build();
            pricingSuggestionRepository.save(initialPricing);

            ReorderSuggestion initialReorder = ReorderSuggestion.builder()
                    .product(prd003)
                    .currentStock(prd003.getStockLevel())
                    .recommendedQuantity(37) // (15 * 3) - 8 = 37
                    .suggestedLeadTimeDays(4)
                    .confidence(0.89)
                    .reasoning("Replenishment required immediately. Current stock of 8 covers less than 16 hours of current velocity (12 units/day). Target safety stock is 45 units.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(TriggerReason.INVENTORY_LOW)
                    .strategyUsed("AI_ADVISOR_INITIAL")
                    .createdAt(LocalDateTime.now())
                    .build();
            reorderSuggestionRepository.save(initialReorder);
            log.info("Seeded initial pending suggestions for PRD-003 demo path.");
        }
    }
}

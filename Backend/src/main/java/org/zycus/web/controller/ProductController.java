package org.zycus.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zycus.commerce.advisor.CommerceAdvisorService;
import org.zycus.domain.model.*;
import org.zycus.service.ProductService;
import org.zycus.web.dto.CreateProductRequest;
import org.zycus.web.dto.SimulateOrderRequest;
import org.zycus.web.dto.UpdateStockRequest;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final CommerceAdvisorService commerceAdvisorService;

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(productService.getProducts(status, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest request) {
        log.info("Received stock update request for product ID '{}': new stock = {}", id, request.getStockLevel());
        Product updated = productService.updateStock(id, request.getStockLevel());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<Product> simulateOrder(
            @PathVariable String id,
            @RequestBody(required = false) SimulateOrderRequest request) {
        int qty = (request != null && request.getQuantity() != null) ? request.getQuantity() : 1;
        log.info("Simulating customer order for product ID '{}': quantity = {}", id, qty);
        Product updated = productService.simulateOrder(id, qty);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<PricingSuggestion> suggestPricing(@PathVariable String id) {
        log.info("On-demand pricing suggestion requested for product ID '{}'", id);
        Product product = productService.getProductById(id);
        PricingSuggestion suggestion = commerceAdvisorService.generatePricingSuggestion(
                product, TriggerReason.MANUAL, "On-demand manual pricing review requested via API."
        );
        return ResponseEntity.ok(suggestion);
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<ReorderSuggestion> suggestReorder(@PathVariable String id) {
        log.info("On-demand reorder suggestion requested for product ID '{}'", id);
        Product product = productService.getProductById(id);
        ReorderSuggestion suggestion = commerceAdvisorService.generateReorderSuggestion(
                product, TriggerReason.MANUAL, "On-demand manual replenishment review requested via API."
        );
        return ResponseEntity.ok(suggestion);
    }
}

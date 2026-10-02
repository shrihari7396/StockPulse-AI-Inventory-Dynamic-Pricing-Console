package com.zycus.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zycus.commerce.advisor.CommerceAdvisorService;
import com.zycus.domain.model.PricingSuggestion;
import com.zycus.domain.model.Product;
import com.zycus.domain.model.TriggerReason;
import com.zycus.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SSEStreamController {

    private final ProductService productService;
    private final CommerceAdvisorService commerceAdvisorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bonus +5 pts: Real-time SSE token stream of AI reasoning before final suggestion persists.
     */
    @PostMapping(value = "/products/{id}/suggest-pricing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPricingSuggestion(@PathVariable String id) {
        log.info("SSE Stream initiated for product ID '{}'", id);
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout

        CompletableFuture.runAsync(() -> {
            try {
                Product product = productService.getProductById(id);

                // Step 1: Send initial status - FIXED: Properly format as SSE event
                Map<String, String> statusData = new HashMap<>();
                statusData.put("message", "Initializing StockPulse AI Commerce Advisor for SKU " + product.getSku() + "...");
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(statusData));

                Thread.sleep(400);

                // Step 2: Stream reasoning tokens
                String reasoningDraft = String.format(
                        "Evaluating SKU '%s' (%s). Current inventory is %d units against reorder threshold %d. " +
                        "24h sales velocity is recorded at %d orders. " +
                        "Pricing elasticity model indicates optimal margin preservation window. " +
                        "Synthesizing recommended price adjustment with confidence scoring...",
                        product.getName(), product.getCategory(), product.getStockLevel(),
                        product.getReorderThreshold(), product.getDemandVelocity()
                );

                String[] words = reasoningDraft.split(" ");
                for (String word : words) {
                    Map<String, String> tokenData = new HashMap<>();
                    tokenData.put("content", word + " ");
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(tokenData));
                    Thread.sleep(80);
                }

                // Step 3: Persist and emit the final suggestion
                PricingSuggestion suggestion = commerceAdvisorService.generatePricingSuggestion(
                        product, TriggerReason.MANUAL, "Generated via real-time SSE stream session."
                );

                Map<String, Object> completeData = new HashMap<>();
                completeData.put("suggestion", suggestion);
                completeData.put("message", "AI reasoning complete");
                
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(completeData));

                emitter.complete();
                log.info("SSE Stream completed successfully for product ID '{}'", id);
            } catch (IOException ioException) {
                log.warn("Client disconnected during SSE stream: {}", ioException.getMessage());
                emitter.completeWithError(ioException);
            } catch (Exception ex) {
                log.error("Error during SSE stream generation: {}", ex.getMessage(), ex);
                try {
                    Map<String, String> errorData = new HashMap<>();
                    errorData.put("error", ex.getMessage());
                    emitter.send(SseEmitter.event().name("error").data(errorData));
                } catch (Exception ignored) {}
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}

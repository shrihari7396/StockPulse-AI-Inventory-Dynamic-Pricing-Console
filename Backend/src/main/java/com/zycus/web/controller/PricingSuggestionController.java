package com.zycus.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zycus.domain.model.PricingSuggestion;
import com.zycus.domain.model.SuggestionStatus;
import com.zycus.domain.model.TriggerReason;
import com.zycus.service.PricingSuggestionService;
import com.zycus.web.dto.UpdateSuggestionStatusRequest;

import java.util.List;

@RestController
@RequestMapping("/pricing-suggestions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PricingSuggestionController {

    private final PricingSuggestionService pricingSuggestionService;

    @GetMapping
    public ResponseEntity<List<PricingSuggestion>> getSuggestions(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(required = false) TriggerReason triggerReason) {
        return ResponseEntity.ok(pricingSuggestionService.search(productId, status, triggerReason));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PricingSuggestion> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pricingSuggestionService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PricingSuggestion> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSuggestionStatusRequest request) {
        log.info("Updating status of pricing suggestion #{} to {}", id, request.getStatus());
        PricingSuggestion updated = pricingSuggestionService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}

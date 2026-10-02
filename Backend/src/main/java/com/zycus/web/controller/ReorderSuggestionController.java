package com.zycus.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zycus.domain.model.ReorderSuggestion;
import com.zycus.domain.model.SuggestionStatus;
import com.zycus.domain.model.TriggerReason;
import com.zycus.service.ReorderSuggestionService;
import com.zycus.web.dto.UpdateSuggestionStatusRequest;

import java.util.List;

@RestController
@RequestMapping("/reorder-suggestions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReorderSuggestionController {

    private final ReorderSuggestionService reorderSuggestionService;

    @GetMapping
    public ResponseEntity<List<ReorderSuggestion>> getSuggestions(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(required = false) TriggerReason triggerReason) {
        return ResponseEntity.ok(reorderSuggestionService.search(productId, status, triggerReason));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReorderSuggestion> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reorderSuggestionService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReorderSuggestion> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSuggestionStatusRequest request) {
        log.info("Updating status of reorder suggestion #{} to {}", id, request.getStatus());
        ReorderSuggestion updated = reorderSuggestionService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}

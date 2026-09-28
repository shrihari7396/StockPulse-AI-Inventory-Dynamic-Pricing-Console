package org.zycus.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zycus.commerce.strategy.StrategyRegistry;
import org.zycus.web.dto.StrategyConfigDto;
import org.zycus.web.dto.UpdateStrategyConfigRequest;

@RestController
@RequestMapping("/api/config/strategy")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StrategyConfigController {

    private final StrategyRegistry strategyRegistry;

    @GetMapping
    public ResponseEntity<StrategyConfigDto> getConfig() {
        StrategyConfigDto dto = StrategyConfigDto.builder()
                .activePricingStrategy(strategyRegistry.getActivePricingStrategyName())
                .activeReorderStrategy(strategyRegistry.getActiveReorderStrategyName())
                .availablePricingStrategies(strategyRegistry.getAvailablePricingStrategies())
                .availableReorderStrategies(strategyRegistry.getAvailableReorderStrategies())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<StrategyConfigDto> updateConfig(@RequestBody UpdateStrategyConfigRequest request) {
        if (request.getPricingStrategy() != null && !request.getPricingStrategy().isBlank()) {
            strategyRegistry.setActivePricingStrategy(request.getPricingStrategy());
            log.info("Runtime pricing strategy set to: {}", request.getPricingStrategy());
        }
        if (request.getReorderStrategy() != null && !request.getReorderStrategy().isBlank()) {
            strategyRegistry.setActiveReorderStrategy(request.getReorderStrategy());
            log.info("Runtime reorder strategy set to: {}", request.getReorderStrategy());
        }

        return getConfig();
    }
}

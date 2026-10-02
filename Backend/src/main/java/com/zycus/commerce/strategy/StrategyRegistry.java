package com.zycus.commerce.strategy;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class StrategyRegistry {

    private final Map<String, PricingStrategy> pricingStrategies = new ConcurrentHashMap<>();
    private final Map<String, ReorderStrategy> reorderStrategies = new ConcurrentHashMap<>();

    private final AtomicReference<String> activePricingStrategy = new AtomicReference<>("RULE_BASED");
    private final AtomicReference<String> activeReorderStrategy = new AtomicReference<>("RULE_BASED");

    @Value("${stockpulse.strategy.pricing:RULE_BASED}")
    private String defaultPricingStrategy;

    @Value("${stockpulse.strategy.reorder:RULE_BASED}")
    private String defaultReorderStrategy;

    public StrategyRegistry(List<PricingStrategy> pricingList, List<ReorderStrategy> reorderList) {
        for (PricingStrategy ps : pricingList) {
            pricingStrategies.put(ps.getStrategyName().toUpperCase(), ps);
        }
        for (ReorderStrategy rs : reorderList) {
            reorderStrategies.put(rs.getStrategyName().toUpperCase(), rs);
        }
    }

    @PostConstruct
    public void init() {
        if (pricingStrategies.containsKey(defaultPricingStrategy.toUpperCase())) {
            activePricingStrategy.set(defaultPricingStrategy.toUpperCase());
        }
        if (reorderStrategies.containsKey(defaultReorderStrategy.toUpperCase())) {
            activeReorderStrategy.set(defaultReorderStrategy.toUpperCase());
        }
        log.info("StrategyRegistry initialized. Active Pricing Strategy: '{}', Active Reorder Strategy: '{}'",
                activePricingStrategy.get(), activeReorderStrategy.get());
    }

    public PricingStrategy getActivePricingStrategy() {
        String key = activePricingStrategy.get();
        PricingStrategy strategy = pricingStrategies.get(key);
        if (strategy == null) {
            log.warn("Active pricing strategy '{}' not found. Falling back to RULE_BASED.", key);
            return pricingStrategies.get("RULE_BASED");
        }
        return strategy;
    }

    public ReorderStrategy getActiveReorderStrategy() {
        String key = activeReorderStrategy.get();
        ReorderStrategy strategy = reorderStrategies.get(key);
        if (strategy == null) {
            log.warn("Active reorder strategy '{}' not found. Falling back to RULE_BASED.", key);
            return reorderStrategies.get("RULE_BASED");
        }
        return strategy;
    }

    public synchronized void setActivePricingStrategy(String strategyName) {
        String upper = strategyName.toUpperCase();
        if (!pricingStrategies.containsKey(upper)) {
            throw new IllegalArgumentException("Unknown pricing strategy: " + strategyName +
                    ". Available: " + pricingStrategies.keySet());
        }
        activePricingStrategy.set(upper);
        log.info("Runtime pricing strategy switched to '{}'", upper);
    }

    public synchronized void setActiveReorderStrategy(String strategyName) {
        String upper = strategyName.toUpperCase();
        if (!reorderStrategies.containsKey(upper)) {
            throw new IllegalArgumentException("Unknown reorder strategy: " + strategyName +
                    ". Available: " + reorderStrategies.keySet());
        }
        activeReorderStrategy.set(upper);
        log.info("Runtime reorder strategy switched to '{}'", upper);
    }

    public String getActivePricingStrategyName() {
        return activePricingStrategy.get();
    }

    public String getActiveReorderStrategyName() {
        return activeReorderStrategy.get();
    }

    public Set<String> getAvailablePricingStrategies() {
        return Collections.unmodifiableSet(pricingStrategies.keySet());
    }

    public Set<String> getAvailableReorderStrategies() {
        return Collections.unmodifiableSet(reorderStrategies.keySet());
    }

    /**
     * Sprint 2 Extensibility Point:
     * Easily register custom strategies (e.g. CompetitorAwareStrategy) at runtime.
     */
    public void registerPricingStrategy(PricingStrategy strategy) {
        pricingStrategies.put(strategy.getStrategyName().toUpperCase(), strategy);
        log.info("Registered new PricingStrategy: '{}'", strategy.getStrategyName());
    }
}

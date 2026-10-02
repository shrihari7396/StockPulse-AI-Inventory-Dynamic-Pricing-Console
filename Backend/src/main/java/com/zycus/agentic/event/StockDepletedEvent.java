package com.zycus.agentic.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockDepletedEvent {
    private final String productId;
    private final int currentStock;
    private final int threshold;
}

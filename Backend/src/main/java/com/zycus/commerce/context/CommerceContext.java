package com.zycus.commerce.context;

import com.zycus.domain.model.TriggerReason;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommerceContext {
    private final Double categoryAverageVelocity;
    private final TriggerReason triggerReason;
    private final Integer peerCount;
    private final String note;
}

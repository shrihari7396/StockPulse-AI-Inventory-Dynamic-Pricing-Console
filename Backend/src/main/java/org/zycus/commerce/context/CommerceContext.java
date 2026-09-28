package org.zycus.commerce.context;

import lombok.Builder;
import lombok.Getter;
import org.zycus.domain.model.TriggerReason;

@Getter
@Builder
public class CommerceContext {
    private final Double categoryAverageVelocity;
    private final TriggerReason triggerReason;
    private final Integer peerCount;
    private final String note;
}

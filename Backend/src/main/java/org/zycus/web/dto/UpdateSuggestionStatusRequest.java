package org.zycus.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.zycus.domain.model.SuggestionStatus;

@Data
public class UpdateSuggestionStatusRequest {
    @NotNull(message = "status is required (ACCEPTED or REJECTED)")
    private SuggestionStatus status;
}

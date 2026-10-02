package com.zycus.web.dto;

import com.zycus.domain.model.SuggestionStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSuggestionStatusRequest {
    @NotNull(message = "status is required (ACCEPTED or REJECTED)")
    private SuggestionStatus status;
}

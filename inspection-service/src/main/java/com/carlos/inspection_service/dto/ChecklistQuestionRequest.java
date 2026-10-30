package com.carlos.inspection_service.dto;

import com.carlos.inspection_service.model.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChecklistQuestionRequest(
    @NotBlank @Size(max = 1000) String text,
    @NotBlank @Size(max = 150) String category,
    @NotNull @Min(1) Integer position,
    @NotNull Severity severityIfFailed
) {}

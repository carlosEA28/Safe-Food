package com.carlos.inspection_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistTemplateRequest(
    @NotBlank @Size(max = 150) String name,
    @Size(max = 2000) String description
) {}

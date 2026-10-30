package com.carlos.inspection_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChecklistTemplateDetailResponse(UUID id, String name, String description, boolean active,
                                              Instant createdAt, List<ChecklistQuestionResponse> questions) {}

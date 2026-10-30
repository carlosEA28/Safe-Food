package com.carlos.inspection_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ChecklistTemplateResponse(UUID id, String name, String description, boolean active, Instant createdAt) {}

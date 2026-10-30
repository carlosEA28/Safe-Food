package com.carlos.inspection_service.dto;

import com.carlos.inspection_service.model.Severity;
import java.util.UUID;

public record ChecklistQuestionResponse(UUID id, String text, String category, int position,
                                        Severity severityIfFailed, boolean active) {}

package com.carlos.inspection_service.dto;

import java.util.List;

public record ChecklistPageResponse(List<ChecklistTemplateResponse> content, int page, int size,
                                    long totalElements, int totalPages) {}

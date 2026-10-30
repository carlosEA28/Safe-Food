package com.carlos.inspection_service.service;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.model.ChecklistTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChecklistTemplateMapper {
    public ChecklistTemplateResponse toResponse(ChecklistTemplate template) {
        return new ChecklistTemplateResponse(template.getId(), template.getName(), template.getDescription(), template.isActive(), template.getCreatedAt());
    }
}

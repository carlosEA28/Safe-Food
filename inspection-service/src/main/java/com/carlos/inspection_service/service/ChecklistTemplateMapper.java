package com.carlos.inspection_service.service;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.dto.ChecklistTemplateDetailResponse;
import com.carlos.inspection_service.dto.ChecklistQuestionResponse;
import com.carlos.inspection_service.model.ChecklistQuestion;
import com.carlos.inspection_service.model.ChecklistTemplate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChecklistTemplateMapper {
    public ChecklistTemplateResponse toResponse(ChecklistTemplate template) {
        return new ChecklistTemplateResponse(template.getId(), template.getName(), template.getDescription(), template.isActive(), template.getCreatedAt());
    }

    public ChecklistQuestionResponse toResponse(ChecklistQuestion question) {
        return new ChecklistQuestionResponse(question.getId(), question.getText(), question.getCategory(),
            question.getPosition(), question.getSeverityIfFailed(), question.isActive());
    }

    public ChecklistTemplateDetailResponse toDetailResponse(ChecklistTemplate template, List<ChecklistQuestion> questions) {
        return new ChecklistTemplateDetailResponse(template.getId(), template.getName(), template.getDescription(),
            template.isActive(), template.getCreatedAt(), questions.stream().map(this::toResponse).toList());
    }
}

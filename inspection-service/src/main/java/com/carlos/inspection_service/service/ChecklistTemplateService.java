package com.carlos.inspection_service.service;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.dto.CreateChecklistTemplateRequest;
import com.carlos.inspection_service.model.ChecklistTemplate;
import com.carlos.inspection_service.repository.ChecklistTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistTemplateService {
    private final ChecklistTemplateRepository repository;
    private final ChecklistTemplateMapper mapper;

    public ChecklistTemplateService(ChecklistTemplateRepository repository, ChecklistTemplateMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ChecklistTemplateResponse create(CreateChecklistTemplateRequest request) {
        ChecklistTemplate template = new ChecklistTemplate(request.name().trim(), request.description());
        return mapper.toResponse(repository.save(template));
    }
}

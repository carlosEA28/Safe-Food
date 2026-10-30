package com.carlos.inspection_service.controller;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.dto.CreateChecklistTemplateRequest;
import com.carlos.inspection_service.service.ChecklistTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checklists")
public class ChecklistTemplateController {
    private final ChecklistTemplateService service;

    public ChecklistTemplateController(ChecklistTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistTemplateResponse create(@Valid @RequestBody CreateChecklistTemplateRequest request) {
        return service.create(request);
    }
}

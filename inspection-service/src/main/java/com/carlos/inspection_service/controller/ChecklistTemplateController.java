package com.carlos.inspection_service.controller;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.dto.ChecklistTemplateDetailResponse;
import com.carlos.inspection_service.dto.ChecklistPageResponse;
import com.carlos.inspection_service.dto.ChecklistQuestionRequest;
import com.carlos.inspection_service.dto.ChecklistQuestionResponse;
import com.carlos.inspection_service.dto.CreateChecklistTemplateRequest;
import com.carlos.inspection_service.service.ChecklistTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checklists")
@Validated
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

    @GetMapping
    public ChecklistPageResponse list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public ChecklistTemplateDetailResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ChecklistTemplateResponse update(@PathVariable UUID id, @Valid @RequestBody CreateChecklistTemplateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ChecklistTemplateResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChecklistQuestionResponse addQuestion(@PathVariable UUID id, @Valid @RequestBody ChecklistQuestionRequest request) {
        return service.addQuestion(id, request);
    }

    @PutMapping("/{id}/questions/{questionId}")
    public ChecklistQuestionResponse updateQuestion(@PathVariable UUID id, @PathVariable UUID questionId,
                                                    @Valid @RequestBody ChecklistQuestionRequest request) {
        return service.updateQuestion(id, questionId, request);
    }

    @PatchMapping("/{id}/questions/{questionId}/deactivate")
    public ChecklistQuestionResponse deactivateQuestion(@PathVariable UUID id, @PathVariable UUID questionId) {
        return service.deactivateQuestion(id, questionId);
    }
}

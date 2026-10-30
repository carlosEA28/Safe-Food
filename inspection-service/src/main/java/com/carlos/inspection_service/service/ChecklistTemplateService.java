package com.carlos.inspection_service.service;

import com.carlos.inspection_service.dto.ChecklistTemplateResponse;
import com.carlos.inspection_service.dto.ChecklistTemplateDetailResponse;
import com.carlos.inspection_service.dto.ChecklistPageResponse;
import com.carlos.inspection_service.dto.ChecklistQuestionRequest;
import com.carlos.inspection_service.dto.ChecklistQuestionResponse;
import com.carlos.inspection_service.dto.CreateChecklistTemplateRequest;
import com.carlos.inspection_service.exceptions.ChecklistConflictException;
import com.carlos.inspection_service.exceptions.ChecklistNotFoundException;
import com.carlos.inspection_service.exceptions.QuestionNotFoundException;
import com.carlos.inspection_service.model.ChecklistQuestion;
import com.carlos.inspection_service.model.ChecklistTemplate;
import com.carlos.inspection_service.repository.ChecklistQuestionRepository;
import com.carlos.inspection_service.repository.ChecklistTemplateRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistTemplateService {
    private final ChecklistTemplateRepository repository;
    private final ChecklistQuestionRepository questionRepository;
    private final ChecklistTemplateMapper mapper;

    public ChecklistTemplateService(ChecklistTemplateRepository repository, ChecklistQuestionRepository questionRepository,
                                    ChecklistTemplateMapper mapper) {
        this.repository = repository;
        this.questionRepository = questionRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ChecklistTemplateResponse create(CreateChecklistTemplateRequest request) {
        ChecklistTemplate template = new ChecklistTemplate(request.name().trim(), request.description());
        return mapper.toResponse(repository.save(template));
    }

    @Transactional(readOnly = true)
    public ChecklistPageResponse list(int page, int size) {
        Page<ChecklistTemplateResponse> results = repository
            .findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
            .map(mapper::toResponse);
        return new ChecklistPageResponse(results.getContent(), page, size, results.getTotalElements(), results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ChecklistTemplateDetailResponse get(UUID id) {
        ChecklistTemplate template = findTemplate(id);
        return mapper.toDetailResponse(template, questionRepository.findByTemplateIdOrderByPositionAsc(id));
    }

    @Transactional
    public ChecklistTemplateResponse update(UUID id, CreateChecklistTemplateRequest request) {
        ChecklistTemplate template = findTemplate(id);
        template.update(request.name().trim(), request.description());
        return mapper.toResponse(template);
    }

    @Transactional
    public ChecklistTemplateResponse deactivate(UUID id) {
        ChecklistTemplate template = findTemplate(id);
        template.deactivate();
        return mapper.toResponse(template);
    }

    @Transactional
    public ChecklistQuestionResponse addQuestion(UUID id, ChecklistQuestionRequest request) {
        ChecklistTemplate template = findTemplate(id);
        if (!template.isActive()) {
            throw new ChecklistConflictException("Cannot add a question to an inactive checklist");
        }
        if (questionRepository.existsByTemplateIdAndPosition(id, request.position())) {
            throw new ChecklistConflictException("Question position already exists in checklist");
        }
        ChecklistQuestion question = new ChecklistQuestion(template, request.text().trim(), request.category().trim(),
            request.position(), request.severityIfFailed());
        return mapper.toResponse(questionRepository.saveAndFlush(question));
    }

    @Transactional
    public ChecklistQuestionResponse updateQuestion(UUID id, UUID questionId, ChecklistQuestionRequest request) {
        findTemplate(id);
        ChecklistQuestion question = findQuestion(id, questionId);
        if (questionRepository.existsByTemplateIdAndPositionAndIdNot(id, request.position(), questionId)) {
            throw new ChecklistConflictException("Question position already exists in checklist");
        }
        question.update(request.text().trim(), request.category().trim(), request.position(), request.severityIfFailed());
        questionRepository.flush();
        return mapper.toResponse(question);
    }

    @Transactional
    public ChecklistQuestionResponse deactivateQuestion(UUID id, UUID questionId) {
        findTemplate(id);
        ChecklistQuestion question = findQuestion(id, questionId);
        question.deactivate();
        return mapper.toResponse(question);
    }

    private ChecklistTemplate findTemplate(UUID id) {
        return repository.findById(id).orElseThrow(ChecklistNotFoundException::new);
    }

    private ChecklistQuestion findQuestion(UUID id, UUID questionId) {
        return questionRepository.findByIdAndTemplateId(questionId, id).orElseThrow(QuestionNotFoundException::new);
    }
}

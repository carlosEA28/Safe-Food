package com.carlos.inspection_service.repository;

import com.carlos.inspection_service.model.ChecklistQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChecklistQuestionRepository extends JpaRepository<ChecklistQuestion, UUID> {
    List<ChecklistQuestion> findByTemplateIdOrderByPositionAsc(UUID templateId);
    boolean existsByTemplateIdAndPosition(UUID templateId, int position);
    boolean existsByTemplateIdAndPositionAndIdNot(UUID templateId, int position, UUID id);
    java.util.Optional<ChecklistQuestion> findByIdAndTemplateId(UUID id, UUID templateId);
}

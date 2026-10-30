package com.carlos.inspection_service.repository;

import com.carlos.inspection_service.model.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID> {}

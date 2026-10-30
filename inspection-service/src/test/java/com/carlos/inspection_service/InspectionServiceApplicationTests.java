package com.carlos.inspection_service;

import com.carlos.inspection_service.model.*;
import com.carlos.inspection_service.repository.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InspectionServiceApplicationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("inspection_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ChecklistTemplateRepository templates;
    @Autowired ChecklistQuestionRepository questions;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired MockMvc mvc;

    @Test
    void migrationsAreRepeatableAndPersistTemplateWithQuestions() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        ChecklistTemplate template = templates.saveAndFlush(new ChecklistTemplate("Hygiene", "Kitchen hygiene"));
        questions.saveAndFlush(new ChecklistQuestion(template, "Clean surfaces?", "Kitchen", 1, Severity.HIGH));
        questions.saveAndFlush(new ChecklistQuestion(template, "Wash hands?", "People", 2, Severity.MEDIUM));
        assertThat(questions.findByTemplateIdOrderByPositionAsc(template.getId()))
            .extracting(ChecklistQuestion::getPosition).containsExactly(1, 2);
    }

    @Test
    void databaseEnforcesPositionAndForeignKey() {
        UUID templateId = templates.saveAndFlush(new ChecklistTemplate("Storage", null)).getId();
        insertQuestion(templateId, 1);
        assertThatThrownBy(() -> insertQuestion(templateId, 1)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertQuestion(UUID.randomUUID(), 2)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private void insertQuestion(UUID templateId, int position) {
        jdbc.update("insert into checklist_question(id, template_id, text, category, position, severity_if_failed, active) values (?, ?, ?, ?, ?, ?, ?)",
            UUID.randomUUID(), templateId, "Temperature?", "Storage", position, "HIGH", true);
    }

    @Test
    void invalidFieldsReturnBadRequest() throws Exception {
        mvc.perform(post("/checklists").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").isString()).andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void healthIsAvailable() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void templateCanBeCreatedThroughApi() throws Exception {
        mvc.perform(post("/checklists").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" Hygiene \",\"description\":\"Kitchen\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Hygiene"))
            .andExpect(jsonPath("$.active").value(true));
    }
}

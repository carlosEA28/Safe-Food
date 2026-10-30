package com.carlos.inspection_service;

import com.carlos.inspection_service.model.*;
import com.carlos.inspection_service.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void checklistQuestionsAreOrderedAndCanBeUpdatedAndDeactivated() throws Exception {
        UUID id = createChecklist("Kitchen");
        UUID third = addQuestion(id, "Third?", 3);
        addQuestion(id, "First?", 1);
        addQuestion(id, "Second?", 2);

        mvc.perform(get("/checklists/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.questions.length()").value(3))
            .andExpect(jsonPath("$.questions[0].text").value("First?"))
            .andExpect(jsonPath("$.questions[1].text").value("Second?"))
            .andExpect(jsonPath("$.questions[2].text").value("Third?"));

        mvc.perform(put("/checklists/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\" Updated \",\"description\":\"New description\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        mvc.perform(put("/checklists/{id}/questions/{questionId}", id, third).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Revised?", 3)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.text").value("Revised?"));
        mvc.perform(patch("/checklists/{id}/questions/{questionId}/deactivate", id, third))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/checklists/{id}/deactivate", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/checklists/{id}", id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.questions.length()").value(3))
            .andExpect(jsonPath("$.questions[2].active").value(false));
    }

    @Test
    void invalidQuestionsDuplicatePositionsAndInactiveTemplatesAreRejected() throws Exception {
        UUID id = createChecklist("Storage");
        UUID questionId = addQuestion(id, "Temperature?", 1);
        UUID otherId = addQuestion(id, "Humidity?", 2);
        mvc.perform(post("/checklists/{id}/questions", id).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Another?", 1)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        mvc.perform(post("/checklists/{id}/questions", id).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("   ", 2)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/checklists/{id}/questions", id).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Bad position?", 0)))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/checklists/{id}/questions/{questionId}", id, questionId).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Temperature?", 1)))
            .andExpect(status().isOk());
        mvc.perform(put("/checklists/{id}/questions/{questionId}", id, otherId).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Humidity?", 1)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        mvc.perform(patch("/checklists/{id}/deactivate", id)).andExpect(status().isOk());
        mvc.perform(post("/checklists/{id}/questions", id).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("New?", 3)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void missingChecklistAndQuestionOwnershipReturnNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        UUID first = createChecklist("First");
        UUID second = createChecklist("Second");
        UUID questionId = addQuestion(first, "Owned?", 1);
        mvc.perform(get("/checklists/{id}", missingId)).andExpect(status().isNotFound());
        mvc.perform(put("/checklists/{id}", missingId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Missing\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/checklists/{id}/questions", missingId).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Missing?", 1)))
            .andExpect(status().isNotFound());
        mvc.perform(put("/checklists/{id}/questions/{questionId}", second, questionId).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody("Wrong owner?", 1)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listingIsPagedAndHasStableOrder() throws Exception {
        UUID firstId = createChecklist("Paged A");
        UUID secondId = createChecklist("Paged B");
        String firstPage = mvc.perform(get("/checklists?page=0&size=2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2)).andReturn().getResponse().getContentAsString();
        String repeated = mvc.perform(get("/checklists?page=0&size=2")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(new ObjectMapper().readTree(firstPage).get("content"))
            .isEqualTo(new ObjectMapper().readTree(repeated).get("content"));
        assertThat(new ObjectMapper().readTree(firstPage).get("content").get(0).get("id").asText())
            .isEqualTo(secondId.toString());
        assertThat(new ObjectMapper().readTree(firstPage).get("content").get(1).get("id").asText())
            .isEqualTo(firstId.toString());
        mvc.perform(get("/checklists?page=-1&size=2")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private UUID createChecklist(String name) throws Exception {
        String body = mvc.perform(post("/checklists").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(new ObjectMapper().readTree(body).get("id").asText());
    }

    private UUID addQuestion(UUID templateId, String text, int position) throws Exception {
        String body = mvc.perform(post("/checklists/{id}/questions", templateId).contentType(MediaType.APPLICATION_JSON)
            .content(questionBody(text, position)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(new ObjectMapper().readTree(body).get("id").asText());
    }

    private String questionBody(String text, int position) {
        return "{\"text\":\"" + text + "\",\"category\":\"Hygiene\",\"position\":" + position
            + ",\"severityIfFailed\":\"HIGH\"}";
    }
}

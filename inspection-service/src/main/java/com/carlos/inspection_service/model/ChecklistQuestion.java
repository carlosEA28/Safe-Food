package com.carlos.inspection_service.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "checklist_question", uniqueConstraints = @UniqueConstraint(name = "uk_checklist_question_template_position", columnNames = {"template_id", "position"}))
public class ChecklistQuestion {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ChecklistTemplate template;
    @Column(nullable = false, length = 1000)
    private String text;
    @Column(nullable = false, length = 150)
    private String category;
    @Column(nullable = false)
    private int position;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_if_failed", nullable = false, length = 10)
    private Severity severityIfFailed;
    @Column(nullable = false)
    private boolean active;

    protected ChecklistQuestion() {}

    public ChecklistQuestion(ChecklistTemplate template, String text, String category, int position, Severity severityIfFailed) {
        this.id = UUID.randomUUID();
        this.template = template;
        this.text = text;
        this.category = category;
        this.position = position;
        this.severityIfFailed = severityIfFailed;
        this.active = true;
    }

    public UUID getId() { return id; }
    public ChecklistTemplate getTemplate() { return template; }
    public String getText() { return text; }
    public String getCategory() { return category; }
    public int getPosition() { return position; }
    public Severity getSeverityIfFailed() { return severityIfFailed; }
    public boolean isActive() { return active; }
}

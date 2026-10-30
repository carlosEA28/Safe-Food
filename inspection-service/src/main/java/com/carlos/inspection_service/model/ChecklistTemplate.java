package com.carlos.inspection_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checklist_template")
public class ChecklistTemplate {
    @Id
    private UUID id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChecklistTemplate() {}

    public ChecklistTemplate(String name, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void deactivate() { this.active = false; }
}

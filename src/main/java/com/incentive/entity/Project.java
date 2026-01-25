package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project extends PanacheEntity {

    @NotBlank(message = "Nome do projeto é obrigatório")
    @Size(min = 3, max = 100, message = "Nome do projeto deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    public String name;

    @Size(max = 500)
    @Column(length = 500)
    public String description;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Panache Active Record methods
    public static List<Project> findActiveProjects() {
        return list("active", true);
    }

    public static List<Project> findByName(String name) {
        return list("name like ?1", "%" + name + "%");
    }
}

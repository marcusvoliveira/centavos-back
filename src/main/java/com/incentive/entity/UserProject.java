package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "user_projects", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "project_id"})
})
public class UserProject extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    public Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Role role;

    @Column(precision = 5, scale = 2)
    public BigDecimal participation;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Panache Active Record methods
    public static List<UserProject> findByUserId(Long userId) {
        return list("user.id", userId);
    }

    public static List<UserProject> findByProjectId(Long projectId) {
        return list("project.id", projectId);
    }

    public static Optional<UserProject> findByUserAndProject(Long userId, Long projectId) {
        return find("user.id = ?1 and project.id = ?2", userId, projectId).firstResultOptional();
    }

    public static List<UserProject> findByUserIdAndRole(Long userId, Role role) {
        return list("user.id = ?1 and role = ?2", userId, role);
    }

    public static boolean userHasAccessToProject(Long userId, Long projectId) {
        return count("user.id = ?1 and project.id = ?2", userId, projectId) > 0;
    }

    public static boolean userIsModeratorOfProject(Long userId, Long projectId) {
        return count("user.id = ?1 and project.id = ?2 and role = ?3",
                     userId, projectId, Role.MODERATOR) > 0;
    }
}

package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "donations")
public class Donation extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @NotNull(message = "Valor da doação é obrigatório")
    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    @NotNull(message = "Data de cálculo é obrigatória")
    @Column(nullable = false)
    public LocalDate calculatedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public DonationStatus status = DonationStatus.PENDING;

    @Column(name = "processed_at")
    public LocalDateTime processedAt;

    @Column(length = 1000)
    public String notes;

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

    public enum DonationStatus {
        PENDING,
        PROCESSED,
        CANCELLED
    }

    // Panache Active Record methods
    public static List<Donation> findByUserId(Long userId) {
        return list("user.id", userId);
    }

    public static List<Donation> findByUserIdAndStatus(Long userId, DonationStatus status) {
        return list("user.id = ?1 and status = ?2", userId, status);
    }

    public static BigDecimal calculateTotalByUserId(Long userId) {
        return find("user.id = ?1 and status = ?2", userId, DonationStatus.PROCESSED)
                .stream()
                .map(donation -> (Donation) donation)
                .map(donation -> donation.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

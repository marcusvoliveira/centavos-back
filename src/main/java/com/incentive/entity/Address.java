package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "addresses")
public class Address extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @NotBlank(message = "CEP é obrigatório")
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    public String zipCode;

    @NotBlank(message = "Rua é obrigatória")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    public String street;

    @Size(max = 20)
    @Column(length = 20)
    public String number;

    @Size(max = 100)
    @Column(length = 100)
    public String complement;

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    public String neighborhood;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    public String city;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    public String state;

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
    public static List<Address> findByUserId(Long userId) {
        return list("user.id", userId);
    }
}

package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    public String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true, length = 150)
    public String email;

    @NotBlank(message = "CPF é obrigatório")
    @Column(nullable = false, unique = true, length = 150)
    public String cpf;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Column(nullable = false)
    public String password;

    @Size(max = 20)
    @Column(length = 20)
    public String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role = Role.USER;

    @Column(name = "email_verified")
    public boolean emailVerified = false;

    @Column(name = "verification_code", length = 6)
    public String verificationCode;

    @Column(name = "verification_code_expires_at")
    public LocalDateTime verificationCodeExpiresAt;

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
    public static Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public static Optional<User> findByEmailAndVerificationCode(String email, String code) {
        return find("email = ?1 and verificationCode = ?2", email, code).firstResultOptional();
    }

    public static boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}

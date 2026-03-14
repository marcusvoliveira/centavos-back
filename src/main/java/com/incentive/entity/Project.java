package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Size(min = 3, max = 50, message = "Slug deve ter entre 3 e 50 caracteres")
    @Column(unique = true, length = 50)
    public String slug;

    @Column(name = "start_date")
    public LocalDate startDate;

    // Theme colors
    @Column(name = "primary_color", length = 7)
    public String primaryColor = "#c41230";

    @Column(name = "secondary_color", length = 7)
    public String secondaryColor = "#ffffff";

    @Column(name = "background_color", length = 7)
    public String backgroundColor = "#f3f4f6";

    // Logo and images (URLs or base64) — stored as TEXT to support base64
    @Column(name = "logo_url", columnDefinition = "TEXT")
    public String logoUrl;

    @Column(name = "hero_image_url", columnDefinition = "TEXT")
    public String heroImageUrl;

    @Column(name = "logo_dark_url", columnDefinition = "TEXT")
    public String logoDarkUrl;

    // Hero section texts
    @Column(name = "hero_title", length = 200)
    public String heroTitle;

    @Column(name = "hero_subtitle", length = 500)
    public String heroSubtitle;

    // Navigation links
    @Column(name = "nav_link1_label", length = 60)
    public String navLink1Label;

    @Column(name = "nav_link1_url", length = 500)
    public String navLink1Url;

    @Column(name = "nav_link2_label", length = 60)
    public String navLink2Label;

    @Column(name = "nav_link2_url", length = 500)
    public String navLink2Url;

    @Column(name = "nav_link3_label", length = 60)
    public String navLink3Label;

    @Column(name = "nav_link3_url", length = 500)
    public String navLink3Url;

    // Financial
    @Column(name = "min_value", precision = 10, scale = 2)
    public BigDecimal minValue;

    // Email templates
    @Column(name = "email_boas_vindas", columnDefinition = "TEXT")
    public String emailBoasVindas;

    @Column(name = "email_aviso_cobranca", columnDefinition = "TEXT")
    public String emailAvisoCobranca;

    @Column(name = "email_cobranca", columnDefinition = "TEXT")
    public String emailCobranca;

    @Column(name = "email_extrato", columnDefinition = "TEXT")
    public String emailExtrato;

    @Column(name = "email_cancelamento", columnDefinition = "TEXT")
    public String emailCancelamento;

    // Payment
    @Column(name = "payment_type", length = 20)
    public String paymentType;

    @Column(name = "bank_code", length = 10)
    public String bankCode;

    @Column(name = "bank_agency", length = 20)
    public String bankAgency;

    @Column(name = "bank_account", length = 30)
    public String bankAccount;

    @Column(name = "bank_holder_name", length = 100)
    public String bankHolderName;

    @Column(name = "bank_holder_document", length = 20)
    public String bankHolderDocument;

    @Column(name = "pix_key", length = 100)
    public String pixKey;

    // Billing plan
    @Column(name = "plan_type", length = 20)
    public String planType;

    // Subscription payment method (how project owner pays the platform)
    @Column(name = "forma_pagamento", length = 20)
    public String formaPagamento;

    @Column(name = "forma_pagamento_pix_key", length = 100)
    public String formaPagamentoPixKey;

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

    public static Project findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static Project findActiveBySlug(String slug) {
        return find("slug = ?1 and active = true", slug).firstResult();
    }
}

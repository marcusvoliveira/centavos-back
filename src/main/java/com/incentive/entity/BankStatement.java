package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bank_statements")
public class BankStatement extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @NotNull(message = "Tipo de conta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public AccountType accountType;

    @Column(length = 100)
    public String accountDescription;

    @NotNull(message = "Data da transação é obrigatória")
    @Column(nullable = false)
    public LocalDate transactionDate;

    @Column(length = 500)
    public String description;

    @NotNull(message = "Valor é obrigatório")
    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public TransactionType transactionType;

    @Column(length = 100)
    public String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Enums
    public enum AccountType {
        CHECKING_ACCOUNT,
        SAVINGS_ACCOUNT,
        CREDIT_CARD
    }

    public enum TransactionType {
        DEBIT,
        CREDIT
    }

    // Panache Active Record methods
    public static List<BankStatement> findByUserId(Long userId) {
        return list("user.id", userId);
    }

    public static List<BankStatement> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return list("user.id = ?1 and transactionDate between ?2 and ?3", userId, startDate, endDate);
    }

    public static BigDecimal calculateTotalByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return find("user.id = ?1 and transactionDate between ?2 and ?3", userId, startDate, endDate)
                .stream()
                .map(statement -> (BankStatement) statement)
                .map(statement -> statement.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

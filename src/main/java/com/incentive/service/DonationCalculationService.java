package com.incentive.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import com.incentive.entity.BankStatement;
import com.incentive.entity.Donation;
import com.incentive.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class DonationCalculationService {

    // Percentual padrão para cálculo de doação (1%)
    private static final BigDecimal DEFAULT_DONATION_PERCENTAGE = new BigDecimal("0.01");

    /**
     * Calcula o valor a ser doado baseado nos gastos do mês
     * Regra: 1% do total de gastos (débitos) do mês
     */
    @Transactional
    public Donation calculateMonthlyDonation(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = User.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        // Buscar todos os extratos do período
        List<BankStatement> statements = BankStatement.findByUserIdAndDateRange(userId, startDate, endDate);

        // Calcular total de gastos (apenas débitos)
        BigDecimal totalExpenses = statements.stream()
                .filter(s -> s.transactionType == BankStatement.TransactionType.DEBIT)
                .map(s -> s.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular valor da doação (1% dos gastos)
        BigDecimal donationAmount = totalExpenses
                .multiply(DEFAULT_DONATION_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);

        // Criar registro de doação
        Donation donation = new Donation();
        donation.user = user;
        donation.amount = donationAmount;
        donation.calculatedDate = LocalDate.now();
        donation.status = Donation.DonationStatus.PENDING;
        donation.notes = String.format(
            "Doação calculada sobre R$ %.2f de gastos do período %s a %s",
            totalExpenses,
            startDate,
            endDate
        );

        donation.persist();

        return donation;
    }

    /**
     * Calcula doação personalizada com percentual customizado
     */
    @Transactional
    public Donation calculateCustomDonation(Long userId, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        User user = User.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        if (percentage.compareTo(BigDecimal.ZERO) <= 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentual deve estar entre 0.01 e 100");
        }

        List<BankStatement> statements = BankStatement.findByUserIdAndDateRange(userId, startDate, endDate);

        BigDecimal totalExpenses = statements.stream()
                .filter(s -> s.transactionType == BankStatement.TransactionType.DEBIT)
                .map(s -> s.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percentageDecimal = percentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal donationAmount = totalExpenses
                .multiply(percentageDecimal)
                .setScale(2, RoundingMode.HALF_UP);

        Donation donation = new Donation();
        donation.user = user;
        donation.amount = donationAmount;
        donation.calculatedDate = LocalDate.now();
        donation.status = Donation.DonationStatus.PENDING;
        donation.notes = String.format(
            "Doação calculada sobre R$ %.2f de gastos (%.2f%%) do período %s a %s",
            totalExpenses,
            percentage,
            startDate,
            endDate
        );

        donation.persist();

        return donation;
    }

    /**
     * Processa uma doação pendente
     */
    @Transactional
    public void processDonation(Long donationId) {
        Donation donation = Donation.findById(donationId);
        if (donation == null) {
            throw new IllegalArgumentException("Doação não encontrada");
        }

        if (donation.status != Donation.DonationStatus.PENDING) {
            throw new IllegalStateException("Apenas doações pendentes podem ser processadas");
        }

        donation.status = Donation.DonationStatus.PROCESSED;
        donation.processedAt = java.time.LocalDateTime.now();
        donation.persist();
    }
}

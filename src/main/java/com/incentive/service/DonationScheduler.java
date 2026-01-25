package com.incentive.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.incentive.entity.Donation;
import com.incentive.entity.User;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class DonationScheduler {

    private static final Logger LOG = Logger.getLogger(DonationScheduler.class);

    @Inject
    DonationCalculationService donationCalculationService;

    @Inject
    EmailService emailService;

    /**
     * Executa todo dia 1 às 9h da manhã para calcular doações do mês anterior
     * Cron: segundo minuto hora dia mês dia-da-semana
     */
    @Scheduled(cron = "0 0 9 1 * ?")
    @Transactional
    public void calculateMonthlyDonations() {
        LOG.info("Iniciando cálculo mensal de doações...");

        LocalDate now = LocalDate.now();
        LocalDate firstDayLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayLastMonth = now.withDayOfMonth(1).minusDays(1);

        List<User> users = User.listAll();

        for (User user : users) {
            try {
                if (user.emailVerified) {
                    Donation donation = donationCalculationService.calculateMonthlyDonation(
                            user.id,
                            firstDayLastMonth,
                            lastDayLastMonth
                    );
                    LOG.infof("Doação calculada para usuário %s: R$ %.2f", user.email, donation.amount);
                }
            } catch (Exception e) {
                LOG.errorf(e, "Erro ao calcular doação para usuário %s", user.email);
            }
        }

        LOG.info("Cálculo mensal de doações concluído");
    }

    /**
     * Executa toda segunda-feira às 10h para enviar relatório semanal
     */
    @Scheduled(cron = "0 0 10 ? * MON")
    @Transactional
    public void sendWeeklyReport() {
        LOG.info("Iniciando envio de relatórios semanais...");

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(1);

        List<User> users = User.listAll();

        for (User user : users) {
            try {
                if (user.emailVerified) {
                    List<Donation> donations = Donation.findByUserId(user.id);
                    String statement = buildWeeklyStatement(donations, startDate, endDate);
                    emailService.sendStatementEmail(user.email, user.name, statement);
                    LOG.infof("Relatório semanal enviado para %s", user.email);
                }
            } catch (Exception e) {
                LOG.errorf(e, "Erro ao enviar relatório semanal para %s", user.email);
            }
        }

        LOG.info("Envio de relatórios semanais concluído");
    }

    /**
     * Executa todo dia 5 às 10h para enviar relatório mensal
     */
    @Scheduled(cron = "0 0 10 5 * ?")
    @Transactional
    public void sendMonthlyReport() {
        LOG.info("Iniciando envio de relatórios mensais...");

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(1).minusDays(1);

        List<User> users = User.listAll();

        for (User user : users) {
            try {
                if (user.emailVerified) {
                    List<Donation> donations = Donation.findByUserId(user.id);
                    String statement = buildMonthlyStatement(donations, startDate, endDate);
                    emailService.sendStatementEmail(user.email, user.name, statement);
                    LOG.infof("Relatório mensal enviado para %s", user.email);
                }
            } catch (Exception e) {
                LOG.errorf(e, "Erro ao enviar relatório mensal para %s", user.email);
            }
        }

        LOG.info("Envio de relatórios mensais concluído");
    }

    private String buildWeeklyStatement(List<Donation> donations, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Relatório Semanal de Doações</h3>");
        sb.append("<p>Período: ").append(startDate).append(" a ").append(endDate).append("</p>");
        sb.append("<table style='width:100%; border-collapse: collapse;'>");
        sb.append("<tr style='background-color: #f2f2f2;'>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Data</th>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Valor</th>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Status</th>");
        sb.append("</tr>");

        donations.stream()
                .filter(d -> d.calculatedDate.isAfter(startDate.minusDays(1)) && d.calculatedDate.isBefore(endDate.plusDays(1)))
                .forEach(d -> {
                    sb.append("<tr>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(d.calculatedDate).append("</td>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>R$ ").append(String.format("%.2f", d.amount)).append("</td>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(d.status).append("</td>");
                    sb.append("</tr>");
                });

        sb.append("</table>");
        return sb.toString();
    }

    private String buildMonthlyStatement(List<Donation> donations, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Relatório Mensal de Doações</h3>");
        sb.append("<p>Período: ").append(startDate).append(" a ").append(endDate).append("</p>");
        sb.append("<table style='width:100%; border-collapse: collapse;'>");
        sb.append("<tr style='background-color: #f2f2f2;'>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Data</th>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Valor</th>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Status</th>");
        sb.append("<th style='border: 1px solid #ddd; padding: 8px;'>Observações</th>");
        sb.append("</tr>");

        donations.stream()
                .filter(d -> d.calculatedDate.isAfter(startDate.minusDays(1)) && d.calculatedDate.isBefore(endDate.plusDays(1)))
                .forEach(d -> {
                    sb.append("<tr>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(d.calculatedDate).append("</td>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>R$ ").append(String.format("%.2f", d.amount)).append("</td>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(d.status).append("</td>");
                    sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(d.notes != null ? d.notes : "").append("</td>");
                    sb.append("</tr>");
                });

        sb.append("</table>");

        // Calcular total
        java.math.BigDecimal total = donations.stream()
                .filter(d -> d.calculatedDate.isAfter(startDate.minusDays(1)) && d.calculatedDate.isBefore(endDate.plusDays(1)))
                .filter(d -> d.status == Donation.DonationStatus.PROCESSED)
                .map(d -> d.amount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        sb.append("<p><strong>Total de doações processadas: R$ ").append(String.format("%.2f", total)).append("</strong></p>");

        return sb.toString();
    }
}

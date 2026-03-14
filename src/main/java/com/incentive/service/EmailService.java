package com.incentive.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MailTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    @Inject
    MailTemplate emailVerification;

    @ConfigProperty(name = "email.verification.subject")
    String verificationSubject;

    @ConfigProperty(name = "email.statement.subject")
    String statementSubject;

    @ConfigProperty(name = "email.verification.greeting", defaultValue = "Bem-vindo ao Incentive!")
    String verificationGreeting;

    @ConfigProperty(name = "email.verification.instructions", defaultValue = "Use o código abaixo para verificar seu e-mail:")
    String verificationInstructions;

    @ConfigProperty(name = "email.verification.expiry", defaultValue = "Este código expira em 30 minutos.")
    String verificationExpiry;

    @ConfigProperty(name = "email.verification.footer", defaultValue = "Se você não solicitou este cadastro, por favor ignore este e-mail.")
    String verificationFooter;

    public void sendVerificationEmail(String to, String name, String code, String logoUrl) {
        emailVerification.to(to)
                .subject(verificationSubject)
                .data("name", name)
                .data("code", code)
                .data("logoBase64", logoUrl != null ? logoUrl : "")
                .data("greeting", verificationGreeting)
                .data("instructions", verificationInstructions)
                .data("expiry", verificationExpiry)
                .data("footer", verificationFooter)
                .send()
                .subscribe().with(
                        ok -> {},
                        err -> System.err.println("[EmailService] Erro ao enviar e-mail de verificação para " + to + ": " + err.getMessage())
                );
    }

    public void sendStatementEmail(String to, String name, String statement) {
        String body = buildStatementEmailBody(name, statement);
        mailer.send(Mail.withHtml(to, statementSubject, body));
    }

    public void sendInviteEmail(String to, String name, String inviteLink) {
        String body = buildInviteEmailBody(name, inviteLink);
        mailer.send(Mail.withHtml(to, "Você foi convidado - Incentive", body));
    }

    private String buildStatementEmailBody(String name, String statement) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2>Olá, %s!</h2>
                        <p>Segue abaixo seu extrato de doações:</p>
                        <div style="background-color: #f4f4f4; padding: 15px; margin: 20px 0;">
                            %s
                        </div>
                        <p>Obrigado por fazer parte do Incentive!</p>
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                        <p style="color: #777; font-size: 12px;">Incentive - Sistema de Doações</p>
                    </div>
                </body>
                </html>
                """.formatted(name, statement);
    }

    private String buildInviteEmailBody(String name, String inviteLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2>Olá, %s!</h2>
                        <p>Você foi convidado para acessar a plataforma Incentive.</p>
                        <p>Clique no link abaixo para completar seu cadastro:</p>
                        <div style="margin: 20px 0;">
                            <a href="%s" style="background-color: #c41230; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">
                                Completar Cadastro
                            </a>
                        </div>
                        <p>Ou copie e cole o link abaixo no seu navegador:</p>
                        <p style="word-break: break-all; color: #555;">%s</p>
                        <p>Este link expira em 7 dias.</p>
                        <p>Se você não esperava este convite, por favor ignore este email.</p>
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                        <p style="color: #777; font-size: 12px;">Incentive - Sistema de Doações</p>
                    </div>
                </body>
                </html>
                """.formatted(name, inviteLink, inviteLink);
    }
}

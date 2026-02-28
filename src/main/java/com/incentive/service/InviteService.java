package com.incentive.service;

import com.incentive.dto.InviteInfoResponse;
import com.incentive.dto.InviteUserRequest;
import com.incentive.entity.Project;
import com.incentive.entity.Role;
import com.incentive.entity.User;
import com.incentive.entity.UserProject;
import com.incentive.util.EmailVerificationCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@ApplicationScoped
public class InviteService {

    @Inject
    EmailService emailService;

    @Inject
    ProjectHashService projectHashService;

    @Inject
    EmailVerificationCache emailVerificationCache;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    @Transactional
    public void inviteUser(InviteUserRequest request, Long inviterUserId) {
        if (User.existsByEmail(request.email)) {
            throw new WebApplicationException("E-mail já cadastrado", Response.Status.CONFLICT);
        }

        String inviteToken = generateSecureToken();

        User user = new User();
        user.name = request.name;
        user.email = request.email;
        user.role = Role.MODERATOR;
        user.password = null;
        user.emailVerified = false;
        user.inviteToken = inviteToken;
        user.inviteTokenExpiresAt = LocalDateTime.now().plusDays(7);
        user.persist();

        String projectHash = null;
        if (request.projectId != null) {
            Project project = Project.findById(request.projectId);
            if (project != null) {
                UserProject up = new UserProject();
                up.user = user;
                up.project = project;
                up.role = Role.MODERATOR;
                up.persist();
                projectHash = projectHashService.encryptProjectId(project.id);
            }
        }

        if (projectHash == null) {
            projectHash = projectHashService.encryptProjectId(1L);
        }

        String inviteLink = frontendUrl + "/" + projectHash + "/completar-cadastro/" + inviteToken;
        emailService.sendInviteEmail(request.email, request.name, inviteLink);
    }

    public InviteInfoResponse getInviteInfo(String token) {
        User user = User.findByInviteToken(token)
                .orElseThrow(() -> new WebApplicationException("Convite inválido ou expirado", Response.Status.NOT_FOUND));

        if (user.inviteTokenExpiresAt == null || LocalDateTime.now().isAfter(user.inviteTokenExpiresAt)) {
            throw new WebApplicationException("Convite expirado", Response.Status.GONE);
        }

        InviteInfoResponse resp = new InviteInfoResponse();
        resp.email = user.email;
        resp.nome = user.name;
        return resp;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

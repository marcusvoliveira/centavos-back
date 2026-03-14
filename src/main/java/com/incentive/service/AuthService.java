package com.incentive.service;

import com.incentive.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import com.incentive.entity.Project;
import com.incentive.entity.Role;
import com.incentive.entity.User;
import com.incentive.entity.UserProject;
import com.incentive.security.TokenService;
import com.incentive.service.ProjectHashService;
import com.incentive.util.EmailVerificationCache;
import com.incentive.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {

    @Inject
    TokenService tokenService;

    @Inject
    EmailService emailService;

    @Inject
    ProjectHashService projectHashService;

    @Inject
    EmailVerificationCache emailVerificationCache;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verificar se email já existe
        if (User.existsByEmail(request.email)) {
            throw new BadRequestException("Email já cadastrado");
        }

        // Criar novo usuário
        User user = new User();
        user.name = request.name;
        user.email = request.email;
        user.password = PasswordUtil.hashPassword(request.password);
        user.phone = request.phone;
        user.role = Role.USER;
        user.emailVerified = true; // e-mail já verificado via formulário

        user.persist();

        // Vincular ao projeto se projectHash fornecido
        if (request.projectHash != null && !request.projectHash.isBlank()) {
            try {
                Long projectId = projectHashService.decryptProjectHash(request.projectHash);
                Project project = Project.findById(projectId);
                if (project != null) {
                    UserProject up = new UserProject();
                    up.user = user;
                    up.project = project;
                    up.role = Role.USER;
                    up.persist();
                }
            } catch (Exception ignored) {
                // hash inválido — continua sem vincular
            }
        }

        // Gerar token
        String token = tokenService.generateToken(user);

        // Buscar projetos do usuário
        List<UserProjectDTO> projects = getUserProjects(user.id);

        return new AuthResponse(token, UserDTO.from(user), projects);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = User.findByEmail(request.email)
                .orElseThrow(() -> new NotAuthorizedException("Email ou senha inválidos"));

        if (!PasswordUtil.verifyPassword(request.password, user.password)) {
            throw new NotAuthorizedException("Email ou senha inválidos");
        }

        String token = tokenService.generateToken(user);

        // Buscar projetos do usuário
        List<UserProjectDTO> projects = getUserProjects(user.id);

        return new AuthResponse(token, UserDTO.from(user), projects);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = User.findByEmailAndVerificationCode(request.email, request.code)
                .orElseThrow(() -> new BadRequestException("Código de verificação inválido"));

        if (user.verificationCodeExpiresAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Código de verificação expirado");
        }

        user.emailVerified = true;
        user.verificationCode = null;
        user.verificationCodeExpiresAt = null;
        user.persist();
    }

    @Transactional
    public void resendVerificationCode(String email) {
        User user = User.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (user.emailVerified) {
            throw new BadRequestException("Email já verificado");
        }

        // Gerar novo código
        String verificationCode = generateVerificationCode();
        user.verificationCode = verificationCode;
        user.verificationCodeExpiresAt = LocalDateTime.now().plusHours(24);
        user.persist();

        // Enviar email
        emailService.sendVerificationEmail(user.email, user.name, verificationCode, null);
    }

    public void sendAdminEmailCode(String email, String name) {
        String code = generateVerificationCode();
        emailVerificationCache.store(email, code);
        emailService.sendVerificationEmail(email, name, code, null);
    }

    public boolean verifyAdminEmailCode(String email, String code) {
        return emailVerificationCache.verify(email, code);
    }

    @Transactional
    public AuthResponse completeRegistration(CompleteRegistrationRequest request) {
        User user = User.findByInviteToken(request.token)
                .orElseThrow(() -> new WebApplicationException("Token inválido", Response.Status.BAD_REQUEST));

        if (user.inviteTokenExpiresAt == null || LocalDateTime.now().isAfter(user.inviteTokenExpiresAt)) {
            throw new WebApplicationException("Token expirado", Response.Status.BAD_REQUEST);
        }

        if (request.nome != null && !request.nome.isBlank()) {
            user.name = request.nome;
        }
        user.email = request.email;
        user.password = PasswordUtil.hashPassword(request.senha);
        user.phone = request.telefone;
        user.emailVerified = true;
        user.inviteToken = null;
        user.inviteTokenExpiresAt = null;
        user.persist();

        String token = tokenService.generateToken(user);

        List<UserProjectDTO> projects = getUserProjects(user.id);

        return new AuthResponse(token, UserDTO.from(user), projects);
    }

    public void sendEmailCode(String email, String projectHash) {
        String code = generateVerificationCode();
        emailVerificationCache.store(email, code);

        String logoUrl = null;
        if (projectHash != null && !projectHash.isBlank()) {
            try {
                Long projectId = projectHashService.decryptProjectHash(projectHash);
                Project project = Project.findById(projectId);
                if (project != null) {
                    logoUrl = project.logoUrl;
                }
            } catch (Exception ignored) {
                // hash inválido — continua sem logo
            }
        }

        emailService.sendVerificationEmail(email, email, code, logoUrl);
    }

    public boolean verifyEmailCode(String email, String code) {
        return emailVerificationCache.verify(email, code);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    private List<UserProjectDTO> getUserProjects(Long userId) {
        return UserProject.findByUserId(userId).stream()
                .map(up -> UserProjectDTO.from(up, projectHashService))
                .collect(Collectors.toList());
    }
}

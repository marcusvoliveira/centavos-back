package com.incentive.service;

import com.incentive.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import com.incentive.entity.Role;
import com.incentive.entity.User;
import com.incentive.entity.UserProject;
import com.incentive.security.TokenService;
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
        user.emailVerified = false;

        // Gerar código de verificação
        String verificationCode = generateVerificationCode();
        user.verificationCode = verificationCode;
        user.verificationCodeExpiresAt = LocalDateTime.now().plusHours(24);

        user.persist();

        // Enviar email de verificação
        emailService.sendVerificationEmail(user.email, user.name, verificationCode);

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
        emailService.sendVerificationEmail(user.email, user.name, verificationCode);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    private List<UserProjectDTO> getUserProjects(Long userId) {
        return UserProject.findByUserId(userId).stream()
                .map(UserProjectDTO::from)
                .collect(Collectors.toList());
    }
}

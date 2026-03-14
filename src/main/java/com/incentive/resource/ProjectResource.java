package com.incentive.resource;

import com.incentive.dto.UserProjectDTO;
import com.incentive.entity.Project;
import com.incentive.entity.Role;
import com.incentive.entity.User;
import com.incentive.entity.UserProject;
import com.incentive.service.ProjectHashService;
import com.incentive.util.CardEncryptionService;
import com.incentive.util.PasswordUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    ProjectHashService projectHashService;

    @Inject
    CardEncryptionService cardEncryptionService;

    @GET
    @RolesAllowed({"USER", "MODERATOR", "AGENT", "ADMIN"})
    public List<ProjectDTO> listProjects() {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        // ADMIN vê todos os projetos
        if ("ADMIN".equals(userRole)) {
            return Project.<Project>listAll().stream()
                    .map(project -> new ProjectDTO().from(project))
                    .collect(Collectors.toList());
        }

        // Outros usuários veem apenas seus projetos
        return UserProject.findByUserId(userId).stream()
                .map(up -> new ProjectDTO().from(up.project))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"USER", "MODERATOR", "AGENT", "ADMIN"})
    public Response getProject(@PathParam("id") Long projectId) {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = Project.findById(projectId);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // ADMIN pode ver qualquer projeto
        if (!"ADMIN".equals(userRole)) {
            // Verificar se o usuário tem acesso ao projeto
            if (!UserProject.userHasAccessToProject(userId, projectId)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }

        return Response.ok(new ProjectDTO().from(project)).build();
    }

    @POST
    @RolesAllowed({"ADMIN", "MODERATOR"})
    @Transactional
    public Response createProject(CreateProjectRequest request) {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = new Project();
        project.name = request.name;
        project.description = request.description;
        project.active = true;

        if (request.startDate != null && !request.startDate.isBlank()) {
            project.startDate = LocalDate.parse(request.startDate);
        }

        // Theme
        if (request.primaryColor != null) project.primaryColor = request.primaryColor;
        if (request.secondaryColor != null) project.secondaryColor = request.secondaryColor;
        if (request.backgroundColor != null) project.backgroundColor = request.backgroundColor;
        if (request.logoUrl != null) project.logoUrl = request.logoUrl;
        if (request.heroImageUrl != null) project.heroImageUrl = request.heroImageUrl;
        if (request.logoDarkUrl != null) project.logoDarkUrl = request.logoDarkUrl;
        if (request.heroTitle != null) project.heroTitle = request.heroTitle;
        if (request.heroSubtitle != null) project.heroSubtitle = request.heroSubtitle;
        project.navLink1Label = request.navLink1Label;
        project.navLink1Url = request.navLink1Url;
        project.navLink2Label = request.navLink2Label;
        project.navLink2Url = request.navLink2Url;
        project.navLink3Label = request.navLink3Label;
        project.navLink3Url = request.navLink3Url;

        // Emails
        project.emailBoasVindas = request.emailBoasVindas;
        project.emailAvisoCobranca = request.emailAvisoCobranca;
        project.emailCobranca = request.emailCobranca;
        project.emailExtrato = request.emailExtrato;
        project.emailCancelamento = request.emailCancelamento;

        // Financial
        if (request.minValue != null) project.minValue = BigDecimal.valueOf(request.minValue);

        // Payment
        project.paymentType = request.paymentType;
        project.bankCode = request.bankCode;
        project.bankAgency = request.bankAgency;
        project.bankAccount = request.bankAccount;
        project.bankHolderName = request.bankHolderName;
        project.bankHolderDocument = request.bankHolderDocument;
        project.pixKey = request.pixKey;

        // Plan
        project.planType = request.planType;

        // Forma de pagamento da plataforma (opcional)
        if (request.formaPagamento != null) {
            project.formaPagamento = request.formaPagamento;
            if ("PIX".equals(request.formaPagamento)) {
                project.formaPagamentoPixKey = request.formaPagamentoPixKey;
            } else if ("CREDIT_CARD".equals(request.formaPagamento)
                    && request.cardNumberEncrypted != null && !request.cardNumberEncrypted.isBlank()) {
                // Descriptografar para uso em outro processo (ex: gateway de pagamento)
                // Dados do cartão NÃO são persistidos no BD
                String cardNumber = cardEncryptionService.decrypt(request.cardNumberEncrypted);
                String cvv = (request.cardCvvEncrypted != null && !request.cardCvvEncrypted.isBlank())
                        ? cardEncryptionService.decrypt(request.cardCvvEncrypted) : null;
                // TODO: encaminhar cardNumber, cvv, request.cardHolder, request.cardExpiry ao gateway
                // Por ora: dados descriptografados disponíveis mas NÃO persistidos
            }
        }

        project.persist();

        // Criar associação do criador como MODERATOR (exceto se for ADMIN criando)
        UserProject creatorProject = new UserProject();
        creatorProject.user = User.findById(userId);
        creatorProject.project = project;
        creatorProject.role = "ADMIN".equals(userRole) ? Role.ADMIN : Role.MODERATOR;
        creatorProject.persist();

        // Processar agentes
        if (request.agents != null) {
            for (AgentRequest agentReq : request.agents) {
                User agentUser = null;

                // Buscar agente por ID se fornecido, ou por email
                if (agentReq.userId != null) {
                    agentUser = User.findById(agentReq.userId);
                }
                if (agentUser == null && agentReq.email != null && !agentReq.email.isBlank()) {
                    agentUser = User.findByEmail(agentReq.email).orElse(null);
                }

                // Criar novo usuário AGENT se não encontrado
                if (agentUser == null && agentReq.email != null && !agentReq.email.isBlank()) {
                    agentUser = new User();
                    agentUser.email = agentReq.email;
                    agentUser.name = agentReq.nome != null ? agentReq.nome : agentReq.email;
                    agentUser.cpf = (agentReq.cpf != null && !agentReq.cpf.isBlank()) ? agentReq.cpf : null;
                    agentUser.phone = agentReq.telefone;
                    agentUser.pixKey = agentReq.chavePix;
                    agentUser.role = Role.AGENT;
                    agentUser.password = null;
                    agentUser.emailVerified = false;
                    agentUser.persist();
                } else if (agentUser != null) {
                    // Atualizar dados se fornecidos
                    boolean changed = false;
                    if (agentReq.nome != null && !agentReq.nome.isBlank()) {
                        agentUser.name = agentReq.nome;
                        changed = true;
                    }
                    if (agentReq.telefone != null && !agentReq.telefone.isBlank()) {
                        agentUser.phone = agentReq.telefone;
                        changed = true;
                    }
                    if (agentReq.chavePix != null && !agentReq.chavePix.isBlank()) {
                        agentUser.pixKey = agentReq.chavePix;
                        changed = true;
                    }
                    if (changed) agentUser.persist();
                }

                if (agentUser != null && !UserProject.userHasAccessToProject(agentUser.id, project.id)) {
                    UserProject agentProject = new UserProject();
                    agentProject.user = agentUser;
                    agentProject.project = project;
                    agentProject.role = Role.AGENT;
                    if (agentReq.participacao != null) {
                        agentProject.participation = BigDecimal.valueOf(agentReq.participacao);
                    }
                    agentProject.persist();
                }
            }
        }

        // Processar responsável do projeto
        if (request.responsavelEmail != null && !request.responsavelEmail.isBlank()) {
            User responsavel = User.findByEmail(request.responsavelEmail).orElse(null);
            if (responsavel == null) {
                responsavel = new User();
                responsavel.name = request.responsavelNome;
                responsavel.email = request.responsavelEmail;
                responsavel.password = request.responsavelSenha != null
                        ? PasswordUtil.hashPassword(request.responsavelSenha) : null;
                responsavel.phone = request.responsavelTelefone;
                responsavel.cpf = (request.responsavelCpf != null && !request.responsavelCpf.isBlank())
                        ? request.responsavelCpf : null;
                responsavel.role = Role.MODERATOR;
                responsavel.emailVerified = true;
                responsavel.persist();
            }
            if (!UserProject.userHasAccessToProject(responsavel.id, project.id)) {
                UserProject rp = new UserProject();
                rp.user = responsavel;
                rp.project = project;
                rp.role = Role.MODERATOR;
                rp.persist();
            }
        }

        ProjectDTO dto = new ProjectDTO();
        return Response.status(Response.Status.CREATED)
                .entity(dto.from(project))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "MODERATOR"})
    @Transactional
    public Response updateProject(@PathParam("id") Long projectId, UpdateProjectRequest request) {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = Project.findById(projectId);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar permissão
        if (!"ADMIN".equals(userRole) && !UserProject.userIsModeratorOfProject(userId, projectId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Apenas ADMIN ou MODERATOR do projeto podem atualizar\"}")
                    .build();
        }

        if (request.name != null) {
            project.name = request.name;
        }
        if (request.description != null) {
            project.description = request.description;
        }
        if (request.active != null) {
            project.active = request.active;
        }

        project.persist();

        return Response.ok(new ProjectDTO().from(project)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "MODERATOR"})
    @Transactional
    public Response deleteProject(@PathParam("id") Long projectId) {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = Project.findById(projectId);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar permissão
        if (!"ADMIN".equals(userRole) && !UserProject.userIsModeratorOfProject(userId, projectId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Apenas ADMIN ou MODERATOR do projeto podem deletar\"}")
                    .build();
        }

        project.delete();

        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/users")
    @RolesAllowed({"USER", "MODERATOR", "AGENT", "ADMIN"})
    public Response listProjectUsers(@PathParam("id") Long projectId) {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = Project.findById(projectId);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar acesso
        if (!"ADMIN".equals(userRole) && !UserProject.userHasAccessToProject(userId, projectId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<UserProjectDTO> users = UserProject.findByProjectId(projectId).stream()
                .map(up -> UserProjectDTO.from(up, projectHashService))
                .collect(Collectors.toList());

        return Response.ok(users).build();
    }

    @POST
    @Path("/{id}/users")
    @RolesAllowed({"ADMIN", "MODERATOR"})
    @Transactional
    public Response addUserToProject(@PathParam("id") Long projectId, AddUserToProjectRequest addRequest) {
        Long currentUserId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        Project project = Project.findById(projectId);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Projeto não encontrado\"}")
                    .build();
        }

        // Verificar permissão
        if (!"ADMIN".equals(userRole) && !UserProject.userIsModeratorOfProject(currentUserId, projectId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Apenas ADMIN ou MODERATOR do projeto podem adicionar usuários\"}")
                    .build();
        }

        User user = User.findById(addRequest.userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Usuário não encontrado\"}")
                    .build();
        }

        // Verificar se já existe
        if (UserProject.userHasAccessToProject(addRequest.userId, projectId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Usuário já está no projeto\"}")
                    .build();
        }

        UserProject userProject = new UserProject();
        userProject.user = user;
        userProject.project = project;
        userProject.role = addRequest.role != null ? addRequest.role : Role.USER;
        userProject.persist();

        return Response.status(Response.Status.CREATED)
                .entity("{\"message\": \"Usuário adicionado ao projeto\"}")
                .build();
    }

    @DELETE
    @Path("/{projectId}/users/{userId}")
    @RolesAllowed({"ADMIN", "MODERATOR"})
    @Transactional
    public Response removeUserFromProject(@PathParam("projectId") Long projectId, @PathParam("userId") Long userId) {
        Long currentUserId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        // Verificar permissão
        if (!"ADMIN".equals(userRole) && !UserProject.userIsModeratorOfProject(currentUserId, projectId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Apenas ADMIN ou MODERATOR do projeto podem remover usuários\"}")
                    .build();
        }

        UserProject userProject = UserProject.findByUserAndProject(userId, projectId)
                .orElse(null);

        if (userProject == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Usuário não está no projeto\"}")
                    .build();
        }

        userProject.delete();

        return Response.noContent().build();
    }

    @GET
    @Path("/by-hash/{hash}")
    @PermitAll
    public Response getProjectByHash(@PathParam("hash") String hash) {
        try {
            Long projectId = projectHashService.decryptProjectHash(hash);
            Project project = Project.findById(projectId);

            if (project == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Projeto não encontrado\"}")
                        .build();
            }

            if (!project.active) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\": \"Projeto inativo\"}")
                        .build();
            }

            PublicProjectDTO dto = new PublicProjectDTO();
            dto.hash = hash;
            dto.name = project.name;
            dto.description = project.description;
            dto.primaryColor = project.primaryColor;
            dto.secondaryColor = project.secondaryColor;
            dto.backgroundColor = project.backgroundColor;
            dto.logoUrl = project.logoUrl;
            dto.heroImageUrl = project.heroImageUrl;
            dto.logoDarkUrl = project.logoDarkUrl;
            dto.heroTitle = project.heroTitle;
            dto.heroSubtitle = project.heroSubtitle;
            dto.navLink1Label = project.navLink1Label;
            dto.navLink1Url = project.navLink1Url;
            dto.navLink2Label = project.navLink2Label;
            dto.navLink2Url = project.navLink2Url;
            dto.navLink3Label = project.navLink3Label;
            dto.navLink3Url = project.navLink3Url;

            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Hash inválido\"}")
                    .build();
        }
    }

    // DTOs
    public class ProjectDTO {
        public Long id;
        public String name;
        public String description;
        public boolean active;
        public String hash;
        public String startDate;
        public String responsavelNome;

        public ProjectDTO from(Project project) {
            ProjectDTO dto = new ProjectDTO();
            dto.id = project.id;
            dto.name = project.name;
            dto.description = project.description;
            dto.active = project.active;
            dto.hash = projectHashService.encryptProjectId(project.id);
            dto.startDate = project.startDate != null ? project.startDate.toString() : null;
            UserProject mod = UserProject.findByProjectId(project.id).stream()
                    .filter(up -> up.role == Role.MODERATOR || up.role == Role.ADMIN)
                    .findFirst().orElse(null);
            dto.responsavelNome = (mod != null && mod.user != null) ? mod.user.name : null;
            return dto;
        }
    }

    public static class PublicProjectDTO {
        public String hash;
        public String name;
        public String description;
        public String primaryColor;
        public String secondaryColor;
        public String backgroundColor;
        public String logoUrl;
        public String heroImageUrl;
        public String logoDarkUrl;
        public String heroTitle;
        public String heroSubtitle;
        public String navLink1Label;
        public String navLink1Url;
        public String navLink2Label;
        public String navLink2Url;
        public String navLink3Label;
        public String navLink3Url;
    }

    public static class CreateProjectRequest {
        public String name;
        public String description;
        public String startDate;
        // Personalização
        public String primaryColor;
        public String secondaryColor;
        public String backgroundColor;
        public String logoUrl;
        public String heroImageUrl;
        public String logoDarkUrl;
        public String heroTitle;
        public String heroSubtitle;
        public String navLink1Label;
        public String navLink1Url;
        public String navLink2Label;
        public String navLink2Url;
        public String navLink3Label;
        public String navLink3Url;
        // Emails
        public String emailBoasVindas;
        public String emailAvisoCobranca;
        public String emailCobranca;
        public String emailExtrato;
        public String emailCancelamento;
        // Responsável
        public String responsavelNome;
        public String responsavelEmail;
        public String responsavelSenha;
        public String responsavelTelefone;
        public String responsavelCpf;
        // Configurações
        public Double minValue;
        public List<AgentRequest> agents;
        // Plano de cobrança
        public String planType;
        // Pagamento
        public String paymentType;
        public String bankCode;
        public String bankAgency;
        public String bankAccount;
        public String bankHolderName;
        public String bankHolderDocument;
        public String pixKey;
        // Forma de pagamento da plataforma (subscription)
        public String formaPagamento;        // 'PIX' | 'CREDIT_CARD'
        public String formaPagamentoPixKey;  // se PIX
        public String cardHolder;            // se cartão — NÃO gravado no BD
        public String cardNumberEncrypted;   // se cartão — criptografado AES-GCM, NÃO gravado no BD
        public String cardExpiry;            // se cartão — NÃO gravado no BD
        public String cardCvvEncrypted;      // se cartão — criptografado AES-GCM, NÃO gravado no BD
    }

    public static class AgentRequest {
        public Long userId;
        public String email;
        public String nome;
        public String cpf;
        public String telefone;
        public Double participacao;
        public String chavePix;
    }

    public static class UpdateProjectRequest {
        public String name;
        public String description;
        public Boolean active;
    }

    public static class AddUserToProjectRequest {
        public Long userId;
        public Role role;
    }
}

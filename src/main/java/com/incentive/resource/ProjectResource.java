package com.incentive.resource;

import com.incentive.dto.UserProjectDTO;
import com.incentive.entity.Project;
import com.incentive.entity.Role;
import com.incentive.entity.User;
import com.incentive.entity.UserProject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProjectResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed({"USER", "MODERATOR", "AGENT", "ADMIN"})
    public List<ProjectDTO> listProjects() {
        Long userId = Long.parseLong(jwt.getClaim("userId").toString());
        String userRole = jwt.getClaim("role").toString();

        // ADMIN vê todos os projetos
        if ("ADMIN".equals(userRole)) {
            return Project.<Project>listAll().stream()
                    .map(ProjectDTO::from)
                    .collect(Collectors.toList());
        }

        // Outros usuários veem apenas seus projetos
        return UserProject.findByUserId(userId).stream()
                .map(up -> ProjectDTO.from(up.project))
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

        return Response.ok(ProjectDTO.from(project)).build();
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
        project.persist();

        // Criar associação do criador como MODERATOR (exceto se for ADMIN criando)
        UserProject userProject = new UserProject();
        userProject.user = User.findById(userId);
        userProject.project = project;
        userProject.role = "ADMIN".equals(userRole) ? Role.ADMIN : Role.MODERATOR;
        userProject.persist();

        return Response.status(Response.Status.CREATED)
                .entity(ProjectDTO.from(project))
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

        return Response.ok(ProjectDTO.from(project)).build();
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
                .map(up -> new UserProjectDTO(
                        up.project.id,
                        up.project.name,
                        up.project.description,
                        up.role,
                        up.project.active
                ))
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

    // DTOs
    public static class ProjectDTO {
        public Long id;
        public String name;
        public String description;
        public boolean active;

        public static ProjectDTO from(Project project) {
            ProjectDTO dto = new ProjectDTO();
            dto.id = project.id;
            dto.name = project.name;
            dto.description = project.description;
            dto.active = project.active;
            return dto;
        }
    }

    public static class CreateProjectRequest {
        public String name;
        public String description;
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

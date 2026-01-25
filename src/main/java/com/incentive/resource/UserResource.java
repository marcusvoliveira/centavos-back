package com.incentive.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.incentive.dto.UserDTO;
import com.incentive.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class UserResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/me")
    public Response getCurrentUser() {
        Long userId = jwt.getClaim("userId");
        User user = User.findById(userId);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }

    @PUT
    @Path("/me")
    @Transactional
    public Response updateCurrentUser(UserDTO userDTO) {
        Long userId = jwt.getClaim("userId");
        User user = User.findById(userId);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Atualizar apenas campos permitidos
        if (userDTO.name != null) {
            user.name = userDTO.name;
        }
        if (userDTO.phone != null) {
            user.phone = userDTO.phone;
        }

        user.persist();

        return Response.ok(UserDTO.from(user)).build();
    }

    @GET
    @RolesAllowed("ADMIN")
    public Response getAllUsers() {
        List<User> users = User.listAll();
        List<UserDTO> userDTOs = users.stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
        return Response.ok(userDTOs).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response getUser(@PathParam("id") Long id) {
        User user = User.findById(id);

        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }
}

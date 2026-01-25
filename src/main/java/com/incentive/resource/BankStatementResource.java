package com.incentive.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.incentive.entity.BankStatement;
import com.incentive.entity.User;

import java.time.LocalDate;
import java.util.List;

@Path("/api/bank-statements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class BankStatementResource {

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getMyStatements(@QueryParam("startDate") String startDate,
                                   @QueryParam("endDate") String endDate) {
        Long userId = jwt.getClaim("userId");

        if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            List<BankStatement> statements = BankStatement.findByUserIdAndDateRange(userId, start, end);
            return Response.ok(statements).build();
        }

        List<BankStatement> statements = BankStatement.findByUserId(userId);
        return Response.ok(statements).build();
    }

    @GET
    @Path("/{id}")
    public Response getStatement(@PathParam("id") Long id) {
        Long userId = jwt.getClaim("userId");
        BankStatement statement = BankStatement.findById(id);

        if (statement == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar se o extrato pertence ao usuário
        if (!statement.user.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(statement).build();
    }

    @POST
    @Transactional
    public Response createStatement(@Valid BankStatement statement) {
        Long userId = jwt.getClaim("userId");
        User user = User.findById(userId);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        statement.user = user;
        statement.persist();

        return Response.status(Response.Status.CREATED).entity(statement).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteStatement(@PathParam("id") Long id) {
        Long userId = jwt.getClaim("userId");
        BankStatement statement = BankStatement.findById(id);

        if (statement == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!statement.user.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        statement.delete();
        return Response.noContent().build();
    }

    @GET
    @Path("/admin/all")
    @RolesAllowed("ADMIN")
    public Response getAllStatements() {
        List<BankStatement> statements = BankStatement.listAll();
        return Response.ok(statements).build();
    }
}

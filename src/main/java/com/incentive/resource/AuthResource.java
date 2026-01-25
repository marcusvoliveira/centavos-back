package com.incentive.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.incentive.dto.AuthResponse;
import com.incentive.dto.LoginRequest;
import com.incentive.dto.RegisterRequest;
import com.incentive.dto.VerifyEmailRequest;
import com.incentive.service.AuthService;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/verify-email")
    public Response verifyEmail(@Valid VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return Response.ok().entity("{\"message\": \"Email verificado com sucesso\"}").build();
    }

    @POST
    @Path("/resend-verification")
    public Response resendVerification(@QueryParam("email") String email) {
        if (email == null || email.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Email é obrigatório\"}")
                    .build();
        }
        authService.resendVerificationCode(email);
        return Response.ok().entity("{\"message\": \"Código de verificação reenviado\"}").build();
    }
}

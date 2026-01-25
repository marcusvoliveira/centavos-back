package com.incentive.resource;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    @GET
    @Path("/hash")
    public Response generateHash(@QueryParam("password") String password) {
        if (password == null || password.isEmpty()) {
            return Response.status(400)
                    .entity("{\"error\": \"Provide password query parameter\"}")
                    .build();
        }

        String hash = BcryptUtil.bcryptHash(password);

        return Response.ok()
                .entity("{\"password\": \"" + password + "\", \"hash\": \"" + hash + "\"}")
                .build();
    }

    @GET
    @Path("/verify")
    public Response verifyHash(@QueryParam("password") String password,
                              @QueryParam("hash") String hash) {
        if (password == null || hash == null) {
            return Response.status(400)
                    .entity("{\"error\": \"Provide both password and hash parameters\"}")
                    .build();
        }

        boolean matches = BcryptUtil.matches(password, hash);

        return Response.ok()
                .entity("{\"password\": \"" + password + "\", \"hash\": \"" + hash + "\", \"matches\": " + matches + "}")
                .build();
    }
}

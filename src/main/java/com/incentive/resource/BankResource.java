package com.incentive.resource;

import com.incentive.entity.Bank;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/banks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BankResource {

    @GET
    @PermitAll
    public Response listBanks() {
        List<Bank> banks = Bank.findAllOrderedByName();
        return Response.ok(banks).build();
    }
}

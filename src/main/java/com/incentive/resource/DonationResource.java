package com.incentive.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import com.incentive.entity.Donation;
import com.incentive.service.DonationCalculationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/api/donations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class DonationResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    DonationCalculationService donationCalculationService;

    @GET
    public Response getMyDonations() {
        Long userId = jwt.getClaim("userId");
        List<Donation> donations = Donation.findByUserId(userId);
        return Response.ok(donations).build();
    }

    @GET
    @Path("/{id}")
    public Response getDonation(@PathParam("id") Long id) {
        Long userId = jwt.getClaim("userId");
        Donation donation = Donation.findById(id);

        if (donation == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!donation.user.id.equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(donation).build();
    }

    @POST
    @Path("/calculate")
    @Transactional
    public Response calculateDonation(@QueryParam("startDate") String startDate,
                                     @QueryParam("endDate") String endDate,
                                     @QueryParam("percentage") BigDecimal percentage) {
        Long userId = jwt.getClaim("userId");
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Donation donation;
        if (percentage != null) {
            donation = donationCalculationService.calculateCustomDonation(userId, start, end, percentage);
        } else {
            donation = donationCalculationService.calculateMonthlyDonation(userId, start, end);
        }

        return Response.status(Response.Status.CREATED).entity(donation).build();
    }

    @POST
    @Path("/{id}/process")
    @RolesAllowed("ADMIN")
    @Transactional
    public Response processDonation(@PathParam("id") Long id) {
        donationCalculationService.processDonation(id);
        Donation donation = Donation.findById(id);
        return Response.ok(donation).build();
    }

    @GET
    @Path("/total")
    public Response getMyTotalDonations() {
        Long userId = jwt.getClaim("userId");
        BigDecimal total = Donation.calculateTotalByUserId(userId);
        return Response.ok().entity("{\"total\": " + total + "}").build();
    }

    @GET
    @Path("/admin/all")
    @RolesAllowed("ADMIN")
    public Response getAllDonations() {
        List<Donation> donations = Donation.listAll();
        return Response.ok(donations).build();
    }
}

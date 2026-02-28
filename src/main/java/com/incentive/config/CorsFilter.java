package com.incentive.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import java.util.Set;

@ApplicationScoped
public class CorsFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:5173",
            "http://localhost:3000"
    );

    @ServerRequestFilter(preMatching = true)
    public Response handlePreflight(ContainerRequestContext requestContext) {
        String origin = requestContext.getHeaderString("Origin");
        if ("OPTIONS".equals(requestContext.getMethod()) && isAllowedOrigin(origin)) {
            return Response.ok()
                    .header("Access-Control-Allow-Origin", origin)
                    .header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS,HEAD,PATCH")
                    .header("Access-Control-Allow-Headers", "Content-Disposition,Origin,Accept,X-Requested-With,Content-Type,Authorization")
                    .header("Access-Control-Max-Age", "86400")
                    .build();
        }
        return null;
    }

    @ServerResponseFilter
    public void addCorsHeaders(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("Origin");
        if (isAllowedOrigin(origin)) {
            responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS,HEAD,PATCH");
            responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Disposition,Origin,Accept,X-Requested-With,Content-Type,Authorization");
            responseContext.getHeaders().putSingle("Access-Control-Expose-Headers", "Content-Disposition,Authorization");
        }
    }

    private boolean isAllowedOrigin(String origin) {
        return origin != null && ALLOWED_ORIGINS.contains(origin);
    }
}

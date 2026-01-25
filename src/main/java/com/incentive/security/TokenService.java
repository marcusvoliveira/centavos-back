package com.incentive.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.incentive.entity.User;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.duration", defaultValue = "86400")
    Long duration;

    public String generateToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.email)
                .groups(Set.of(user.role.name()))
                .claim("userId", user.id)
                .claim("name", user.name)
                .claim("email", user.email)
                .claim("role", user.role.name())
                .expiresIn(Duration.ofSeconds(duration))
                .sign();
    }

    public String generateVerificationToken(String email, String code) {
        return Jwt.issuer(issuer)
                .upn(email)
                .claim("code", code)
                .claim("type", "verification")
                .expiresIn(Duration.ofMinutes(15))
                .sign();
    }
}

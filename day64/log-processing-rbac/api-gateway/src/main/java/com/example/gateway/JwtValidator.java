package com.example.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserContext extractUserContext(String token) {
        Claims claims = validateToken(token);
        
        UserContext context = new UserContext();
        context.setUsername(claims.getSubject());
        context.setUserId(claims.get("userId", Long.class));
        
        @SuppressWarnings("unchecked")
        List<String> rolesList = (List<String>) claims.get("roles");
        context.setRoles(rolesList != null ? Set.copyOf(rolesList) : Set.of());
        
        @SuppressWarnings("unchecked")
        List<String> teamsList = (List<String>) claims.get("teams");
        context.setTeams(teamsList != null ? Set.copyOf(teamsList) : Set.of());
        
        return context;
    }
}

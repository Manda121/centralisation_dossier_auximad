package com.auximad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Génère la clé de signature à partir du secret
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un token JWT pour un utilisateur
     */
    public String genererToken(Integer userId, String nomUtilisateur, Integer codeAgence, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("codeAgence", codeAgence);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(nomUtilisateur)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrait le nom d'utilisateur du token
     */
    public String extraireNomUtilisateur(String token) {
        return extraireClaims(token).getSubject();
    }

    /**
     * Vérifie si le token est valide
     */
    public boolean estValide(String token) {
        try {
            Claims claims = extraireClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrait toutes les claims du token
     */
    public Claims extraireClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Retourne la durée d'expiration en millisecondes
     */
    public Long getExpiration() {
        return expiration;
    }
}

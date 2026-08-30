package com.auximad.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper pour extraire les claims JWT de l'authentification courante.
 * Le filtre JwtAuthenticationFilter stocke les Claims dans authentication.details.
 */
public final class JwtSecurityContext {

    private JwtSecurityContext() {}

    /**
     * Extrait les claims JWT depuis le SecurityContext.
     */
    private static Claims getClaims() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Claims) {
            return (Claims) auth.getDetails();
        }
        return null;
    }

    /**
     * @return l'ID de l'agence de l'utilisateur connecté, ou null
     */
    public static Integer getIdAgence() {
        Claims claims = getClaims();
        if (claims == null) return null;
        Object val = claims.get("idAgence");
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    /**
     * @return le niveau d'autorisation de l'utilisateur connecté, ou 0
     */
    public static int getNiveauAutorisation() {
        Claims claims = getClaims();
        if (claims == null) return 0;
        Object val = claims.get("niveauAutorisation");
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    /**
     * @return l'ID de l'utilisateur connecté, ou null
     */
    public static Integer getIdUtilisateur() {
        Claims claims = getClaims();
        if (claims == null) return null;
        Object val = claims.get("userId");
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    /**
     * Retourne true si l'utilisateur a la permission requise.
     * Formule : niveauAutorisation >= (portee + codeAction)
     */
    public static boolean aPermission(int portee, int codeAction) {
        return getNiveauAutorisation() >= (portee + codeAction);
    }
}

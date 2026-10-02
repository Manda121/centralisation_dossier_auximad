package com.auximad.config;

import com.auximad.entity.Action;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.HistorisationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepteur qui journalise automatiquement chaque requête API dans l'historique :
 * visualisation (GET), création (POST), modification (PUT/PATCH), suppression (DELETE).
 *
 * Le login/logout sont traités séparément (plus de détails),
 * et les endpoints de haute fréquence (polling, Swagger) sont exclus pour éviter le bruit.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final HistorisationService historisationService;

    public AuditInterceptor(HistorisationService historisationService) {
        this.historisationService = historisationService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            String uri = request.getRequestURI();

            // Ne tracer que l'API métier
            if (!uri.startsWith("/api/") || uri.startsWith("/api/auth/")) {
                return;
            }
            // Exclusions : polling et bruit
            if (uri.contains("/non-vues-count") || uri.contains("/referentiels")
                    || uri.contains("/phases") || uri.contains("/statuts")
                    || uri.contains("/incontermes") || uri.contains("/swagger")
                    || uri.contains("/v3/api-docs")) {
                return;
            }

            // L'utilisateur doit être authentifié (les 401 n'ont rien à tracer ici)
            String methode = request.getMethod();
            // Automatic badge and chat status reads do not represent user actions.
            // Reading the audit journal must not create more audit entries.
            if ("GET".equals(methode) && (uri.equals("/api/dossiers/sans-documents")
                    || uri.equals("/api/documents/remises/urgentes")
                    || uri.equals("/api/chat/etat")
                    || uri.equals("/api/historiques") || uri.startsWith("/api/historiques/"))) {
                return;
            }
            Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
            if (idUtilisateur == null) {
                return;
            }

            if ("OPTIONS".equals(methode)) {
                return;
            }

            String nomAction = switch (methode) {
                case "POST" -> Action.CREATION;
                case "PUT", "PATCH" -> Action.MODIFICATION;
                case "DELETE" -> Action.SUPPRESSION;
                default -> Action.VISUALISATION;
            };

            // Nommage d'entité : /api/dossiers/12 -> "Dossier"
            String entite = extraireEntite(uri);

            String details = methode + " " + uri
                    + (response.getStatus() >= 400 ? " — échec (" + response.getStatus() + ")" : " — succès");

            historisationService.enregistrerPourUtilisateur(
                    idUtilisateur, nomAction, details, entite, null, response.getStatus());
        } catch (Exception e) {
            // L'audit ne doit jamais casser la requête
        }
    }

    private String extraireEntite(String uri) {
        String chemin = uri.substring("/api/".length());
        int slash = chemin.indexOf('/');
        String segment = (slash > 0 ? chemin.substring(0, slash) : chemin);
        if (segment.isBlank()) return "API";
        String singular = segment.endsWith("s") ? segment.substring(0, segment.length() - 1) : segment;
        // Capitalisation + nettoie les tirets (ex: ordres-transit -> Ordres-transit)
        return Character.toUpperCase(singular.charAt(0)) + singular.substring(1);
    }
}

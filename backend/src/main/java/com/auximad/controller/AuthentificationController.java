package com.auximad.controller;

import com.auximad.dto.LoginRequest;
import com.auximad.dto.LoginResponse;
import com.auximad.entity.Action;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.AuthentificationService;
import com.auximad.service.HistorisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "API de connexion et gestion des tokens JWT")
public class AuthentificationController {

    private final AuthentificationService authentificationService;
    private final HistorisationService historisationService;

    public AuthentificationController(AuthentificationService authentificationService,
                                      HistorisationService historisationService) {
        this.authentificationService = authentificationService;
        this.historisationService = historisationService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur avec son nom d'utilisateur, code agence et mot de passe. "
                    + "Retourne un token JWT en cas de succès. "
                    + "Après 3 tentatives échouées, le compte est suspendu temporairement."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connexion réussie",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects"),
            @ApiResponse(responseCode = "423", description = "Compte suspendu")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Map<String, Object> resultat = authentificationService.login(request);

        int status = (int) resultat.get("status");

        if (status == 200) {
            return ResponseEntity.ok(resultat.get("response"));
        }

        // Construire la réponse d'erreur
        Map<String, Object> erreur = Map.of(
                "erreur", resultat.get("erreur"),
                "status", status
        );

        if (resultat.containsKey("minutesRestantes")) {
            erreur = Map.of(
                    "erreur", resultat.get("erreur"),
                    "status", status,
                    "minutesRestantes", resultat.get("minutesRestantes")
            );
        }

        return ResponseEntity.status(status).body(erreur);
    }

    /**
     * Déconnexion : journalise l'événement côté serveur avant que le client
     * ne supprime son token. Le JWT reste valide jusqu'à expiration (stateless).
     */
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Journalise la déconnexion de l'utilisateur connecté.")
    public ResponseEntity<?> logout() {
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        String nomUtilisateur = null;
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                nomUtilisateur = auth.getName();
            }
        } catch (Exception ignored) {
        }

        if (idUtilisateur != null) {
            historisationService.enregistrerPourUtilisateur(idUtilisateur, Action.DECONNEXION,
                    "Déconnexion de " + (nomUtilisateur != null ? nomUtilisateur : "utilisateur #" + idUtilisateur),
                    "Utilisateur", nomUtilisateur, 200);
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion enregistrée"));
    }
}

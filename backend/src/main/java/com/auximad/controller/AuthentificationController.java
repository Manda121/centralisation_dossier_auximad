package com.auximad.controller;

import com.auximad.dto.LoginRequest;
import com.auximad.dto.LoginResponse;
import com.auximad.service.AuthentificationService;
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

    public AuthentificationController(AuthentificationService authentificationService) {
        this.authentificationService = authentificationService;
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
}

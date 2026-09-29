package com.auximad.controller;

import com.auximad.dto.DeclarationRequest;
import com.auximad.entity.Declaration;
import com.auximad.entity.TypeDeclaration;
import com.auximad.service.DeclarationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/declarations")
@Tag(name = "Déclarations Douanières", description = "API pour la gestion des déclarations douanières de dossier")
public class DeclarationController {

    private final DeclarationService declarationService;

    public DeclarationController(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @GetMapping("/types")
    @Operation(summary = "Lister les types de déclaration douanière")
    public ResponseEntity<List<TypeDeclaration>> getTypeDeclarations() {
        return ResponseEntity.ok(declarationService.getAllTypeDeclarations());
    }

    @GetMapping("/dossier/{dossierId}")
    @Operation(summary = "Obtenir la déclaration douanière d'un dossier")
    public ResponseEntity<?> getByDossierId(@PathVariable Integer dossierId) {
        return declarationService.getByDossierId(dossierId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer ou mettre à jour la déclaration douanière d'un dossier")
    public ResponseEntity<?> saveDeclaration(
            @RequestParam("dossierId") Integer dossierId,
            @RequestParam(value = "numeroDeclaration", required = false) String numeroDeclaration,
            @RequestParam(value = "dateDeclaration", required = false) String dateDeclarationStr,
            @RequestParam(value = "bureauId", required = false) Integer bureauId,
            @RequestParam(value = "typeDeclarationId", required = false) Integer typeDeclarationId,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "fichier", required = false) MultipartFile file
    ) {
        try {
            DeclarationRequest req = new DeclarationRequest();
            req.setDossierId(dossierId);
            req.setNumeroDeclaration(numeroDeclaration);
            req.setBureauId(bureauId);
            req.setTypeDeclarationId(typeDeclarationId);
            req.setDescription(description);
            if (dateDeclarationStr != null && !dateDeclarationStr.isBlank()) {
                req.setDateDeclaration(LocalDateTime.parse(dateDeclarationStr));
            } else {
                req.setDateDeclaration(LocalDateTime.now());
            }

            Declaration saved = declarationService.saveDeclaration(req, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erreur", "Erreur lors de la sauvegarde de la déclaration", "details", e.getMessage() != null ? e.getMessage() : ""));
        }
    }
}

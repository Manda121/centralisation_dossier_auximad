package com.auximad.controller;

import com.auximad.entity.Conteneur;
import com.auximad.entity.TypeContainer;
import com.auximad.service.ConteneurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conteneurs")
@Tag(name = "Conteneurs", description = "API pour l'assignation et la gestion des conteneurs de dossiers")
public class ConteneurController {

    private final ConteneurService conteneurService;

    public ConteneurController(ConteneurService conteneurService) {
        this.conteneurService = conteneurService;
    }

    @GetMapping("/types")
    @Operation(summary = "Lister tous les types de conteneurs (20 pieds, 40 pieds, High Cube, etc.)")
    public ResponseEntity<List<TypeContainer>> getAllTypes() {
        return ResponseEntity.ok(conteneurService.getAllTypeContainers());
    }

    @GetMapping
    @Operation(summary = "Lister les conteneurs (Optionnel: filtré par dossierId)")
    public ResponseEntity<List<Conteneur>> getConteneurs(@RequestParam(required = false) Integer dossierId) {
        if (dossierId != null) {
            return ResponseEntity.ok(conteneurService.getConteneursByDossierId(dossierId));
        }
        return ResponseEntity.ok(conteneurService.getAllConteneurs());
    }

    @GetMapping("/dossier/{dossierId}")
    @Operation(summary = "Obtenir tous les conteneurs assignés à un dossier")
    public ResponseEntity<List<Conteneur>> getByDossier(@PathVariable Integer dossierId) {
        return ResponseEntity.ok(conteneurService.getConteneursByDossierId(dossierId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un conteneur par son ID")
    public ResponseEntity<Conteneur> getById(@PathVariable Integer id) {
        return conteneurService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Assigner un conteneur à un dossier maritime")
    public ResponseEntity<?> create(@RequestBody Conteneur conteneur) {
        try {
            Conteneur créé = conteneurService.saveConteneur(conteneur);
            return ResponseEntity.status(HttpStatus.CREATED).body(créé);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'assignation du conteneur", "details", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un conteneur")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Conteneur conteneur) {
        try {
            conteneur.setId(id);
            Conteneur misAJour = conteneurService.saveConteneur(conteneur);
            return ResponseEntity.ok(misAJour);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la mise à jour du conteneur", "details", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer l'assignation d'un conteneur")
    public ResponseEntity<?> delete(@PathVariable Integer id, @RequestParam(required = false) Integer dossierId) {
        try {
            conteneurService.deleteConteneur(id, dossierId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression du conteneur", "details", e.getMessage()));
        }
    }
}

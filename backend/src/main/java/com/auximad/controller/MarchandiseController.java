package com.auximad.controller;

import com.auximad.entity.Marchandise;
import com.auximad.entity.NatureMarchandise;
import com.auximad.entity.UniteMesure;
import com.auximad.service.MarchandiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marchandises")
@Tag(name = "Marchandises", description = "API pour la gestion des marchandises au sein des ordres de transit")
public class MarchandiseController {

    private final MarchandiseService marchandiseService;

    public MarchandiseController(MarchandiseService marchandiseService) {
        this.marchandiseService = marchandiseService;
    }

    @GetMapping
    @Operation(summary = "Récupérer la liste des marchandises d'un Ordre de Transit")
    public ResponseEntity<List<Marchandise>> getByOt(@RequestParam Integer otId) {
        return ResponseEntity.ok(marchandiseService.getMarchandisesByOt(otId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une marchandise spécifique par son ID")
    public ResponseEntity<Marchandise> getById(@PathVariable Integer id) {
        return marchandiseService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer ou mettre à jour une marchandise")
    public ResponseEntity<Marchandise> save(@RequestBody Marchandise m) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marchandiseService.saveMarchandise(m));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une marchandise")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            marchandiseService.deleteMarchandise(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Référentiels
    @GetMapping("/unites")
    @Operation(summary = "Lister toutes les unités de mesure de la marchandise")
    public ResponseEntity<List<UniteMesure>> getAllUnites() {
        return ResponseEntity.ok(marchandiseService.getAllUniteMesures());
    }

    @GetMapping("/natures")
    @Operation(summary = "Lister toutes les natures de marchandise")
    public ResponseEntity<List<NatureMarchandise>> getAllNatures() {
        return ResponseEntity.ok(marchandiseService.getAllNatureMarchandises());
    }
}

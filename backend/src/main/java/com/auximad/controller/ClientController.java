package com.auximad.controller;

import com.auximad.dto.PageResponse;
import com.auximad.entity.Client;
import com.auximad.entity.Pays;
import com.auximad.entity.TypeClient;
import com.auximad.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "API de gestion des clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "Rechercher et lister les clients avec pagination et filtres avancés")
    public ResponseEntity<PageResponse<Client>> getClients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String nif,
            @RequestParam(required = false) String stat,
            @RequestParam(required = false) String fax,
            @RequestParam(required = false) Integer codeClient,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String cpClient,
            @RequestParam(required = false) Integer paysId,
            @RequestParam(required = false) Integer typeClientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        PageResponse<Client> response = clientService.searchClients(
                query, nif, stat, fax, codeClient, nom, prenom,
                email, adresse, ville, cpClient, paysId, typeClientId,
                page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Suggestions de clients pour l'autocomplétion (code client, nom, NIF, STAT)")
    public ResponseEntity<List<Client>> suggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") Integer limite) {
        return ResponseEntity.ok(clientService.suggestionsClients(q, limite));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les détails d'un client par son ID")
    public ResponseEntity<Client> getClientById(@PathVariable Integer id) {
        return clientService.getClientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau client")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        return ResponseEntity.ok(clientService.createClient(client));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un client existant")
    public ResponseEntity<Client> updateClient(@PathVariable Integer id, @RequestBody Client clientDetails) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, clientDetails));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un client")
    public ResponseEntity<Void> deleteClient(@PathVariable Integer id) {
        try {
            clientService.deleteClient(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/pays")
    @Operation(summary = "Lister tous les pays de référence")
    public ResponseEntity<List<Pays>> getAllPays() {
        return ResponseEntity.ok(clientService.getAllPays());
    }

    @GetMapping("/types")
    @Operation(summary = "Lister tous les types de clients de référence")
    public ResponseEntity<List<TypeClient>> getAllTypeClients() {
        return ResponseEntity.ok(clientService.getAllTypeClients());
    }
}

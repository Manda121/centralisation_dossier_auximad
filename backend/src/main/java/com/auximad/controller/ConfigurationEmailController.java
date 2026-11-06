package com.auximad.controller;

import com.auximad.dto.AdresseEmailAgenceRequest;
import com.auximad.dto.PageResponse;
import com.auximad.entity.AdresseEmailAgence;
import com.auximad.entity.HistoriqueMail;
import com.auximad.repository.HistoriqueMailRepository;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.ConfigurationEmailService;
import com.auximad.service.EnvoiMailService;
import com.auximad.service.NotificationInteragenceService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/emails")
public class ConfigurationEmailController {
    private final ConfigurationEmailService configuration;
    private final HistoriqueMailRepository historique;
    private final NotificationInteragenceService notifications;
    private final EnvoiMailService envoi;

    public ConfigurationEmailController(ConfigurationEmailService configuration, HistoriqueMailRepository historique,
            NotificationInteragenceService notifications, EnvoiMailService envoi) {
        this.configuration = configuration;
        this.historique = historique;
        this.notifications = notifications;
        this.envoi = envoi;
    }

    private void exigerAdmin() {
        if (JwtSecurityContext.getNiveauAutorisation() < 10000) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs.");
        }
    }

    @GetMapping("/configuration")
    public Map<String, Boolean> etat() {
        exigerAdmin();
        return Map.of("smtpConfigure", envoi.estConfigure());
    }

    @GetMapping("/adresses")
    public List<AdresseEmailAgence> lister() {
        exigerAdmin();
        return configuration.lister();
    }

    @PostMapping("/adresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AdresseEmailAgence creer(@RequestBody AdresseEmailAgenceRequest request) {
        exigerAdmin();
        return configuration.enregistrer(null, request);
    }

    @PutMapping("/adresses/{id}")
    public AdresseEmailAgence modifier(@PathVariable Integer id, @RequestBody AdresseEmailAgenceRequest request) {
        exigerAdmin();
        return configuration.enregistrer(id, request);
    }

    @GetMapping("/historique")
    public PageResponse<HistoriqueMail> historique(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size, @RequestParam(required = false) Integer agenceId,
            @RequestParam(required = false) HistoriqueMail.Statut statut) {
        exigerAdmin();
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagination invalide (taille de 1 à 100).");
        }
        return PageResponse.of(historique.rechercher(agenceId, statut,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @PostMapping("/historique/{id}/relancer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> relancer(@PathVariable Long id) {
        exigerAdmin();
        notifications.relancer(id);
        return Map.of("message", "Nouvelle tentative enregistrée dans l'historique.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<?> erreur(ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("erreur", e.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflit() {
        return Map.of("erreur", "L'adresse existe déjà pour cette agence ou les données associées ont changé.");
    }
}

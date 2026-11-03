package com.auximad.service;

import com.auximad.dto.AdresseEmailAgenceRequest;
import com.auximad.entity.AdresseEmailAgence;
import com.auximad.repository.AdresseEmailAgenceRepository;
import com.auximad.repository.AgenceRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ConfigurationEmailService {
    private final AdresseEmailAgenceRepository adresses;
    private final AgenceRepository agences;

    public ConfigurationEmailService(AdresseEmailAgenceRepository adresses, AgenceRepository agences) {
        this.adresses = adresses;
        this.agences = agences;
    }

    public List<AdresseEmailAgence> lister() {
        return adresses.findAllByOrderByAgenceNomAscEmailAsc();
    }

    public static String validerAdresse(String valeur) {
        String email = valeur == null ? "" : valeur.trim();
        try {
            if (email.isBlank() || email.length() > 254 || email.contains("\r") || email.contains("\n")) {
                throw new AddressException();
            }
            InternetAddress adresse = new InternetAddress(email, true);
            adresse.validate();
            if (adresse.getPersonal() != null || !email.equals(adresse.getAddress()) || !email.contains("@")) {
                throw new AddressException();
            }
        } catch (AddressException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse email invalide.");
        }
        return email.toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AdresseEmailAgence enregistrer(Integer id, AdresseEmailAgenceRequest request) {
        String email = validerAdresse(request.email());
        if (request.idAgence() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence obligatoire.");
        }
        var agence = agences.findById(request.idAgence()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable."));
        AdresseEmailAgence adresse = id == null ? new AdresseEmailAgence() : adresses.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adresse introuvable."));
        if (adresses.existsByAgenceIdAndEmailAndIdNot(agence.getId(), email, id == null ? -1 : id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse existe déjà pour cette agence.");
        }
        adresse.setAgence(agence);
        adresse.setEmail(email);
        adresse.setActif(!Boolean.FALSE.equals(request.actif()));
        return adresses.save(adresse);
    }
}

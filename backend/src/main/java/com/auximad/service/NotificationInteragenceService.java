package com.auximad.service;

import com.auximad.entity.DemandeInteragence;
import com.auximad.entity.HistoriqueMail;
import com.auximad.repository.AdresseEmailAgenceRepository;
import com.auximad.repository.HistoriqueMailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class NotificationInteragenceService {
    private final AdresseEmailAgenceRepository adresses;
    private final HistoriqueMailRepository historique;

    public NotificationInteragenceService(AdresseEmailAgenceRepository adresses, HistoriqueMailRepository historique) {
        this.adresses = adresses;
        this.historique = historique;
    }

    // La file et la demande sont validées ensemble. Aucun SMTP avant le commit.
    @Transactional(propagation = Propagation.MANDATORY)
    public void preparer(DemandeInteragence demande) {
        historique.annulerEnAttente(demande.getId(), HistoriqueMail.Statut.EN_ATTENTE, HistoriqueMail.Statut.ANNULE);
        HistoriqueMail modele = new HistoriqueMail();
        modele.setIdDemandeInteragence(demande.getId());
        modele.setIdAgence(demande.getAgenceDestinataire().getId());
        modele.setNomAgence(demande.getAgenceDestinataire().getNom());
        modele.setNumeroDossier(demande.getDossier().getNumeroDossier());
        modele.setSujet("Demande interagences n°" + demande.getId() + " — dossier " + modele.getNumeroDossier());
        modele.setCorps("Bonjour,\n\nL'agence " + demande.getAgenceDemandeuse().getNom()
                + " sollicite votre agence " + modele.getNomAgence() + " pour le dossier "
                + modele.getNumeroDossier() + ".\n\nDemande n°" + demande.getId()
                + "\nMotif : " + (demande.getMotif() == null ? "Non précisé" : demande.getMotif())
                + "\n\nConnectez-vous à l'application Auximad, rubrique Demandes interagences, pour répondre.\n");
        preparerDestinataires(modele);
    }

    private void preparerDestinataires(HistoriqueMail modele) {
        List<String> destinataires = adresses.findByAgenceIdAndActifTrueOrderByEmailAsc(modele.getIdAgence())
                .stream().map(a -> a.getEmail()).toList();
        if (destinataires.isEmpty()) {
            HistoriqueMail mail = copier(modele);
            mail.setStatut(HistoriqueMail.Statut.NON_CONFIGURE);
            mail.setErreur("Aucune adresse email active pour l'agence sollicitée.");
            historique.save(mail);
        } else {
            for (String destinataire : destinataires) {
                HistoriqueMail mail = copier(modele);
                mail.setDestinataire(destinataire);
                historique.save(mail);
            }
        }
    }

    @Transactional
    public void relancer(Long id) {
        HistoriqueMail precedent = historique.verrouiller(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Email introuvable."));
        if (precedent.getStatut() != HistoriqueMail.Statut.ECHEC
                && precedent.getStatut() != HistoriqueMail.Statut.NON_CONFIGURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seul un email en échec ou non configuré peut être relancé.");
        }
        HistoriqueMail nouveau = copier(precedent);
        if (historique.existsByIdMailOrigine(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet email a déjà été relancé. Consultez la dernière tentative dans l'historique.");
        }
        nouveau.setIdMailOrigine(precedent.getId());
        if (precedent.getDestinataire() == null) {
            preparerDestinataires(nouveau);
        } else {
            nouveau.setDestinataire(precedent.getDestinataire());
            historique.save(nouveau);
        }
    }

    private HistoriqueMail copier(HistoriqueMail modele) {
        HistoriqueMail mail = new HistoriqueMail();
        mail.setIdDemandeInteragence(modele.getIdDemandeInteragence());
        mail.setIdAgence(modele.getIdAgence());
        mail.setNomAgence(modele.getNomAgence());
        mail.setNumeroDossier(modele.getNumeroDossier());
        mail.setSujet(modele.getSujet());
        mail.setCorps(modele.getCorps());
        mail.setIdMailOrigine(modele.getIdMailOrigine());
        return mail;
    }
}

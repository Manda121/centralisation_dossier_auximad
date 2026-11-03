package com.auximad.service;

import com.auximad.entity.HistoriqueMail;
import com.auximad.repository.HistoriqueMailRepository;
import com.auximad.repository.DemandeInteragenceRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Service
public class EnvoiMailService {
    private final HistoriqueMailRepository historique;
    private final ObjectProvider<JavaMailSender> sender;
    private final String expediteur;
    private final String smtpHote;
    private final DemandeInteragenceRepository demandes;

    public EnvoiMailService(HistoriqueMailRepository historique, ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.from:}") String expediteur,
            @Value("${spring.mail.host:}") String smtpHote,
            DemandeInteragenceRepository demandes) {
        this.historique = historique;
        this.sender = sender;
        this.expediteur = expediteur;
        this.smtpHote = smtpHote;
        this.demandes = demandes;
    }

    public boolean estConfigure() {
        return sender.getIfAvailable() != null && smtpHote != null && !smtpHote.isBlank()
                && expediteur != null && !expediteur.isBlank();
    }

    @Transactional
    public void traiter(Long id) {
        HistoriqueMail mail = historique.verrouiller(id).orElse(null);
        if (mail == null || mail.getStatut() != HistoriqueMail.Statut.EN_ATTENTE) return;
        var demande = demandes.findById(mail.getIdDemandeInteragence()).orElse(null);
        if (demande == null || !"EN_ATTENTE".equals(demande.getStatut())
                || !mail.getIdAgence().equals(demande.getAgenceDestinataire().getId())) {
            mail.setStatut(HistoriqueMail.Statut.ANNULE);
            mail.setErreur("Envoi annulé : la demande a été traitée ou son agence destinataire a changé.");
            historique.save(mail);
            return;
        }
        if (!estConfigure()) {
            mail.setStatut(HistoriqueMail.Statut.NON_CONFIGURE);
            mail.setErreur("Serveur SMTP ou adresse expéditrice non configuré. Contactez l'administrateur du serveur.");
            historique.save(mail);
            return;
        }
        mail.setDateTentative(LocalDateTime.now());
        try {
            String from = ConfigurationEmailService.validerAdresse(expediteur);
            ConfigurationEmailService.validerAdresse(mail.getDestinataire());
            mail.setExpediteur(from);
            JavaMailSender javaMailSender = sender.getObject();
            var message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(mail.getDestinataire());
            helper.setSubject(mail.getSujet().replaceAll("[\\r\\n]", " "));
            helper.setText(mail.getCorps(), false);
            javaMailSender.send(message);
            mail.setStatut(HistoriqueMail.Statut.ENVOYE);
            mail.setDateEnvoi(LocalDateTime.now());
            mail.setErreur(null);
        } catch (MailException | MessagingException e) {
            mail.setStatut(HistoriqueMail.Statut.ECHEC);
            // Ne pas exposer les réponses SMTP ni les identifiants dans l'interface.
            mail.setErreur("Échec de l'envoi SMTP (" + e.getClass().getSimpleName()
                    + "). Vérifiez le serveur, l'authentification et l'adresse destinataire.");
        } catch (ResponseStatusException e) {
            mail.setStatut(HistoriqueMail.Statut.NON_CONFIGURE);
            mail.setErreur("Adresse expéditrice ou destinataire invalide.");
        }
        historique.save(mail);
    }
}

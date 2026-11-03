package com.auximad.config;

import com.auximad.entity.HistoriqueMail;
import com.auximad.repository.HistoriqueMailRepository;
import com.auximad.service.EnvoiMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class MailScheduler {
    private static final Logger log = LoggerFactory.getLogger(MailScheduler.class);
    private final HistoriqueMailRepository historique;
    private final EnvoiMailService envoi;

    public MailScheduler(HistoriqueMailRepository historique, EnvoiMailService envoi) {
        this.historique = historique;
        this.envoi = envoi;
    }

    @Scheduled(initialDelayString = "${app.mail.poll-delay:10000}", fixedDelayString = "${app.mail.poll-delay:10000}")
    public void envoyerEnAttente() {
        for (HistoriqueMail mail : historique.findByStatutOrderByIdAsc(
                HistoriqueMail.Statut.EN_ATTENTE, PageRequest.of(0, 10))) {
            try {
                envoi.traiter(mail.getId());
            } catch (RuntimeException e) {
                log.error("Impossible de traiter l'email {} ({})", mail.getId(), e.getClass().getSimpleName());
            }
        }
    }
}

package com.auximad.service;

import com.auximad.entity.Action;
import com.auximad.entity.Historique;
import com.auximad.repository.ActionRepository;
import com.auximad.repository.HistoriqueRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.JwtSecurityContext;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'audit : enregistre chaque action métier dans la table historique
 * (utilisateur, IP, hostname, URL, logiciel, type d'action, détails…).
 * L'écriture est asynchrone pour ne jamais ralentir ni bloquer la requête métier,
 * et toute erreur d'audit est silencieuse (l'audit ne doit pas casser l'application).
 */
@Service
public class HistorisationService {

    private static final Logger log = LoggerFactory.getLogger(HistorisationService.class);

    private final ActionRepository actionRepository;
    private final HistoriqueRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Auto-injection (proxy) pour que @Async fonctionne malgré l'appel interne
    private final HistorisationService self;

    @Autowired
    public HistorisationService(ActionRepository actionRepository,
                                HistoriqueRepository historiqueRepository,
                                UtilisateurRepository utilisateurRepository,
                                @Lazy HistorisationService self) {
        this.actionRepository = actionRepository;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.self = self;
    }

    /**
     * Au démarrage : s'assure que les actions de base existent dans la table action.
     */
    @PostConstruct
    @Transactional
    public void seedActions() {
        List<String[]> actions = List.of(
                new String[]{Action.VISUALISATION, "Visualisation d'une ressource"},
                new String[]{Action.CREATION, "Création d'une ressource"},
                new String[]{Action.MODIFICATION, "Modification d'une ressource"},
                new String[]{Action.SUPPRESSION, "Suppression d'une ressource"},
                new String[]{Action.VALIDATION, "Validation d'une ressource"},
                new String[]{Action.CONNEXION, "Connexion utilisateur réussie"},
                new String[]{Action.DECONNEXION, "Déconnexion utilisateur"},
                new String[]{Action.CONNEXION_ECHOUEE, "Tentative de connexion échouée"}
        );
        for (String[] a : actions) {
            if (actionRepository.findByNom(a[0]).isEmpty()) {
                Action action = new Action();
                action.setNom(a[0]);
                action.setDescription(a[1]);
                actionRepository.save(action);
                log.info("Action d'audit créée : {}", a[0]);
            }
        }
    }

    /**
     * Enregistre une action pour l'utilisateur connecté (déduit du JWT).
     * Les infos de requête (IP, hostname, logiciel) sont capturées immédiatement
     * sur le thread HTTP, l'écriture en base est asynchrone.
     */
    public void enregistrer(String nomAction, String details, String entite, String identifiantEntite) {
        enregistrer(nomAction, details, entite, identifiantEntite, 200);
    }

    public void enregistrer(String nomAction, String details, String entite, String identifiantEntite, int statutHttp) {
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        enregistrerPourUtilisateur(idUtilisateur, nomAction, details, entite, identifiantEntite, statutHttp);
    }

    /**
     * Variante pour le login : l'utilisateur n'a pas encore de JWT,
     * son ID est passé directement par le service d'authentification.
     */
    public void enregistrerPourUtilisateur(Integer idUtilisateur, String nomAction, String details,
                                           String entite, String identifiantEntite, int statutHttp) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String ip = null;
        String hostname = null;
        String logiciel = null;
        String url = null;
        String methode = null;

        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ip = extraireIpClient(request);

            // Hostname : le lien et le port (ex: localhost:8080, 192.4.56.1:5173)
            String hostHeader = request.getHeader("Host");
            if (hostHeader != null && !hostHeader.isBlank()) {
                hostname = hostHeader;
            } else {
                hostname = request.getServerName() + ":" + request.getServerPort();
            }

            // Logiciel : extraction dynamique du logiciel/navigateur réel à partir du User-Agent
            logiciel = extraireNomLogiciel(request.getHeader("User-Agent"));

            url = request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            methode = request.getMethod();
        }

        // Persistance asynchrone via le proxy Spring (sinon @Async serait ignoré)
        self.persistAsync(nomAction, idUtilisateur, ip, hostname, logiciel, url, methode, statutHttp,
                details, entite, identifiantEntite);
    }

    /**
     * Écriture asynchrone en base. Ne doit jamais faire échouer la requête métier.
     */
    @Async
    public void persistAsync(String nomAction, Integer idUtilisateur, String ip, String hostname,
                             String logiciel, String url, String methode, int statutHttp,
                             String details, String entite, String identifiantEntite) {
        try {
            Historique h = new Historique();
            h.setDateAction(LocalDateTime.now());
            h.setAdresseIp(ip);
            h.setHostname(hostname);
            h.setLogiciel(tronquer(logiciel, 200));
            h.setUrl(tronquer(url, 200));
            h.setMethodeHttp(methode);
            h.setStatutHttp(statutHttp);
            h.setDetails(details);
            h.setEntite(tronquer(entite, 255));
            h.setIdentifiantEntite(tronquer(identifiantEntite, 100));

            if (idUtilisateur != null) {
                utilisateurRepository.findById(idUtilisateur).ifPresent(h::setUtilisateur);
            }

            Action action = actionRepository.findByNom(nomAction).orElse(null);
            if (action == null) {
                Action nouvelle = new Action();
                nouvelle.setNom(nomAction);
                nouvelle.setDescription("Action générée automatiquement");
                action = actionRepository.save(nouvelle);
            }
            h.setAction(action);

            historiqueRepository.save(h);
        } catch (Exception e) {
            log.error("Échec de l'enregistrement d'audit (action={}, url={})", nomAction, url, e);
        }
    }

    private String extraireIpClient(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For peut contenir une liste : le client est le premier
                return ip.split(",")[0].trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()
                || "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr)) {
            try {
                return java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                return remoteAddr != null ? remoteAddr : "127.0.0.1";
            }
        }
        return remoteAddr;
    }

    /**
     * Analyse dynamiquement le User-Agent pour en déduire le nom réel du logiciel/navigateur exécutant la requête.
     */
    private String extraireNomLogiciel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Inconnu";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/") || ua.contains("edge/")) {
            return "Microsoft Edge";
        } else if (ua.contains("opr/") || ua.contains("opera/")) {
            return "Opera";
        } else if (ua.contains("chrome/") || ua.contains("crios/")) {
            return "Google Chrome";
        } else if (ua.contains("firefox/") || ua.contains("fxios/")) {
            return "Mozilla Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Apple Safari";
        } else if (ua.contains("postman")) {
            return "Postman";
        } else if (ua.contains("insomnia")) {
            return "Insomnia";
        } else if (ua.contains("curl")) {
            return "cURL";
        } else if (ua.contains("swagger")) {
            return "Swagger UI";
        }

        int slash = userAgent.indexOf('/');
        if (slash > 0) {
            return userAgent.substring(0, slash).trim();
        }
        return userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent;
    }

    private String tronquer(String valeur, int max) {
        if (valeur == null) return null;
        return valeur.length() <= max ? valeur : valeur.substring(0, max);
    }
}

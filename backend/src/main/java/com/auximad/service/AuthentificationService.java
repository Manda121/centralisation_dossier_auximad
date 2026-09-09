package com.auximad.service;

import com.auximad.dto.LoginRequest;
import com.auximad.dto.LoginResponse;
import com.auximad.entity.Autorisation;
import com.auximad.entity.SuspensionConnexion;
import com.auximad.entity.Utilisateur;
import com.auximad.repository.AutorisationRepository;
import com.auximad.repository.SuspensionConnexionRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthentificationService {

    private final UtilisateurRepository utilisateurRepository;
    private final SuspensionConnexionRepository suspensionRepository;
    private final AutorisationRepository autorisationRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final HistorisationService historisationService;

    @Value("${login.max-tentatives}")
    private int maxTentatives;

    @Value("${login.duree-suspension-minutes}")
    private int dureeSuspensionMinutes;

    public AuthentificationService(UtilisateurRepository utilisateurRepository,
                                    SuspensionConnexionRepository suspensionRepository,
                                    AutorisationRepository autorisationRepository,
                                    JwtUtil jwtUtil,
                                    PasswordEncoder passwordEncoder,
                                    @Lazy HistorisationService historisationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.suspensionRepository = suspensionRepository;
        this.autorisationRepository = autorisationRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.historisationService = historisationService;
    }

    /**
     * Traite une demande de connexion.
     * Retourne un Map contenant soit le LoginResponse (clé "response"), soit une erreur (clé "erreur").
     */
    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> resultat = new HashMap<>();

        // 1. Rechercher l'utilisateur
        Utilisateur utilisateur = utilisateurRepository
                .findByNomUtilisateurAndAgenceCode(request.getNomUtilisateur(), request.getCodeAgence())
                .orElse(null);

        if (utilisateur == null) {
            historisationService.enregistrerPourUtilisateur(null, "connexion echouee",
                    "Échec de connexion — identifiants inconnus (nom=" + request.getNomUtilisateur()
                            + ", codeAgence=" + request.getCodeAgence() + ")",
                    "Utilisateur", request.getNomUtilisateur(), 401);
            resultat.put("erreur", "Nom d'utilisateur, code agence ou mot de passe incorrect");
            resultat.put("status", 401);
            return resultat;
        }

        // 2. Vérifier la suspension
        SuspensionConnexion suspension = suspensionRepository
                .findByUtilisateurId(utilisateur.getId())
                .orElseGet(() -> {
                    SuspensionConnexion nouvelle = SuspensionConnexion.creerPourUtilisateur(utilisateur);
                    return suspensionRepository.save(nouvelle);
                });

        if (suspension.estSuspendu()) {
            long minutesRestantes = suspension.getTempsRestantMinutes();
            historisationService.enregistrerPourUtilisateur(utilisateur.getId(), "connexion echouee",
                    "Échec de connexion — compte suspendu (nom=" + utilisateur.getNomUtilisateur() + ")",
                    "Utilisateur", utilisateur.getNomUtilisateur(), 423);
            resultat.put("erreur", "Compte suspendu. Réessayez dans " + minutesRestantes + " minute(s)");
            resultat.put("status", 423);
            resultat.put("minutesRestantes", minutesRestantes);
            return resultat;
        }

        // 3. Vérifier le mot de passe
        if (!utilisateur.verifierMotDePasse(request.getMotDePasse(), passwordEncoder)) {
            suspension.incrementerTentatives(maxTentatives, dureeSuspensionMinutes);
            suspensionRepository.save(suspension);

            int tentativesRestantes = maxTentatives - suspension.getTentativesEchouees();

            historisationService.enregistrerPourUtilisateur(utilisateur.getId(), "connexion echouee",
                    "Échec de connexion — mot de passe incorrect (nom=" + utilisateur.getNomUtilisateur()
                            + ", restant=" + tentativesRestantes + ")",
                    "Utilisateur", utilisateur.getNomUtilisateur(), 401);

            if (suspension.estSuspendu()) {
                resultat.put("erreur", "Trop de tentatives échouées. Compte suspendu pour "
                        + dureeSuspensionMinutes + " minute(s)");
                resultat.put("status", 423);
            } else {
                resultat.put("erreur", "Mot de passe incorrect. "
                        + tentativesRestantes + " tentative(s) restante(s)");
                resultat.put("status", 401);
            }
            return resultat;
        }

        // 4. Connexion réussie — réinitialiser les tentatives
        suspension.reinitialiser();
        suspensionRepository.save(suspension);

        // 5. Récupérer le niveau d'autorisation
        Integer niveauAutorisation = autorisationRepository
                .findByUtilisateurId(utilisateur.getId())
                .map(Autorisation::getNiveauAutorisation)
                .orElse(0);

        // 6. Générer le token JWT
        String token = jwtUtil.genererToken(
                utilisateur.getId(),
                utilisateur.getNomUtilisateur(),
                utilisateur.getAgence().getCode(),
                utilisateur.getAgence().getId(),
                utilisateur.getRole().getNom(),
                niveauAutorisation
        );

        System.out.println("Niveau autorisation pour l'utilisateur " + utilisateur.getId() + ": " + niveauAutorisation + " token: "+ token);

        // Audit : connexion réussie
        historisationService.enregistrerPourUtilisateur(utilisateur.getId(), "connexion",
                "Connexion réussie de " + utilisateur.getNomComplet() + " (" + utilisateur.getNomUtilisateur() + ")",
                "Utilisateur", utilisateur.getNomUtilisateur(), 200);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .expiration(jwtUtil.getExpiration())
                .id(utilisateur.getId())
                .nomUtilisateur(utilisateur.getNomUtilisateur())
                .nomComplet(utilisateur.getNomComplet())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().getNom())
                .agence(utilisateur.getAgence().getNom())
                .codeAgence(utilisateur.getAgence().getCode())
                .idAgence(utilisateur.getAgence().getId())
                .niveauAutorisation(niveauAutorisation)
                .build();

        resultat.put("response", response);
        resultat.put("status", 200);
        return resultat;
    }
}

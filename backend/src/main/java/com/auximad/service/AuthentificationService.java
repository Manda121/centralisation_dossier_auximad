package com.auximad.service;

import com.auximad.dto.LoginRequest;
import com.auximad.dto.LoginResponse;
import com.auximad.entity.SuspensionConnexion;
import com.auximad.entity.Utilisateur;
import com.auximad.repository.SuspensionConnexionRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthentificationService {

    private final UtilisateurRepository utilisateurRepository;
    private final SuspensionConnexionRepository suspensionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${login.max-tentatives}")
    private int maxTentatives;

    @Value("${login.duree-suspension-minutes}")
    private int dureeSuspensionMinutes;

    public AuthentificationService(UtilisateurRepository utilisateurRepository,
                                    SuspensionConnexionRepository suspensionRepository,
                                    JwtUtil jwtUtil,
                                    PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.suspensionRepository = suspensionRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
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

        // 5. Générer le token JWT
        String token = jwtUtil.genererToken(
                utilisateur.getId(),
                utilisateur.getNomUtilisateur(),
                utilisateur.getAgence().getCode(),
                utilisateur.getRole().getNom()
        );

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .expiration(jwtUtil.getExpiration())
                .nomUtilisateur(utilisateur.getNomUtilisateur())
                .nomComplet(utilisateur.getNomComplet())
                .role(utilisateur.getRole().getNom())
                .agence(utilisateur.getAgence().getNom())
                .codeAgence(utilisateur.getAgence().getCode())
                .build();

        resultat.put("response", response);
        resultat.put("status", 200);
        return resultat;
    }
}

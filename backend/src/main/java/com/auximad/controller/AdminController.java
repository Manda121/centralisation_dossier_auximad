package com.auximad.controller;

import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.OperationDossierService;
import com.auximad.service.SuppressionService;
import com.auximad.service.SuppressionConfirmationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur d'administration (niveau autorisation = 10000 requis).
 *
 * Couvre les CRUD pour toutes les entités de configuration :
 *  1. Utilisateurs & Sécurité
 *  2. Structure organisationnelle
 *  3. Référentiels métier transit
 *  4. Documents
 *  5. Tiers / annuaires
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Endpoints réservés aux administrateurs (niveau 10000)")
public class AdminController {

    private final SuppressionService suppressionService;
    private static final int NIVEAU_ADMIN = 10000;

    // ── Repositories ────────────────────────────────────────────────────────
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final AutorisationRepository autorisationRepository;
    private final SuspensionConnexionRepository suspensionConnexionRepository;
    private final ActionRepository actionRepository;
    private final AgenceRepository agenceRepository;
    private final BureauRepository bureauRepository;
    private final ModeTransportRepository modeTransportRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final TypeRegimeRepository typeRegimeRepository;
    private final PhaseDossierRepository phaseDossierRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final StatutOtRepository statutOtRepository;
    private final IncontermeRepository incontermeRepository;
    private final NatureMarchandiseRepository natureMarchandiseRepository;
    private final UniteMesureRepository uniteMesureRepository;
    private final TypeContainerRepository typeContainerRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final CompagnieRepository compagnieRepository;
    private final NavireRepository navireRepository;
    private final PaysRepository paysRepository;
    private final PasswordEncoder passwordEncoder;
    private final OperationDossierService operationDossierService;

    public AdminController(
            UtilisateurRepository utilisateurRepository,
            RoleRepository roleRepository,
            AutorisationRepository autorisationRepository,
            SuspensionConnexionRepository suspensionConnexionRepository,
            ActionRepository actionRepository,
            AgenceRepository agenceRepository,
            BureauRepository bureauRepository,
            ModeTransportRepository modeTransportRepository,
            TypeProductionRepository typeProductionRepository,
            TypeRegimeRepository typeRegimeRepository,
            PhaseDossierRepository phaseDossierRepository,
            StatutDossierRepository statutDossierRepository,
            StatutOtRepository statutOtRepository,
            IncontermeRepository incontermeRepository,
            NatureMarchandiseRepository natureMarchandiseRepository,
            UniteMesureRepository uniteMesureRepository,
            TypeContainerRepository typeContainerRepository,
            TypeDocumentRepository typeDocumentRepository,
            CompagnieRepository compagnieRepository,
            NavireRepository navireRepository,
            PaysRepository paysRepository,
            PasswordEncoder passwordEncoder,
            OperationDossierService operationDossierService, SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.autorisationRepository = autorisationRepository;
        this.suspensionConnexionRepository = suspensionConnexionRepository;
        this.actionRepository = actionRepository;
        this.agenceRepository = agenceRepository;
        this.bureauRepository = bureauRepository;
        this.modeTransportRepository = modeTransportRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.typeRegimeRepository = typeRegimeRepository;
        this.phaseDossierRepository = phaseDossierRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.statutOtRepository = statutOtRepository;
        this.incontermeRepository = incontermeRepository;
        this.natureMarchandiseRepository = natureMarchandiseRepository;
        this.uniteMesureRepository = uniteMesureRepository;
        this.typeContainerRepository = typeContainerRepository;
        this.typeDocumentRepository = typeDocumentRepository;
        this.compagnieRepository = compagnieRepository;
        this.navireRepository = navireRepository;
        this.paysRepository = paysRepository;
        this.passwordEncoder = passwordEncoder;
        this.operationDossierService = operationDossierService;
    }

    // ── Garde d'accès ────────────────────────────────────────────────────────
    private boolean estAdmin() {
        return JwtSecurityContext.getNiveauAutorisation() >= NIVEAU_ADMIN;
    }

    private ResponseEntity<?> interdit() {
        return ResponseEntity.status(403)
                .body(Map.of("erreur", "Accès refusé — niveau d'autorisation insuffisant (10000 requis)"));
    }

    private Integer parseId(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private ResponseEntity<?> securiserSuppression(Runnable deleteAction, String nomEntite) {
        try {
            deleteAction.run();
            return ResponseEntity.ok(Map.of("message", nomEntite + " supprimé(e) avec succès"));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", "La suppression a été annulée : les données liées ont changé ou une contrainte empêche cette opération."));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. UTILISATEURS
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/utilisateurs")
    public ResponseEntity<?> listerUtilisateurs() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<?> obtenirUtilisateur(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        return utilisateurRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/utilisateurs")
    public ResponseEntity<?> creerUtilisateur(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            Utilisateur u = new Utilisateur();
            u.setNomUtilisateur((String) body.get("nomUtilisateur"));
            u.setNom((String) body.get("nom"));
            u.setPrenom((String) body.get("prenom"));
            u.setEmail((String) body.get("email"));
            String mdp = (String) body.get("motDePasse");
            if (mdp == null || mdp.isBlank())
                return ResponseEntity.badRequest().body(Map.of("erreur", "Mot de passe requis"));
            u.setMotDePasse(passwordEncoder.encode(mdp));

            Integer idRole = parseId(body.get("idRole"));
            Integer idAgence = parseId(body.get("idAgence"));
            if (idRole == null || idAgence == null)
                return ResponseEntity.badRequest().body(Map.of("erreur", "idRole et idAgence sont requis"));

            u.setRole(roleRepository.findById(idRole).orElseThrow(() -> new IllegalArgumentException("Rôle introuvable")));
            u.setAgence(agenceRepository.findById(idAgence).orElseThrow(() -> new IllegalArgumentException("Agence introuvable")));
            u.setDateCreation(LocalDateTime.now());
            Utilisateur saved = utilisateurRepository.save(u);

            Integer niveauAut = parseId(body.get("niveauAutorisation"));
            if (niveauAut == null) niveauAut = 0;
            Autorisation aut = new Autorisation();
            aut.setUtilisateur(saved);
            aut.setNiveauAutorisation(niveauAut);
            autorisationRepository.save(aut);

            return ResponseEntity.ok(saved);
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/utilisateurs/{id}")
    public ResponseEntity<?> modifierUtilisateur(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<Utilisateur> opt = utilisateurRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Utilisateur u = opt.get();
        if (body.containsKey("nom")) u.setNom((String) body.get("nom"));
        if (body.containsKey("prenom")) u.setPrenom((String) body.get("prenom"));
        if (body.containsKey("email")) u.setEmail((String) body.get("email"));
        if (body.containsKey("nomUtilisateur")) u.setNomUtilisateur((String) body.get("nomUtilisateur"));
        String mdp = (String) body.get("motDePasse");
        if (mdp != null && !mdp.isBlank()) u.setMotDePasse(passwordEncoder.encode(mdp));
        Integer idRole = parseId(body.get("idRole"));
        if (idRole != null) roleRepository.findById(idRole).ifPresent(u::setRole);
        Integer idAgence = parseId(body.get("idAgence"));
        if (idAgence != null) agenceRepository.findById(idAgence).ifPresent(u::setAgence);
        u.setDateModification(LocalDateTime.now());
        return ResponseEntity.ok(utilisateurRepository.save(u));
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!utilisateurRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("utilisateur", id), "Utilisateur");
    }

    @PostMapping("/utilisateurs/{id}/reinitialiser-mdp")
    public ResponseEntity<?> reinitialiserMotDePasse(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        if (!estAdmin()) return interdit();
        Optional<Utilisateur> opt = utilisateurRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        String nouveauMdp = body.get("motDePasse");
        if (nouveauMdp == null || nouveauMdp.isBlank())
            return ResponseEntity.badRequest().body(Map.of("erreur", "Mot de passe vide"));
        Utilisateur u = opt.get();
        u.setMotDePasse(passwordEncoder.encode(nouveauMdp));
        u.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(u);
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. RÔLES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/roles")
    public ResponseEntity<?> listerRoles() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping("/roles")
    public ResponseEntity<?> creerRole(@RequestBody Role role) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(roleRepository.save(role));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<?> modifierRole(@PathVariable Integer id, @RequestBody Role body) {
        if (!estAdmin()) return interdit();
        Optional<Role> opt = roleRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Role r = opt.get();
        if (body.getNom() != null) r.setNom(body.getNom());
        if (body.getDescription() != null) r.setDescription(body.getDescription());
        return ResponseEntity.ok(roleRepository.save(r));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> supprimerRole(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!roleRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("role", id), "Rôle");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. AUTORISATIONS
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/autorisations")
    public ResponseEntity<?> listerAutorisations() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(autorisationRepository.findAll());
    }

    @PutMapping("/autorisations/utilisateur/{idUtilisateur}")
    public ResponseEntity<?> modifierAutorisation(@PathVariable Integer idUtilisateur,
                                                   @RequestBody Map<String, Integer> body) {
        if (!estAdmin()) return interdit();
        Integer niveau = body.get("niveauAutorisation");
        if (niveau == null) return ResponseEntity.badRequest().body(Map.of("erreur", "niveauAutorisation requis"));
        Utilisateur u = utilisateurRepository.findById(idUtilisateur).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        Autorisation aut = autorisationRepository.findByUtilisateurId(idUtilisateur)
                .orElseGet(() -> { Autorisation a = new Autorisation(); a.setUtilisateur(u); return a; });
        aut.setNiveauAutorisation(niveau);
        return ResponseEntity.ok(autorisationRepository.save(aut));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. SUSPENSIONS DE CONNEXION
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/suspensions")
    public ResponseEntity<?> listerSuspensions() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(suspensionConnexionRepository.findAll());
    }

    @PostMapping("/suspensions/{idUtilisateur}/debloquer")
    public ResponseEntity<?> debloquer(@PathVariable Integer idUtilisateur) {
        if (!estAdmin()) return interdit();
        Optional<SuspensionConnexion> opt = suspensionConnexionRepository.findByUtilisateurId(idUtilisateur);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SuspensionConnexion s = opt.get();
        s.reinitialiser();
        suspensionConnexionRepository.save(s);
        return ResponseEntity.ok(Map.of("message", "Compte débloqué"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  5. ACTIONS TRAÇABLES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/actions")
    public ResponseEntity<?> listerActions() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(actionRepository.findAll());
    }

    @PostMapping("/actions")
    public ResponseEntity<?> creerAction(@RequestBody Action action) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(actionRepository.save(action));
    }

    @PutMapping("/actions/{id}")
    public ResponseEntity<?> modifierAction(@PathVariable Integer id, @RequestBody Action body) {
        if (!estAdmin()) return interdit();
        Optional<Action> opt = actionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Action a = opt.get();
        if (body.getNom() != null) a.setNom(body.getNom());
        if (body.getDescription() != null) a.setDescription(body.getDescription());
        return ResponseEntity.ok(actionRepository.save(a));
    }

    @DeleteMapping("/actions/{id}")
    public ResponseEntity<?> supprimerAction(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!actionRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("action", id), "Action");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  6. AGENCES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/agences")
    public ResponseEntity<?> listerAgences() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(agenceRepository.findAll());
    }

    @PostMapping("/agences")
    public ResponseEntity<?> creerAgence(@RequestBody Agence agence) {
        if (!estAdmin()) return interdit();
        agence.setDateCreation(LocalDateTime.now());
        return ResponseEntity.ok(agenceRepository.save(agence));
    }

    @PutMapping("/agences/{id}")
    public ResponseEntity<?> modifierAgence(@PathVariable Integer id, @RequestBody Agence body) {
        if (!estAdmin()) return interdit();
        Optional<Agence> opt = agenceRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Agence a = opt.get();
        if (body.getNom() != null) a.setNom(body.getNom());
        if (body.getAdresse() != null) a.setAdresse(body.getAdresse());
        if (body.getVille() != null) a.setVille(body.getVille());
        if (body.getCpClient() != null) a.setCpClient(body.getCpClient());
        if (body.getCode() != null) a.setCode(body.getCode());
        return ResponseEntity.ok(agenceRepository.save(a));
    }

    @DeleteMapping("/agences/{id}")
    public ResponseEntity<?> supprimerAgence(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!agenceRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("agence", id), "Agence");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7. BUREAUX DE DOUANE
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/bureaux")
    public ResponseEntity<?> listerBureaux() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(bureauRepository.findAll());
    }

    @PostMapping("/bureaux")
    public ResponseEntity<?> creerBureau(@RequestBody Bureau bureau) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(bureauRepository.save(bureau));
    }

    @PutMapping("/bureaux/{id}")
    public ResponseEntity<?> modifierBureau(@PathVariable Integer id, @RequestBody Bureau body) {
        if (!estAdmin()) return interdit();
        Optional<Bureau> opt = bureauRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Bureau b = opt.get();
        if (body.getNom() != null) b.setNom(body.getNom());
        if (body.getCodeBureau() != null) b.setCodeBureau(body.getCodeBureau());
        if (body.getAdresse() != null) b.setAdresse(body.getAdresse());
        if (body.getVille() != null) b.setVille(body.getVille());
        if (body.getDescription() != null) b.setDescription(body.getDescription());
        return ResponseEntity.ok(bureauRepository.save(b));
    }

    @DeleteMapping("/bureaux/{id}")
    public ResponseEntity<?> supprimerBureau(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!bureauRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("bureau", id), "Bureau");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  8. MODES DE TRANSPORT
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/modes-transport")
    public ResponseEntity<?> listerModesTransport() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(modeTransportRepository.findAll());
    }

    @PostMapping("/modes-transport")
    public ResponseEntity<?> creerModeTransport(@RequestBody ModeTransport mt) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(modeTransportRepository.save(mt));
    }

    @PutMapping("/modes-transport/{id}")
    public ResponseEntity<?> modifierModeTransport(@PathVariable Integer id, @RequestBody ModeTransport body) {
        if (!estAdmin()) return interdit();
        Optional<ModeTransport> opt = modeTransportRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        ModeTransport mt = opt.get();
        if (body.getNom() != null) mt.setNom(body.getNom());
        if (body.getDescription() != null) mt.setDescription(body.getDescription());
        return ResponseEntity.ok(modeTransportRepository.save(mt));
    }

    @DeleteMapping("/modes-transport/{id}")
    public ResponseEntity<?> supprimerModeTransport(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!modeTransportRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("mode_transport", id), "Mode de transport");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  9. TYPES DE PRODUCTION
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/types-production")
    public ResponseEntity<?> listerTypesProduction() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeProductionRepository.findAll());
    }

    @PostMapping("/types-production")
    public ResponseEntity<?> creerTypeProduction(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            TypeProduction tp = new TypeProduction();
            tp.setNom((String) body.get("nom"));
            tp.setCode(parseId(body.get("code")));
            tp.setDescription((String) body.get("description"));

            Integer idMt = parseId(body.get("idModeTransport"));
            if (idMt != null) modeTransportRepository.findById(idMt).ifPresent(tp::setModeTransport);

            return ResponseEntity.ok(typeProductionRepository.save(tp));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/types-production/{id}")
    public ResponseEntity<?> modifierTypeProduction(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<TypeProduction> opt = typeProductionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        TypeProduction tp = opt.get();
        if (body.containsKey("nom")) tp.setNom((String) body.get("nom"));
        if (body.containsKey("code")) tp.setCode(parseId(body.get("code")));
        if (body.containsKey("description")) tp.setDescription((String) body.get("description"));

        if (body.containsKey("idModeTransport")) {
            Integer idMt = parseId(body.get("idModeTransport"));
            tp.setModeTransport(idMt != null ? modeTransportRepository.findById(idMt).orElse(null) : null);
        }
        return ResponseEntity.ok(typeProductionRepository.save(tp));
    }

    @DeleteMapping("/types-production/{id}")
    public ResponseEntity<?> supprimerTypeProduction(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!typeProductionRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("type_production", id), "Type de production");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  10. TYPES DE RÉGIME DOUANIER
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/types-regime")
    public ResponseEntity<?> listerTypesRegime() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeRegimeRepository.findAll());
    }

    @PostMapping("/types-regime")
    public ResponseEntity<?> creerTypeRegime(@RequestBody TypeRegime tr) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeRegimeRepository.save(tr));
    }

    @PutMapping("/types-regime/{id}")
    public ResponseEntity<?> modifierTypeRegime(@PathVariable Integer id, @RequestBody TypeRegime body) {
        if (!estAdmin()) return interdit();
        Optional<TypeRegime> opt = typeRegimeRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        TypeRegime tr = opt.get();
        if (body.getNom() != null) tr.setNom(body.getNom());
        if (body.getCode() != null) tr.setCode(body.getCode());
        if (body.getDescription() != null) tr.setDescription(body.getDescription());
        return ResponseEntity.ok(typeRegimeRepository.save(tr));
    }

    @DeleteMapping("/types-regime/{id}")
    public ResponseEntity<?> supprimerTypeRegime(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!typeRegimeRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("type_regime", id), "Type de régime");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  11. PHASES DOSSIER
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/phases-dossier")
    public ResponseEntity<?> listerPhasesDossier() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(phaseDossierRepository.findAll());
    }

    @PostMapping("/phases-dossier")
    public ResponseEntity<?> creerPhaseDossier(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            PhaseDossier pd = new PhaseDossier();
            pd.setNom((String) body.get("nom"));
            pd.setDescription((String) body.get("description"));

            Integer idMt = parseId(body.get("idModeTransport"));
            if (idMt != null) modeTransportRepository.findById(idMt).ifPresent(pd::setModeTransport);

            return ResponseEntity.ok(phaseDossierRepository.save(pd));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/phases-dossier/{id}")
    public ResponseEntity<?> modifierPhaseDossier(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<PhaseDossier> opt = phaseDossierRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        PhaseDossier pd = opt.get();
        if (body.containsKey("nom")) pd.setNom((String) body.get("nom"));
        if (body.containsKey("description")) pd.setDescription((String) body.get("description"));

        if (body.containsKey("idModeTransport")) {
            Integer idMt = parseId(body.get("idModeTransport"));
            pd.setModeTransport(idMt != null ? modeTransportRepository.findById(idMt).orElse(null) : null);
        }
        return ResponseEntity.ok(phaseDossierRepository.save(pd));
    }

    @DeleteMapping("/phases-dossier/{id}")
    public ResponseEntity<?> supprimerPhaseDossier(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!phaseDossierRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("phase_dossier", id), "Phase dossier");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  12. STATUTS DOSSIER
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/statuts-dossier")
    public ResponseEntity<?> listerStatutsDossier() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(statutDossierRepository.findAll());
    }

    @PostMapping("/statuts-dossier")
    public ResponseEntity<?> creerStatutDossier(@RequestBody StatutDossier sd) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(statutDossierRepository.save(sd));
    }

    @PutMapping("/statuts-dossier/{id}")
    public ResponseEntity<?> modifierStatutDossier(@PathVariable Integer id, @RequestBody StatutDossier body) {
        if (!estAdmin()) return interdit();
        Optional<StatutDossier> opt = statutDossierRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StatutDossier sd = opt.get();
        if (body.getNom() != null) sd.setNom(body.getNom());
        if (body.getDescription() != null) sd.setDescription(body.getDescription());
        return ResponseEntity.ok(statutDossierRepository.save(sd));
    }

    @DeleteMapping("/statuts-dossier/{id}")
    public ResponseEntity<?> supprimerStatutDossier(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!statutDossierRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("statut_dossier", id), "Statut dossier");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  13. STATUTS OT
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/statuts-ot")
    public ResponseEntity<?> listerStatutsOt() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(statutOtRepository.findAll());
    }

    @PostMapping("/statuts-ot")
    public ResponseEntity<?> creerStatutOt(@RequestBody StatutOt so) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(statutOtRepository.save(so));
    }

    @PutMapping("/statuts-ot/{id}")
    public ResponseEntity<?> modifierStatutOt(@PathVariable Integer id, @RequestBody StatutOt body) {
        if (!estAdmin()) return interdit();
        Optional<StatutOt> opt = statutOtRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StatutOt so = opt.get();
        if (body.getNom() != null) so.setNom(body.getNom());
        if (body.getDescription() != null) so.setDescription(body.getDescription());
        return ResponseEntity.ok(statutOtRepository.save(so));
    }

    @DeleteMapping("/statuts-ot/{id}")
    public ResponseEntity<?> supprimerStatutOt(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!statutOtRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("statut_ot", id), "Statut OT");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  14. INCOTERMES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/incotermes")
    public ResponseEntity<?> listerIncotermes() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(incontermeRepository.findAll());
    }

    @PostMapping("/incotermes")
    public ResponseEntity<?> creerIncoterme(@RequestBody Inconterme inc) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(incontermeRepository.save(inc));
    }

    @PutMapping("/incotermes/{id}")
    public ResponseEntity<?> modifierIncoterme(@PathVariable Integer id, @RequestBody Inconterme body) {
        if (!estAdmin()) return interdit();
        Optional<Inconterme> opt = incontermeRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Inconterme inc = opt.get();
        if (body.getNom() != null) inc.setNom(body.getNom());
        if (body.getAbreviation() != null) inc.setAbreviation(body.getAbreviation());
        if (body.getDescription() != null) inc.setDescription(body.getDescription());
        return ResponseEntity.ok(incontermeRepository.save(inc));
    }

    @DeleteMapping("/incotermes/{id}")
    public ResponseEntity<?> supprimerIncoterme(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!incontermeRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("inconterme", id), "Incoterme");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  15. NATURES DE MARCHANDISE
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/natures-marchandise")
    public ResponseEntity<?> listerNaturesMarchandise() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(natureMarchandiseRepository.findAll());
    }

    @PostMapping("/natures-marchandise")
    public ResponseEntity<?> creerNatureMarchandise(@RequestBody NatureMarchandise nm) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(natureMarchandiseRepository.save(nm));
    }

    @PutMapping("/natures-marchandise/{id}")
    public ResponseEntity<?> modifierNatureMarchandise(@PathVariable Integer id, @RequestBody NatureMarchandise body) {
        if (!estAdmin()) return interdit();
        Optional<NatureMarchandise> opt = natureMarchandiseRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        NatureMarchandise nm = opt.get();
        if (body.getNom() != null) nm.setNom(body.getNom());
        return ResponseEntity.ok(natureMarchandiseRepository.save(nm));
    }

    @DeleteMapping("/natures-marchandise/{id}")
    public ResponseEntity<?> supprimerNatureMarchandise(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!natureMarchandiseRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("nature_marchandise", id), "Nature de marchandise");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  16. UNITÉS DE MESURE
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/unites-mesure")
    public ResponseEntity<?> listerUnitesMesure() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(uniteMesureRepository.findAll());
    }

    @PostMapping("/unites-mesure")
    public ResponseEntity<?> creerUniteMesure(@RequestBody UniteMesure um) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(uniteMesureRepository.save(um));
    }

    @PutMapping("/unites-mesure/{id}")
    public ResponseEntity<?> modifierUniteMesure(@PathVariable Integer id, @RequestBody UniteMesure body) {
        if (!estAdmin()) return interdit();
        Optional<UniteMesure> opt = uniteMesureRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        UniteMesure um = opt.get();
        if (body.getNom() != null) um.setNom(body.getNom());
        if (body.getRapportMCube() != null) um.setRapportMCube(body.getRapportMCube());
        if (body.getDescription() != null) um.setDescription(body.getDescription());
        return ResponseEntity.ok(uniteMesureRepository.save(um));
    }

    @DeleteMapping("/unites-mesure/{id}")
    public ResponseEntity<?> supprimerUniteMesure(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!uniteMesureRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("unite_mesure", id), "Unité de mesure");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  17. TYPES DE CONTENEUR
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/types-container")
    public ResponseEntity<?> listerTypesContainer() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeContainerRepository.findAll());
    }

    @PostMapping("/types-container")
    public ResponseEntity<?> creerTypeContainer(@RequestBody TypeContainer tc) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeContainerRepository.save(tc));
    }

    @PutMapping("/types-container/{id}")
    public ResponseEntity<?> modifierTypeContainer(@PathVariable Integer id, @RequestBody TypeContainer body) {
        if (!estAdmin()) return interdit();
        Optional<TypeContainer> opt = typeContainerRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        TypeContainer tc = opt.get();
        if (body.getNom() != null) tc.setNom(body.getNom());
        if (body.getDescription() != null) tc.setDescription(body.getDescription());
        if (body.getLongueur() != null) tc.setLongueur(body.getLongueur());
        if (body.getLargeur() != null) tc.setLargeur(body.getLargeur());
        if (body.getHauteur() != null) tc.setHauteur(body.getHauteur());
        if (body.getPoidsMax() != null) tc.setPoidsMax(body.getPoidsMax());
        if (body.getVolume() != null) tc.setVolume(body.getVolume());
        return ResponseEntity.ok(typeContainerRepository.save(tc));
    }

    @DeleteMapping("/types-container/{id}")
    public ResponseEntity<?> supprimerTypeContainer(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!typeContainerRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("type_container", id), "Type de conteneur");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  18. TYPES DE DOCUMENT
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/types-document")
    public ResponseEntity<?> listerTypesDocument() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(typeDocumentRepository.findAll());
    }

    @PostMapping("/types-document")
    public ResponseEntity<?> creerTypeDocument(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            TypeDocument td = new TypeDocument();
            td.setNom((String) body.get("nom"));
            td.setDescription((String) body.get("description"));
            Boolean oblig = (Boolean) body.get("estObligatoire");
            td.setEstObligatoire(oblig != null ? oblig : false);

            Integer idTp = parseId(body.get("idTypeProduction"));
            if (idTp != null) typeProductionRepository.findById(idTp).ifPresent(td::setTypeProduction);

            Integer idNm = parseId(body.get("idNatureMarchandise"));
            if (idNm != null) natureMarchandiseRepository.findById(idNm).ifPresent(td::setNatureMarchandise);

            return ResponseEntity.ok(typeDocumentRepository.save(td));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/types-document/{id}")
    public ResponseEntity<?> modifierTypeDocument(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<TypeDocument> opt = typeDocumentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        TypeDocument td = opt.get();
        if (body.containsKey("nom")) td.setNom((String) body.get("nom"));
        if (body.containsKey("description")) td.setDescription((String) body.get("description"));
        if (body.containsKey("estObligatoire")) td.setEstObligatoire((Boolean) body.get("estObligatoire"));

        if (body.containsKey("idTypeProduction")) {
            Integer idTp = parseId(body.get("idTypeProduction"));
            td.setTypeProduction(idTp != null ? typeProductionRepository.findById(idTp).orElse(null) : null);
        }
        if (body.containsKey("idNatureMarchandise")) {
            Integer idNm = parseId(body.get("idNatureMarchandise"));
            td.setNatureMarchandise(idNm != null ? natureMarchandiseRepository.findById(idNm).orElse(null) : null);
        }
        return ResponseEntity.ok(typeDocumentRepository.save(td));
    }

    @DeleteMapping("/types-document/{id}")
    public ResponseEntity<?> supprimerTypeDocument(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!typeDocumentRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("type_document", id), "Type de document");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  19. COMPAGNIES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/compagnies")
    public ResponseEntity<?> listerCompagnies() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(compagnieRepository.findAll());
    }

    @PostMapping("/compagnies")
    public ResponseEntity<?> creerCompagnie(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            Compagnie c = new Compagnie();
            c.setNom((String) body.get("nom"));
            c.setCode((String) body.get("code"));
            c.setDescription((String) body.get("description"));

            Integer idPays = parseId(body.get("idPays"));
            if (idPays != null) paysRepository.findById(idPays).ifPresent(c::setPays);

            Integer idMt = parseId(body.get("idModeTransport"));
            if (idMt != null) modeTransportRepository.findById(idMt).ifPresent(c::setModeTransport);

            return ResponseEntity.ok(compagnieRepository.save(c));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/compagnies/{id}")
    public ResponseEntity<?> modifierCompagnie(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<Compagnie> opt = compagnieRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Compagnie c = opt.get();
        if (body.containsKey("nom")) c.setNom((String) body.get("nom"));
        if (body.containsKey("code")) c.setCode((String) body.get("code"));
        if (body.containsKey("description")) c.setDescription((String) body.get("description"));

        if (body.containsKey("idPays")) {
            Integer idPays = parseId(body.get("idPays"));
            c.setPays(idPays != null ? paysRepository.findById(idPays).orElse(null) : null);
        }
        if (body.containsKey("idModeTransport")) {
            Integer idMt = parseId(body.get("idModeTransport"));
            c.setModeTransport(idMt != null ? modeTransportRepository.findById(idMt).orElse(null) : null);
        }
        return ResponseEntity.ok(compagnieRepository.save(c));
    }

    @DeleteMapping("/compagnies/{id}")
    public ResponseEntity<?> supprimerCompagnie(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!compagnieRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("compagnie", id), "Compagnie");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  20. NAVIRES
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/navires")
    public ResponseEntity<?> listerNavires() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(navireRepository.findAll());
    }

    @PostMapping("/navires")
    public ResponseEntity<?> creerNavire(@RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            Navire n = new Navire();
            n.setNom((String) body.get("nom"));
            n.setImo((String) body.get("imo"));

            Integer idC = parseId(body.get("idCompagnie"));
            if (idC != null) compagnieRepository.findById(idC).ifPresent(n::setCompagnie);

            return ResponseEntity.ok(navireRepository.save(n));
        } catch (SuppressionConfirmationException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/navires/{id}")
    public ResponseEntity<?> modifierNavire(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        Optional<Navire> opt = navireRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Navire n = opt.get();
        if (body.containsKey("nom")) n.setNom((String) body.get("nom"));
        if (body.containsKey("imo")) n.setImo((String) body.get("imo"));

        if (body.containsKey("idCompagnie")) {
            Integer idC = parseId(body.get("idCompagnie"));
            n.setCompagnie(idC != null ? compagnieRepository.findById(idC).orElse(null) : null);
        }
        return ResponseEntity.ok(navireRepository.save(n));
    }

    @DeleteMapping("/navires/{id}")
    public ResponseEntity<?> supprimerNavire(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!navireRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("navire", id), "Navire");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  21. PAYS
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/pays")
    public ResponseEntity<?> listerPays() {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(paysRepository.findAll());
    }

    @PostMapping("/pays")
    public ResponseEntity<?> creerPays(@RequestBody Pays p) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(paysRepository.save(p));
    }

    @PutMapping("/pays/{id}")
    public ResponseEntity<?> modifierPays(@PathVariable Integer id, @RequestBody Pays body) {
        if (!estAdmin()) return interdit();
        Optional<Pays> opt = paysRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Pays p = opt.get();
        if (body.getNom() != null) p.setNom(body.getNom());
        if (body.getCode() != null) p.setCode(body.getCode());
        return ResponseEntity.ok(paysRepository.save(p));
    }

    @DeleteMapping("/pays/{id}")
    public ResponseEntity<?> supprimerPays(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        if (!paysRepository.existsById(id)) return ResponseEntity.notFound().build();
        return securiserSuppression(() -> suppressionService.supprimer("pays", id), "Pays");
    }

    // ── Opération Dossier ───────────────────────────────────────────────────

    @GetMapping("/operations-dossier")
    public ResponseEntity<?> listerOperationsDossier(@RequestParam(required = false) Integer typeProductionId) {
        if (!estAdmin()) return interdit();
        if (typeProductionId != null) {
            return ResponseEntity.ok(operationDossierService.getOperationsByTypeProduction(typeProductionId));
        }
        return ResponseEntity.ok(operationDossierService.getAllOperations());
    }

    @PostMapping("/operations-dossier")
    public ResponseEntity<?> creerOperationDossier(@RequestBody OperationDossier op) {
        if (!estAdmin()) return interdit();
        return ResponseEntity.ok(operationDossierService.saveOperation(op));
    }

    @PutMapping("/operations-dossier/{id}")
    public ResponseEntity<?> modifierOperationDossier(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        if (!estAdmin()) return interdit();
        try {
            return ResponseEntity.ok(operationDossierService.updateOperationFromMap(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }

    @DeleteMapping("/operations-dossier/{id}")
    public ResponseEntity<?> supprimerOperationDossier(@PathVariable Integer id) {
        if (!estAdmin()) return interdit();
        return securiserSuppression(() -> operationDossierService.deleteOperation(id), "Opération Dossier");
    }

    @PostMapping("/operations-dossier/batch-delete")
    public ResponseEntity<?> supprimerOperationsDossierBatch(@RequestBody List<Integer> ids) {
        if (!estAdmin()) return interdit();
        return securiserSuppression(() -> operationDossierService.deleteBatch(ids), "Opérations Dossier (Lot)");
    }

    @PutMapping("/operations-dossier/reorder")
    public ResponseEntity<?> reordonnerOperationsDossier(@RequestBody List<Integer> ids) {
        if (!estAdmin()) return interdit();
        operationDossierService.reorderOperations(ids);
        return ResponseEntity.ok(Map.of("message", "Opérations réordonnées avec succès"));
    }
}

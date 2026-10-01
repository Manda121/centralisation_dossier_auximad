package com.auximad.ai.outils;

import com.auximad.ai.AbstractOutil;
import com.auximad.ai.ContexteChat;
import com.auximad.ai.NiveauDanger;
import com.auximad.ai.OutilsArgs;
import com.auximad.ai.OutilsDate;
import com.auximad.ai.Resolveur;
import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Outils transverses : tableau de bord, référentiels et journal d'audit.
 */
public final class OutilsTableauBord {

    private OutilsTableauBord() {
    }

    private static final String FORMATE_SQL = "yyyy-MM-dd HH:mm:ss";

    // ══════════════════════════ Tableau de bord ══════════════════════════

    @Component
    public static class TableauDeBord extends AbstractOutil {

        private final DossierRepository dossierRepository;
        private final ClientRepository clientRepository;
        private final OrdreTransitRepository ordreTransitRepository;
        private final ConteneurRepository conteneurRepository;
        private final MarchandiseRepository marchandiseRepository;
        private final DemandeInteragenceRepository demandeRepository;
        private final RemiseRepository remiseRepository;
        private final UtilisateurRepository utilisateurRepository;

        public TableauDeBord(DossierRepository dossierRepository,
                              ClientRepository clientRepository,
                              OrdreTransitRepository ordreTransitRepository,
                              ConteneurRepository conteneurRepository,
                              MarchandiseRepository marchandiseRepository,
                              DemandeInteragenceRepository demandeRepository,
                              RemiseRepository remiseRepository,
                              UtilisateurRepository utilisateurRepository) {
            super("tableau_de_bord",
                    "Retourne l'état chiffré et à jour de l'application : nombre de dossiers, clients, "
                            + "ordres de transit, conteneurs, marchandises, dossiers incomplets, demandes "
                            + "inter-agences en attente et remises à venir, avec la répartition par phase, "
                            + "statut et agence. Utilise cet outil pour toute question de type « combien de… », "
                            + "« quel est l'état des lieux », « résumé de la situation ».",
                    schema(Map.of(), new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.dossierRepository = dossierRepository;
            this.clientRepository = clientRepository;
            this.ordreTransitRepository = ordreTransitRepository;
            this.conteneurRepository = conteneurRepository;
            this.marchandiseRepository = marchandiseRepository;
            this.demandeRepository = demandeRepository;
            this.remiseRepository = remiseRepository;
            this.utilisateurRepository = utilisateurRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Consulter le tableau de bord";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Integer agenceId = contexte.peutVoirToutesAgences() ? null : contexte.idAgence();
            return synthese(agenceId, contexte);
        }

        @Transactional(readOnly = true)
        public String synthese(Integer agenceId, ContexteChat contexte) {
            LocalDateTime maintenant = LocalDateTime.now();
            StringBuilder sb = new StringBuilder("Situation au ").append(OutilsDate.lisible(maintenant)).append(".\n");

            sb.append("Périmètre : ").append(contexte.peutVoirToutesAgences()
                    ? "toutes les agences" : "agence " + contexte.nomAgence()).append("\n\n");

            sb.append("VOLUMES\n");
            sb.append("- Dossiers : ").append(dossierRepository.count()).append(" au total");
            if (agenceId != null) {
                sb.append(" (agence : ").append(dossierRepository.compterDossiersDepuis(agenceId, "1900-01-01 00:00:00")).append(")");
            }
            sb.append("\n- Clients : ").append(clientRepository.count())
                    .append("\n- Ordres de transit : ").append(ordreTransitRepository.count())
                    .append("\n- Conteneurs : ").append(conteneurRepository.count())
                    .append("\n- Marchandises : ").append(marchandiseRepository.count())
                    .append("\n- Utilisateurs : ").append(utilisateurRepository.count()).append("\n");

            sb.append("\nACTIVITE RECENTE\n");
            sb.append("- Dossiers créés aujourd'hui : ").append(dossierRepository.compterDossiersDepuis(agenceId, maintenant.toLocalDate().atStartOfDay().format(DateTimeFormatter.ofPattern(FORMATE_SQL)))).append("\n");
            sb.append("- Dossiers créés ce mois-ci : ").append(dossierRepository.compterDossiersDepuis(agenceId, maintenant.toLocalDate().withDayOfMonth(1).atStartOfDay().format(DateTimeFormatter.ofPattern(FORMATE_SQL)))).append("\n");
            sb.append("- Dossiers créés sur 30 jours : ").append(dossierRepository.compterDossiersDepuis(agenceId, maintenant.minusDays(30).format(DateTimeFormatter.ofPattern(FORMATE_SQL)))).append("\n");

            sb.append("\nDOSSIERS PAR PHASE\n");
            for (Object[] ligne : dossierRepository.compterDossiersParPhase(agenceId)) {
                sb.append("- ").append(ligne[0]).append(" : ").append(ligne[1]).append("\n");
            }

            sb.append("\nDOSSIERS PAR STATUT\n");
            for (Object[] ligne : dossierRepository.compterDossiersParStatut(agenceId)) {
                sb.append("- ").append(ligne[0]).append(" : ").append(ligne[1]).append("\n");
            }

            if (contexte.peutVoirToutesAgences()) {
                sb.append("\nDOSSIERS PAR AGENCE\n");
                for (Object[] ligne : dossierRepository.compterDossiersParAgence()) {
                    sb.append("- ").append(ligne[0]).append(" : ").append(ligne[1]).append("\n");
                }
            }

            sb.append("\nPOINTS DE VIGILANCE\n");
            long incomplets = dossierRepository.compterDossiersIncomplets(agenceId);
            sb.append("- Dossiers avec document obligatoire manquant : ").append(incomplets).append("\n");

            long demandesEnAttente = demandeRepository.count();
            sb.append("- Demandes inter-agences (toutes agences) : ").append(demandesEnAttente).append("\n");
            if (contexte.idAgence() != null) {
                sb.append("- Demandes inter-agences en attente pour votre agence : ")
                        .append(demandeRepository.countByAgenceDestinataireIdAndStatutAndEstVueFalse(contexte.idAgence(), "EN_ATTENTE"))
                        .append("\n");
            }

            List<Remise> urgentes = agenceId != null
                    ? remiseRepository.findRemisesUrgentesParAgence(maintenant, maintenant.plusDays(7), agenceId)
                    : remiseRepository.findRemisesUrgentes(maintenant, maintenant.plusDays(7));
            sb.append("- Remises de documents attendues sous 7 jours : ").append(urgentes.size()).append("\n");
            urgentes.stream().limit(10).forEach(r -> sb.append("    · dossier ")
                    .append(r.getDossier() != null ? r.getDossier().getNumeroDossier() : "?")
                    .append(" — ").append(r.getTypeDocument().getNom())
                    .append(" — remise le ").append(OutilsDate.lisible(r.getDateRemise())).append("\n"));

            return sb.toString();
        }
    }

    // ══════════════════════════ Référentiels ══════════════════════════

    @Component
    public static class ListerReferentiels extends AbstractOutil {

        private final PhaseDossierRepository phaseRepository;
        private final StatutDossierRepository statutRepository;
        private final AgenceRepository agenceRepository;
        private final TypeProductionRepository typeProductionRepository;
        private final TypeRegimeRepository typeRegimeRepository;
        private final TypeDocumentRepository typeDocumentRepository;
        private final NatureMarchandiseRepository natureRepository;
        private final UniteMesureRepository uniteRepository;
        private final PaysRepository paysRepository;
        private final BureauRepository bureauRepository;
        private final IncontermeRepository incontermeRepository;
        private final ConteneurRepository conteneurRepository;
        private final TypeContainerRepository typeConteneurRepository;
        private final UtilisateurRepository utilisateurRepository;
        private final RoleRepository roleRepository;
        private final OperationDossierRepository operationRepository;

        public ListerReferentiels(PhaseDossierRepository phaseRepository,
                                  StatutDossierRepository statutRepository,
                                  AgenceRepository agenceRepository,
                                  TypeProductionRepository typeProductionRepository,
                                  TypeRegimeRepository typeRegimeRepository,
                                  TypeDocumentRepository typeDocumentRepository,
                                  NatureMarchandiseRepository natureRepository,
                                  UniteMesureRepository uniteRepository,
                                  PaysRepository paysRepository,
                                  BureauRepository bureauRepository,
                                  IncontermeRepository incontermeRepository,
                                  ConteneurRepository conteneurRepository,
                                  TypeContainerRepository typeConteneurRepository,
                                  UtilisateurRepository utilisateurRepository,
                                  RoleRepository roleRepository,
                                  OperationDossierRepository operationRepository) {
            super("lister_referentiels",
                    "Liste les valeurs d'un référentiel de l'application. Utilise cet outil AVANT toute "
                            + "opération qui réclame un nom existant (changer une phase, un statut, un type de "
                            + "document…) afin de proposer des valeurs réellement présentes en base. "
                            + "Types acceptés : phases, statuts, agences, types_production, documents, regimes, "
                            + "natures, unites, pays, bureaux, incoterms, types_conteneur, conteneurs, roles, "
                            + "utilisateurs, operations.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("type", OutilsArgs.propEnum(
                                    "Le référentiel à lister.",
                                    "phases", "statuts", "agences", "types_production", "documents", "regimes",
                                    "natures", "unites", "pays", "bureaux", "incoterms", "types_conteneur",
                                    "conteneurs", "roles", "utilisateurs", "operations"))),
                            "type"),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.phaseRepository = phaseRepository;
            this.statutRepository = statutRepository;
            this.agenceRepository = agenceRepository;
            this.typeProductionRepository = typeProductionRepository;
            this.typeRegimeRepository = typeRegimeRepository;
            this.typeDocumentRepository = typeDocumentRepository;
            this.natureRepository = natureRepository;
            this.uniteRepository = uniteRepository;
            this.paysRepository = paysRepository;
            this.bureauRepository = bureauRepository;
            this.incontermeRepository = incontermeRepository;
            this.conteneurRepository = conteneurRepository;
            this.typeConteneurRepository = typeConteneurRepository;
            this.utilisateurRepository = utilisateurRepository;
            this.roleRepository = roleRepository;
            this.operationRepository = operationRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Lister le référentiel « " + OutilsArgs.texte(arguments, "type", "?") + " »";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            String type = OutilsArgs.texteObligatoire(arguments, "type")
                    .toLowerCase().replace(' ', '_').replace('-', '_');

            List<String> lignes = new ArrayList<>();
            switch (type) {
                case "phases", "phase" -> phaseRepository.findAll().forEach(p ->
                        lignes.add(ligne(p.getId(), p.getNom(), p.getDescription())));
                case "statuts", "statut" -> statutRepository.findAll().forEach(s ->
                        lignes.add(ligne(s.getId(), s.getNom(), s.getDescription())));
                case "agences", "agence" -> agenceRepository.findAll().forEach(a ->
                        lignes.add(ligne(a.getId(), a.getNom(),
                                "code " + a.getCode() + (a.getVille() != null ? ", " + a.getVille() : ""))));
                case "types_production", "type_production" -> typeProductionRepository.findAll().forEach(t ->
                        lignes.add(ligne(t.getId(), t.getNom(),
                                "code " + t.getCode() + (t.getModeTransport() != null ? ", " + t.getModeTransport().getNom() : ""))));
                case "documents", "types_document", "type_document", "typedocument" -> typeDocumentRepository.findAll().forEach(t ->
                        lignes.add(ligne(t.getId(), t.getNom(),
                                (Boolean.TRUE.equals(t.getEstObligatoire()) ? "obligatoire" : "facultatif")
                                        + (t.getTypeProduction() != null ? ", type de production : " + t.getTypeProduction().getNom() : "")
                                        + (t.getDescription() != null ? ", " + t.getDescription() : ""))));
                case "regimes", "types_regime", "type_regime" -> typeRegimeRepository.findAll().forEach(t ->
                        lignes.add(ligne(t.getId(), t.getNom(), "code " + t.getCode())));
                case "natures", "nature_marchandise", "natures_marchandise" -> natureRepository.findAll().forEach(n ->
                        lignes.add(ligne(n.getId(), n.getNom(), n.getDescription())));
                case "unites", "unites_mesure", "unite_mesure" -> uniteRepository.findAll().forEach(u ->
                        lignes.add(ligne(u.getId(), u.getNom(), u.getDescription())));
                case "pays" -> paysRepository.findAll().forEach(p ->
                        lignes.add(ligne(p.getId(), p.getNom(), "code " + p.getCode())));
                case "bureaux", "bureau" -> bureauRepository.findAll().forEach(b ->
                        lignes.add(ligne(b.getId(), b.getNom(),
                                (b.getVille() != null ? b.getVille() : "") + (b.getCodeBureau() != null ? " (" + b.getCodeBureau() + ")" : ""))));
                case "incoterms", "incoterm", "incontermes" -> incontermeRepository.findAll().forEach(i ->
                        lignes.add(ligne(i.getId(), i.getAbreviation() + " — " + i.getNom(), i.getDescription())));
                case "types_conteneur", "type_conteneur" -> typeConteneurRepository.findAll().forEach(t ->
                        lignes.add(ligne(t.getId(), t.getNom(),
                                "volume " + t.getVolume() + ", poids max " + t.getPoidsMax())));
                case "conteneurs", "conteneur" -> conteneurRepository.findAll().forEach(c ->
                        lignes.add(ligne(c.getId(), c.getNumeroContainer(),
                                c.getTypeContainer() != null ? c.getTypeContainer().getNom() : null)));
                case "roles", "role" -> roleRepository.findAll().forEach(r ->
                        lignes.add(ligne(r.getId(), r.getNom(), r.getDescription())));
                case "utilisateurs", "utilisateur" -> utilisateurRepository.findAll().forEach(u ->
                        lignes.add(ligne(u.getId(), u.getNomUtilisateur(),
                                u.getNomComplet() + ", rôle " + (u.getRole() != null ? u.getRole().getNom() : "-")
                                        + ", agence " + (u.getAgence() != null ? u.getAgence().getNom() : "-"))));
                case "operations", "operations_dossier" -> operationRepository.findAll().forEach(o ->
                        lignes.add(ligne(o.getId(), o.getDescription(),
                                "niveau " + o.getNiveauOperation() + ", validation " + o.getNiveauValidation()
                                        + (o.getPhaseDossier() != null ? ", phase " + o.getPhaseDossier().getNom() : ""))));
                default -> throw new IllegalArgumentException("Type de référentiel inconnu : " + type);
            }

            if (lignes.isEmpty()) {
                return "Le référentiel « " + type + " » est vide.";
            }
            return "Référentiel « " + type + " » (" + lignes.size() + " valeurs) :\n- "
                    + String.join("\n- ", lignes);
        }

        private String ligne(Integer id, String nom, String precision) {
            return (id != null ? "#" + id + " " : "") + (nom == null ? "(sans nom)" : nom)
                    + (precision != null && !precision.isBlank() ? " — " + precision : "");
        }
    }

    // ══════════════════════════ Historique d'audit ══════════════════════════

    @Component
    public static class RechercherHistorique extends AbstractOutil {

        private final HistoriqueRepository historiqueRepository;
        private final UtilisateurRepository utilisateurRepository;
        private final ActionRepository actionRepository;

        public RechercherHistorique(HistoriqueRepository historiqueRepository,
                                    UtilisateurRepository utilisateurRepository,
                                    ActionRepository actionRepository) {
            super("rechercher_historique",
                    "Consulte le journal d'audit : qui a fait quoi, quand et depuis quelle machine. "
                            + "Utilise cet outil pour les questions sur l'historique, les traces, l'activité "
                            + "d'un utilisateur, les connexions, ou pour vérifier qu'une action a bien été "
                            + "enregistrée.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("utilisateur", str("Nom d'utilisateur ou identifiant, ex: « ramanana » ou 7.")),
                            OutilsArgs.p("action", str("Type d'action : VISUALISATION, CREATION, MODIFICATION, SUPPRESSION, VALIDATION, CONNEXION, DECONNEXION.")),
                            OutilsArgs.p("jours", entierProp("Période en jours à partir d'aujourd'hui. Par défaut 7.")),
                            OutilsArgs.p("limite", entierProp("Nombre maximum de lignes à renvoyer (max 50)."))),
                            new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.historiqueRepository = historiqueRepository;
            this.utilisateurRepository = utilisateurRepository;
            this.actionRepository = actionRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Consulter le journal d'audit";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Integer utilisateurId = OutilsArgs.entier(arguments, "utilisateur");
            if (utilisateurId == null) {
                String nom = OutilsArgs.texte(arguments, "utilisateur");
                if (nom != null) {
                    utilisateurId = utilisateurRepository.findByNomUtilisateur(nom)
                            .map(Utilisateur::getId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Aucun utilisateur nommé « " + nom + " ». Demande la liste exacte via lister_referentiels type=utilisateurs."));
                }
            }

            Integer actionId = null;
            String action = OutilsArgs.texte(arguments, "action");
            if (action != null) {
                actionId = actionRepository.findByNom(action.toUpperCase().trim())
                        .map(Action::getId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Type d'action inconnu : « " + action + " ». Valeurs possibles : VISUALISATION, "
                                        + "CREATION, MODIFICATION, SUPPRESSION, VALIDATION, CONNEXION, DECONNEXION, "
                                        + "CONNEXION_ECHOUEE."));
            }

            int jours = OutilsArgs.entier(arguments, "jours", 7);
            int limite = Math.min(Math.max(OutilsArgs.entier(arguments, "limite", 20), 1), 50);
            LocalDateTime depuis = LocalDateTime.now().minusDays(Math.max(jours, 1));

            Page<Historique> page = historiqueRepository.searchHistoriques(
                    null, utilisateurId, actionId, null, null, null, null, null, depuis, null,
                    PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "dateAction")));

            if (page.getNumberOfElements() == 0) {
                return "Aucune entrée d'audit ne correspond sur les " + jours + " derniers jours"
                        + (utilisateurId != null ? " pour l'utilisateur #" + utilisateurId : "")
                        + (actionId != null ? " avec l'action demandée" : "") + ".";
            }

            StringBuilder sb = new StringBuilder("Journal d'audit : ").append(page.getTotalElements())
                    .append(" entrée(s) sur les ").append(jours).append(" derniers jours")
                    .append(", ").append(page.getNumberOfElements()).append(" affichée(s) :\n");
            for (Historique h : page.getContent()) {
                sb.append("- ").append(OutilsDate.lisible(h.getDateAction()))
                        .append(" | ").append(h.getUtilisateur() != null ? h.getUtilisateur().getNomUtilisateur() : "anonyme")
                        .append(" | ").append(h.getAction() != null ? h.getAction().getNom() : "?")
                        .append(" | ").append(h.getEntite() == null ? "" : h.getEntite())
                        .append(" | ").append(h.getDetails() == null ? "" : h.getDetails());
                if (h.getLogiciel() != null) {
                    sb.append(" | depuis ").append(h.getLogiciel());
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }
}
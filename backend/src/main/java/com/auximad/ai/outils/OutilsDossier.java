package com.auximad.ai.outils;

import com.auximad.ai.AbstractOutil;
import com.auximad.ai.ContexteChat;
import com.auximad.ai.NiveauDanger;
import com.auximad.ai.OutilsArgs;
import com.auximad.ai.OutilsDate;
import com.auximad.ai.RagService;
import com.auximad.ai.Resolveur;
import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.service.DocumentService;
import com.auximad.service.DossierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Outils portant sur les dossiers de transit : consultation, documents et opérations d'écriture.
 * Toutes les opérations d'écriture sont de niveau MODIFICATION ou CRITIQUE : elles ne sont
 * exécutées qu'après confirmation explicite de l'utilisateur.
 */
public final class OutilsDossier {

    private OutilsDossier() {
    }

    private static final String DESC_REFERENCE =
            "Numéro de dossier (ex: 30-70-01-6-0001) ou identifiant numérique.";

    // ══════════════════════════ Lecture ══════════════════════════

    @Component
    public static class RechercherDossiers extends AbstractOutil {

        private final DossierRepository dossierRepository;
        private final PhaseDossierRepository phaseRepository;
        private final StatutDossierRepository statutRepository;

        public RechercherDossiers(DossierRepository dossierRepository,
                                  PhaseDossierRepository phaseRepository,
                                  StatutDossierRepository statutRepository) {
            super("rechercher_dossiers",
                    "Recherche des dossiers correspondant à des critères : texte libre (numéro, client, "
                            + "pour compte, départ), phase, statut, type de dossier. Utilise cet outil pour "
                            + "trouver un ou plusieurs dossiers, jamais pour lire le détail d'un dossier connu "
                            + "(utilise obtenir_dossier pour cela).",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("recherche", str("Texte libre : nom du client, pour compte, lieu de départ, partie du numéro.")),
                            OutilsArgs.p("phase", str("Nom exact de la phase, ex: « En transit ». Utilise lister_referentiels type=phases pour connaître les valeurs.")),
                            OutilsArgs.p("statut", str("Nom exact du statut, ex: « Ouvert ».")),
                            OutilsArgs.p("type", strEnum("Filtrer sur le type de dossier : MERE, SOUS_DOSSIER ou null pour tous.")),
                            OutilsArgs.p("limite", entierProp("Nombre maximum de dossiers à renvoyer (max 50)."))),
                            new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.dossierRepository = dossierRepository;
            this.phaseRepository = phaseRepository;
            this.statutRepository = statutRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Rechercher des dossiers";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Integer agenceId = contexte.peutVoirToutesAgences() ? null : contexte.idAgence();
            String recherche = OutilsArgs.texte(arguments, "recherche");
            Integer phaseId = null;
            Integer statutId = null;

            String nomPhase = OutilsArgs.texte(arguments, "phase");
            if (nomPhase != null) {
                PhaseDossier phase = phaseRepository.findAll().stream()
                        .filter(p -> equalsIgnorantAccents(p.getNom(), nomPhase))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Phase inconnue : « " + nomPhase
                                + " ». Utilise lister_referentiels type=phases."));
                phaseId = phase.getId();
            }
            String nomStatut = OutilsArgs.texte(arguments, "statut");
            if (nomStatut != null) {
                StatutDossier statut = statutRepository.findAll().stream()
                        .filter(s -> equalsIgnorantAccents(s.getNom(), nomStatut))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Statut inconnu : « " + nomStatut
                                + " ». Utilise lister_referentiels type=statuts."));
                statutId = statut.getId();
            }

            String type = OutilsArgs.texte(arguments, "type");
            int limite = Math.min(Math.max(OutilsArgs.entier(arguments, "limite", 20), 1), 50);

            Page<Dossier> page = dossierRepository.searchDossiers(
                    recherche, phaseId, statutId, null, null, null, null, null, null, agenceId, type,
                    PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "date_creation")));

            if (page.getNumberOfElements() == 0) {
                return "Aucun dossier ne correspond aux critères demandés dans le périmètre accessible.";
            }

            StringBuilder sb = new StringBuilder((int) page.getTotalElements()).append(" dossier(s) correspondent :\n");
            for (Dossier d : page.getContent()) {
                sb.append("- Dossier n° ").append(d.getNumeroDossier())
                        .append(" (id ").append(d.getId()).append(")")
                        .append(" | client : ").append(Resolveur.libelle(d.getClient()))
                        .append(" | agence : ").append(d.getAgence() != null ? d.getAgence().getNom() : "-")
                        .append(" | phase : ").append(d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "-")
                        .append(" | statut : ").append(d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "-")
                        .append(" | échéance : ").append(OutilsDate.lisible(d.getEcheance()))
                        .append(" | créé le ").append(OutilsDate.lisible(d.getDateCreation()))
                        .append("\n");
            }
            return sb.toString();
        }
    }

    // ══════════════════════════ Détail d'un dossier ══════════════════════════

    @Component
    public static class ObtenirDossier extends AbstractOutil {

        private final DossierRepository dossierRepository;
        private final DocumentRepository documentRepository;
        private final RemiseRepository remiseRepository;
        private final ConteneurRepository conteneurRepository;
        private final DemandeInteragenceRepository demandeRepository;
        private final MvtDossierRepository mvtRepository;
        private final Resolveur resolveur;

        public ObtenirDossier(DossierRepository dossierRepository,
                               DocumentRepository documentRepository,
                               RemiseRepository remiseRepository,
                               ConteneurRepository conteneurRepository,
                               DemandeInteragenceRepository demandeRepository,
                               MvtDossierRepository mvtRepository,
                               Resolveur resolveur) {
            super("obtenir_dossier",
                    "Retourne la fiche complète et à jour d'UN dossier : client, agence, phase, statut, type de "
                            + "production, ordre de transit, pays, incoterm, échéance, livraison, provisions, "
                            + "documents remis, conteneurs, demandes inter-agences et derniers mouvements. "
                            + "Utilise cet outil dès que l'utilisateur cite un numéro de dossier précis.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE))),
                            "reference"),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.dossierRepository = dossierRepository;
            this.documentRepository = documentRepository;
            this.remiseRepository = remiseRepository;
            this.conteneurRepository = conteneurRepository;
            this.demandeRepository = demandeRepository;
            this.mvtRepository = mvtRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Lire le dossier " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            StringBuilder sb = new StringBuilder("Dossier n° ").append(d.getNumeroDossier())
                    .append(" (identifiant ").append(d.getId()).append(")\n");

            if (d.getClient() != null) {
                Client c = d.getClient();
                sb.append("- Client : ").append(Resolveur.libelle(c))
                        .append(" (id ").append(c.getId()).append(")");
                if (c.getNif() != null) {
                    sb.append(", NIF ").append(c.getNif());
                }
                if (c.getEmail() != null) {
                    sb.append(", ").append(c.getEmail());
                }
                sb.append("\n");
            }
            sb.append("- Agence : ").append(d.getAgence() != null ? d.getAgence().getNom() : "-").append("\n");
            sb.append("- Phase : ").append(d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "aucune").append("\n");
            sb.append("- Statut : ").append(d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "aucun").append("\n");
            sb.append("- Type de production : ").append(d.getTypeProduction() != null ? d.getTypeProduction().getNom() : "-").append("\n");
            sb.append("- Ordre de transit : ").append(d.getOrdreTransit() != null
                    ? "OT n° " + d.getOrdreTransit().getNumeroOrdreTransit() : "aucun").append("\n");
            sb.append("- Créé le ").append(OutilsDate.lisible(d.getDateCreation()))
                    .append(" par ").append(d.getUtilisateur() != null ? d.getUtilisateur().getNomUtilisateur() : "-").append("\n");
            sb.append("- Échéance : ").append(OutilsDate.lisible(d.getEcheance()))
                    .append(" | Livraison prévue : ").append(OutilsDate.lisible(d.getDateLivraison())).append("\n");
            sb.append("- Pays origine : ").append(d.getPaysOrigine() != null ? d.getPaysOrigine().getNom() : "-")
                    .append(" | Pays destination : ").append(d.getPaysDestination() != null ? d.getPaysDestination().getNom() : "-").append("\n");
            sb.append("- Incoterm : ").append(d.getInconterme() != null ? d.getInconterme().getAbreviation() : "-").append("\n");
            sb.append("- Départ : ").append(d.getDepart() == null ? "-" : d.getDepart())
                    .append(" | Lieu de paiement du fret : ").append(d.getLieuPaiementFret() == null ? "-" : d.getLieuPaiementFret()).append("\n");
            sb.append("- Pour le compte de : ").append(d.getPourCompte() == null ? "-" : d.getPourCompte()).append("\n");
            sb.append("- Groupage : ").append(Boolean.TRUE.equals(d.getEstGroupage()) ? "oui" : "non")
                    .append(" | Prépayé : ").append(Boolean.TRUE.equals(d.getEstPrepaye()) ? "oui" : "no")
                    .append(" | Provisionné : ").append(Boolean.TRUE.equals(d.getEstProvisionne()) ? "oui" : "no")
                    .append(" | Provision demandée : ").append(d.getProvisionDemandee()).append("\n");
            sb.append("- Adresse de livraison : ").append(d.getAdresseLivraison() == null ? "-" : d.getAdresseLivraison()).append("\n");

            List<Document> documents = documentRepository.findByDossierIdAndEstActifTrue(d.getId());
            List<Remise> remises = remiseRepository.findByDossierId(d.getId());
            Map<Integer, LocalDateTime> dates = new java.util.HashMap<>();
            remises.forEach(r -> dates.put(r.getTypeDocument().getId(), r.getDateRemise()));
            sb.append("- Documents remis (").append(documents.size()).append(") :\n");
            if (documents.isEmpty()) {
                sb.append("    aucun document remis\n");
            } else {
                for (Document doc : documents) {
                    sb.append("    · ").append(doc.getTypeDocument().getNom())
                            .append(Boolean.TRUE.equals(doc.getTypeDocument().getEstObligatoire()) ? " (obligatoire)" : "")
                            .append(dates.containsKey(doc.getTypeDocument().getId())
                                    ? " — remis le " + OutilsDate.lisible(dates.get(doc.getTypeDocument().getId()))
                                    : " — sans date de remise")
                            .append(doc.getNomFichier() != null ? " — fichier « " + doc.getNomFichier() + " »" : "")
                            .append("\n");
                }
            }

            List<Conteneur> conteneurs = conteneurRepository.findByDossierId(d.getId());
            sb.append("- Conteneurs (").append(conteneurs.size()).append(") : ");
            sb.append(conteneurs.isEmpty() ? "aucun"
                    : conteneurs.stream().map(c -> c.getNumeroContainer()
                    + (c.getTypeContainer() != null ? " (" + c.getTypeContainer().getNom() + ")" : "")).toList());
            sb.append("\n");

            List<DemandeInteragence> demandes = demandeRepository.findByDossierIdOrderByDateDemandeDesc(d.getId());
            sb.append("- Demandes inter-agences (").append(demandes.size()).append(") :\n");
            if (demandes.isEmpty()) {
                sb.append("    aucune\n");
            } else {
                for (DemandeInteragence di : demandes) {
                    sb.append("    · #").append(di.getId())
                            .append(" vers ").append(di.getAgenceDestinataire() != null ? di.getAgenceDestinataire().getNom() : "?")
                            .append(" — ").append(di.getStatut())
                            .append(" (demandée le ").append(OutilsDate.lisible(di.getDateDemande())).append(")")
                            .append(di.getMotif() != null ? " — motif : " + di.getMotif() : "")
                            .append("\n");
                }
            }

            List<MvtDossier> mvt = mvtRepository.findByDossierIdOrderByDateMvtAscIdAsc(d.getId());
            sb.append("- Mouvements (").append(mvt.size()).append("), les ").append(Math.min(5, mvt.size())).append(" derniers :\n");
            List<MvtDossier> derniers = mvt.size() <= 5 ? mvt : mvt.subList(mvt.size() - 5, mvt.size());
            for (MvtDossier m : derniers) {
                sb.append("    · ").append(OutilsDate.lisible(m.getDateMvt()))
                        .append(" — ").append(m.getOperation() != null ? m.getOperation().getDescription() : "opération")
                        .append(m.getAnciennePhase() != null || m.getNouvellePhase() != null
                                ? " (" + (m.getAnciennePhase() != null ? m.getAnciennePhase().getNom() : "-")
                                + " → " + (m.getNouvellePhase() != null ? m.getNouvellePhase().getNom() : "-") + ")" : "")
                        .append(" par ").append(m.getUtilisateur() != null ? m.getUtilisateur().getNomUtilisateur() : "-")
                        .append("\n");
            }

            if (Boolean.TRUE.equals(d.getEstDossierMere())) {
                sb.append("ATTENTION : ce dossier est un DOSSIÈRE MÈRE (des sous-dossiers lui sont rattachés).\n");
            }
            if (Boolean.TRUE.equals(d.getEstSousDossier())) {
                sb.append("ATTENTION : ce dossier est un SOUS-DOSSIER du dossier mère ")
                        .append(d.getNumeroDossierMere() == null ? "inconnu" : d.getNumeroDossierMere()).append("\n");
            }

            return sb.toString();
        }
    }

    // ══════════════════════════ Documents d'un dossier ══════════════════════════

    @Component
    public static class DocumentsDuDossier extends AbstractOutil {

        private final DocumentRepository documentRepository;
        private final RemiseRepository remiseRepository;
        private final TypeDocumentRepository typeDocumentRepository;
        private final Resolveur resolveur;

        public DocumentsDuDossier(DocumentRepository documentRepository,
                                  RemiseRepository remiseRepository,
                                  TypeDocumentRepository typeDocumentRepository,
                                  Resolveur resolveur) {
            super("documents_du_dossier",
                    "Analyse documentaire d'un dossier : documents déjà remis avec leur date de remise, "
                            + "documents obligatoires encore manquants, et dates de remise prévues. "
                            + "Utilise cet outil pour les questions sur les pièces, factures, justificatifs ou "
                            + "le taux de complétion documentaire d'un dossier.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE))),
                            "reference"),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.documentRepository = documentRepository;
            this.remiseRepository = remiseRepository;
            this.typeDocumentRepository = typeDocumentRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Examiner les documents du dossier " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            List<Document> documents = documentRepository.findByDossierIdAndEstActifTrue(d.getId());
            Map<Integer, LocalDateTime> dates = new java.util.HashMap<>();
            for (Remise r : remiseRepository.findByDossierId(d.getId())) {
                dates.put(r.getTypeDocument().getId(), r.getDateRemise());
            }

            List<TypeDocument> attendus = d.getTypeProduction() != null
                    ? typeDocumentRepository.findByTypeProductionId(d.getTypeProduction().getId())
                    : typeDocumentRepository.findAll();

            StringBuilder sb = new StringBuilder("Documents du dossier ").append(d.getNumeroDossier())
                    .append(" (type de production : ")
                    .append(d.getTypeProduction() != null ? d.getTypeProduction().getNom() : "non défini")
                    .append(")\n\n");

            sb.append("REMIS (").append(documents.size()).append(")\n");
            if (documents.isEmpty()) {
                sb.append("- aucun document remis\n");
            }
            for (Document doc : documents) {
                sb.append("- ").append(doc.getTypeDocument().getNom())
                        .append(" — remis le ").append(OutilsDate.lisible(dates.get(doc.getTypeDocument().getId())))
                        .append(doc.getNomFichier() != null ? " — fichier « " + doc.getNomFichier() + " »" : "")
                        .append("\n");
            }

            List<TypeDocument> manquants = new ArrayList<>();
            for (TypeDocument attendu : attendus) {
                boolean remis = documents.stream()
                        .anyMatch(doc -> doc.getTypeDocument().getId().equals(attendu.getId()));
                if (!remis) {
                    manquants.add(attendu);
                }
            }

            sb.append("\nEN ATTENTE (").append(manquants.size()).append(")\n");
            if (manquants.isEmpty()) {
                sb.append("- tous les documents attendus ont été remis\n");
            } else {
                for (TypeDocument t : manquants) {
                    sb.append("- ").append(t.getNom())
                            .append(Boolean.TRUE.equals(t.getEstObligatoire()) ? " (OBLIGATOIRE, dossier incomplet)" : " (facultatif)")
                            .append(t.getDescription() != null ? " — " + t.getDescription() : "")
                            .append("\n");
                }
            }

            sb.append("\nREMISES PLANIFIÉES\n");
            List<Remise> remises = remiseRepository.findByDossierId(d.getId());
            if (remises.isEmpty()) {
                sb.append("- aucune date de remise enregistrée\n");
            } else {
                remises.stream()
                        .sorted(java.util.Comparator.comparing(Remise::getDateRemise,
                                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                        .forEach(r -> sb.append("- ").append(r.getTypeDocument().getNom())
                                .append(" — ").append(OutilsDate.lisible(r.getDateRemise())).append("\n"));
            }

            return sb.toString();
        }
    }

    // ══════════════════════════ Écriture : changement de phase ══════════════════════════

    @Component
    public static class ChangerPhaseDossier extends AbstractOutil {

        private final DossierService dossierService;
        private final Resolveur resolveur;

        public ChangerPhaseDossier(DossierService dossierService, Resolveur resolveur) {
            super("changer_phase_dossier",
                    "Change la phase d'un dossier (par exemple passer de « En transit » à « Dossier valide »). "
                            + "Vérifie d'abord la fiche du dossier et les phases existantes avant d'appeler cet "
                            + "outil. Opération qui écrit en base : elle est soumise à confirmation de l'utilisateur.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE)),
                            OutilsArgs.p("phase", str("Nom exact de la nouvelle phase. Utilise lister_referentiels type=phases pour connaître les valeurs.")),
                            OutilsArgs.p("commentaire", str("Motif du changement, enregistré dans l'historique."))),
                            "reference", "phase"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE);
            this.dossierService = dossierService;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Passer le dossier " + OutilsArgs.texte(arguments, "reference", "?")
                    + " en phase « " + OutilsArgs.texte(arguments, "phase", "?") + " »";
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);
            PhaseDossier phase = resolveur.phase(OutilsArgs.texteObligatoire(arguments, "phase"));

            String phaseActuelle = d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "aucune";
            if (phase.getId().equals(d.getPhaseDossier() != null ? d.getPhaseDossier().getId() : null)) {
                return "Le dossier " + d.getNumeroDossier() + " est déjà en phase « " + phase.getNom()
                        + " ». Aucune modification n'a été faite.";
            }

            d.setPhaseDossier(phase);
            dossierService.saveDossier(d);

            return "Phase du dossier " + d.getNumeroDossier() + " modifiée : « " + phaseActuelle
                    + " » → « " + phase.getNom() + " »."
                    + (OutilsArgs.texte(arguments, "commentaire") != null
                    ? " Commentaire : " + OutilsArgs.texte(arguments, "commentaire") + "." : "");
        }
    }

    // ══════════════════════════ Écriture : changement de statut ══════════════════════════

    @Component
    public static class ChangerStatutDossier extends AbstractOutil {

        private final DossierService dossierService;
        private final Resolveur resolveur;

        public ChangerStatutDossier(DossierService dossierService, Resolveur resolveur) {
            super("changer_statut_dossier",
                    "Change le statut d'un dossier (Ouvert, Clôturé, Annulé, En litige…). "
                            + "Vérifie d'abord la fiche du dossier et les statuts existants avant d'appeler cet "
                            + "outil. Opération qui écrit en base : elle est soumise à confirmation.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE)),
                            OutilsArgs.p("statut", str("Nom exact du nouveau statut. Utilise lister_referentiels type=statuts.")),
                            OutilsArgs.p("commentaire", str("Motif du changement."))),
                            "reference", "statut"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE);
            this.dossierService = dossierService;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Passer le dossier " + OutilsArgs.texte(arguments, "reference", "?")
                    + " au statut « " + OutilsArgs.texte(arguments, "statut", "?") + " »";
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);
            StatutDossier statut = resolveur.statut(OutilsArgs.texteObligatoire(arguments, "statut"));

            String statutActuel = d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "aucun";
            if (statut.getId().equals(d.getStatutDossier() != null ? d.getStatutDossier().getId() : null)) {
                return "Le dossier " + d.getNumeroDossier() + " est déjà au statut « " + statut.getNom()
                        + " ». Aucune modification n'a été faite.";
            }

            d.setStatutDossier(statut);
            dossierService.saveDossier(d);

            return "Statut du dossier " + d.getNumeroDossier() + " modifié : « " + statutActuel
                    + " » → « " + statut.getNom() + " »."
                    + (OutilsArgs.texte(arguments, "commentaire") != null
                    ? " Commentaire : " + OutilsArgs.texte(arguments, "commentaire") + "." : "");
        }
    }

    // ══════════════════════════ Écriture : remise de documents ══════════════════════════

    @Component
    public static class AssignerDocumentsDossier extends AbstractOutil {

        private final DocumentService documentService;
        private final Resolveur resolveur;

        public AssignerDocumentsDossier(DocumentService documentService, Resolveur resolveur) {
            super("assigner_documents_dossier",
                    "Remet un ou plusieurs documents à un dossier, avec une date de remise. Utilise cet outil "
                            + "pour marquer une pièce comme remise ou pour corriger / repousser une date de remise. "
                            + "Vérifie d'abord les documents attendus (documents_du_dossier) et les noms exacts des "
                            + "types de documents (lister_referentiels type=documents). "
                            + "Opération qui écrit en base : elle est soumise à confirmation.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE)),
                            OutilsArgs.p("documents", listeDe("string",
                                    "Noms exacts des types de documents à remettre (ex: [\"Facture commerciale\", \"Connaissement\"]).")),
                            OutilsArgs.p("date_remise", str("Date de remise : \"2026-10-15\", \"15/10/2026\", \"demain\"… Si absente, la date du jour est utilisée."))),
                            "reference", "documents"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE);
            this.documentService = documentService;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Remettre les documents " + OutilsArgs.textes(arguments, "documents")
                    + " au dossier " + OutilsArgs.texte(arguments, "reference", "?")
                    + (OutilsArgs.texte(arguments, "date_remise") != null
                    ? " (remise le " + OutilsArgs.texte(arguments, "date_remise") + ")" : "");
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            List<String> noms = OutilsArgs.textes(arguments, "documents");
            if (noms.isEmpty()) {
                throw new IllegalArgumentException("Indiquez au moins un type de document à remettre.");
            }

            LocalDateTime dateRemise = OutilsDate.finJour(
                    OutilsArgs.texte(arguments, "date_remise"), LocalDateTime.now());

            List<Integer> ids = new ArrayList<>();
            List<String> nomsResolus = new ArrayList<>();
            for (String nom : noms) {
                TypeDocument type = resolveur.typeDocument(nom);
                ids.add(type.getId());
                nomsResolus.add(type.getNom());
            }

            documentService.assignerDocuments(d.getId(), ids, dateRemise);

            StringBuilder sb = new StringBuilder("Documents remis pour le dossier ")
                    .append(d.getNumeroDossier()).append(" le ").append(OutilsDate.lisible(dateRemise)).append(" : ");
            sb.append(String.join(", ", nomsResolus)).append(". ");
            sb.append(documentService.tousDocsObligatoiresRemis(d.getId())
                    ? "Le dossier est désormais complet au regard des documents obligatoires."
                    : "Attention : il reste des documents obligatoires non remis sur ce dossier.");
            return sb.toString();
        }
    }

    // ══════════════════════════ Écriture : création de dossier ══════════════════════════

    @Component
    public static class CreerDossier extends AbstractOutil {

        private final DossierService dossierService;
        private final UtilisateurRepository utilisateurRepository;
        private final PhaseDossierRepository phaseRepository;
        private final StatutDossierRepository statutRepository;
        private final Resolveur resolveur;

        public CreerDossier(DossierService dossierService,
                            UtilisateurRepository utilisateurRepository,
                            PhaseDossierRepository phaseRepository,
                            StatutDossierRepository statutRepository,
                            Resolveur resolveur) {
            super("creer_dossier",
                    "Crée un nouveau dossier de transit rattaché à un client existant et à l'agence de "
                            + "l'utilisateur. Le numéro de dossier est généré automatiquement par l'application. "
                            + "Avant d'appeler cet outil, vérifie que le client existe (rechercher_clients) et "
                            + "demande à l'utilisateur les informations manquantes (ordre de transit, type de "
                            + "production, échéance…). Opération qui écrit en base : confirmation obligatoire.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("client", str("Identifiant, nom, NIF ou STAT du client existant.")),
                            OutilsArgs.p("type_production", str("Nom du type de production (ex: « Maritime », « Routier »).")),
                            OutilsArgs.p("ordre_transit", str("Numéro ou identifiant de l'ordre de transit à rattacher, si connu.")),
                            OutilsArgs.p("phase", str("Phase initiale du dossier. Si absente, la première phase du référentiel est utilisée.")),
                            OutilsArgs.p("statut", str("Statut initial du dossier. Si absent, « Ouvert » est utilisé.")),
                            OutilsArgs.p("pour_compte", str("Nom du donneur d'ordre / destinataire facturé.")),
                            OutilsArgs.p("depart", str("Lieu ou port de départ.")),
                            OutilsArgs.p("echeance", str("Échéance du dossier, ex: \"2026-11-30\".")),
                            OutilsArgs.p("date_livraison", str("Date de livraison prévue.")),
                            OutilsArgs.p("destination", str("Pays de destination."))),
                            "client"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE);
            this.dossierService = dossierService;
            this.utilisateurRepository = utilisateurRepository;
            this.phaseRepository = phaseRepository;
            this.statutRepository = statutRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Créer un dossier pour le client " + OutilsArgs.texte(arguments, "client", "?")
                    + (OutilsArgs.texte(arguments, "ordre_transit") != null
                    ? " rattaché à l'OT n° " + OutilsArgs.texte(arguments, "ordre_transit") : "");
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Utilisateur utilisateur = contexte.utilisateur();
            if (utilisateur == null || utilisateur.getAgence() == null) {
                throw new IllegalStateException("Impossible de créer un dossier : l'utilisateur connecté n'a pas d'agence.");
            }

            Client client = resolveur.client(OutilsArgs.texteObligatoire(arguments, "client"), contexte);

            Dossier dossier = new Dossier();
            dossier.setAgence(utilisateur.getAgence());
            dossier.setUtilisateur(utilisateur);
            dossier.setClient(client);

            String typeProduction = OutilsArgs.texte(arguments, "type_production");
            if (typeProduction != null) {
                dossier.setTypeProduction(resolveur.typeProduction(typeProduction));
            }

            String ot = OutilsArgs.texte(arguments, "ordre_transit");
            if (ot != null) {
                dossier.setOrdreTransit(resolveur.ordreTransit(ot, contexte));
            }

            dossier.setPourCompte(OutilsArgs.texte(arguments, "pour_compte"));
            dossier.setDepart(OutilsArgs.texte(arguments, "depart"));
            dossier.setEcheance(OutilsDate.finJour(OutilsArgs.texte(arguments, "echeance"), null));
            dossier.setDateLivraison(OutilsDate.finJour(OutilsArgs.texte(arguments, "date_livraison"), null));
            dossier.setPhaseDossier(resolveur.phase(OutilsArgs.texte(arguments, "phase")));
            dossier.setStatutDossier(resolveur.statut(OutilsArgs.texte(arguments, "statut")));

            String destination = OutilsArgs.texte(arguments, "destination");

            Dossier enregistre = dossierService.saveDossier(dossier);

            StringBuilder sb = new StringBuilder("Dossier créé avec succès : n° ")
                    .append(enregistre.getNumeroDossier()).append(" (identifiant ").append(enregistre.getId()).append(")\n");
            sb.append("- Client : ").append(Resolveur.libelle(client)).append("\n");
            sb.append("- Agence : ").append(enregistre.getAgence().getNom()).append("\n");
            sb.append("- Phase : ").append(enregistre.getPhaseDossier() != null ? enregistre.getPhaseDossier().getNom() : "-")
                    .append(" | Statut : ").append(enregistre.getStatutDossier() != null ? enregistre.getStatutDossier().getNom() : "-").append("\n");
            if (enregistre.getOrdreTransit() != null) {
                sb.append("- Ordre de transit : n° ").append(enregistre.getOrdreTransit().getNumeroOrdreTransit()).append("\n");
            }
            if (enregistre.getEcheance() != null) {
                sb.append("- Échéance : ").append(OutilsDate.lisible(enregistre.getEcheance())).append("\n");
            }
            sb.append(destination != null ? "- Pays de destination demandé : " + destination
                    + " (à sélectionner dans la fiche du dossier)" : "");
            return sb.toString();
        }
    }

    // ══════════════════════════ Écriture : suppression de dossier ══════════════════════════

    @Component
    public static class SupprimerDossier extends AbstractOutil {

        private final DossierService dossierService;
        private final DossierRepository dossierRepository;
        private final Resolveur resolveur;

        public SupprimerDossier(DossierService dossierService,
                                DossierRepository dossierRepository,
                                Resolveur resolveur) {
            super("supprimer_dossier",
                    "Supprime définitivement un dossier et toutes ses données rattachées (documents, remises, "
                            + "demandes inter-agences, liens inter-agences). Action irréversible : ne l'appelle "
                            + "que si l'utilisateur a explicitement demandé la suppression et après lui avoir "
                            + "montré la fiche du dossier.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_REFERENCE)),
                            OutilsArgs.p("motif", str("Motif de la suppression, consigné dans la confirmation."))),
                            "reference"),
                    NiveauDanger.CRITIQUE,
                    Resolveur.NIVEAU_SUPPRESSION);
            this.dossierService = dossierService;
            this.dossierRepository = dossierRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Supprimer définitivement le dossier " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Dossier d = resolveur.dossier(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);
            String numero = d.getNumeroDossier();
            String client = Resolveur.libelle(d.getClient());

            dossierService.deleteDossier(d.getId());

            return "Le dossier " + numero + " (client " + client + ") a été supprimé définitivement, "
                    + "ainsi que ses documents, remises et demandes inter-agences rattachées.";
        }
    }

    // ══════════════════════════ Outils internes ══════════════════════════

    private static Map<String, Object> strEnum(String description, String... valeurs) {
        Map<String, Object> p = OutilsArgs.prop("string", description);
        p.put("enum", List.of(valeurs));
        return p;
    }

    /** Comparaison insensible à la casse et aux accents. */
    static boolean equalsIgnorantAccents(String a, String b) {
        return a != null && b != null
                && com.auximad.ai.BaseConnaissance.normaliser(a).equals(com.auximad.ai.BaseConnaissance.normaliser(b.trim()));
    }
}
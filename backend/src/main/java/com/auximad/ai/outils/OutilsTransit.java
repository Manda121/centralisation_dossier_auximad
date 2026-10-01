package com.auximad.ai.outils;

import com.auximad.ai.AbstractOutil;
import com.auximad.ai.ContexteChat;
import com.auximad.ai.NiveauDanger;
import com.auximad.ai.OutilsArgs;
import com.auximad.ai.OutilsDate;
import com.auximad.ai.Resolveur;
import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.service.OrdreTransitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Outils portant sur les ordres de transit, les marchandises et leur validation.
 */
public final class OutilsTransit {

    private OutilsTransit() {
    }

    private static final String DESC_OT =
            "Numéro d'ordre de transit (ex: 10234), identifiant, numéro de commande ou nom du client.";

    // ══════════════════════════ Recherche ══════════════════════════

    @Component
    public static class RechercherOrdresTransit extends AbstractOutil {

        private final OrdreTransitRepository ordreTransitRepository;

        public RechercherOrdresTransit(OrdreTransitRepository ordreTransitRepository) {
            super("rechercher_ordres_transit",
                    "Recherche des ordres de transit par numéro, numéro de commande ou nom du client. "
                            + "Utilise cet outil pour trouver un ou plusieurs OT ; pour le détail complet d'un OT "
                            + "connu, utilise obtenir_ordre_transit.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("recherche", str("Texte libre : numéro de commande, nom du client, partie du numéro d'OT.")),
                            OutilsArgs.p("numero", entierProp("Numéro exact de l'ordre de transit.")),
                            OutilsArgs.p("client_id", entierProp("Restreindre aux ordres d'un client donné.")),
                            OutilsArgs.p("limite", entierProp("Nombre maximum d'OT à renvoyer (max 50)."))),
                            new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.ordreTransitRepository = ordreTransitRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Rechercher des ordres de transit";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Page<OrdreTransit> page = ordreTransitRepository.searchOrdreTransits(
                    OutilsArgs.texte(arguments, "recherche"),
                    OutilsArgs.entier(arguments, "numero"),
                    null, null, null, null, null, null, null, null,
                    OutilsArgs.entier(arguments, "client_id"),
                    PageRequest.of(0, Math.min(Math.max(OutilsArgs.entier(arguments, "limite", 20), 1), 50),
                            // Requête native : le tri doit porter sur une colonne SQL.
                            Sort.by(Sort.Direction.DESC, "date_creation")));

            if (page.getNumberOfElements() == 0) {
                return "Aucun ordre de transit ne correspond aux critères demandés.";
            }

            StringBuilder sb = new StringBuilder((int) page.getTotalElements()).append(" ordre(s) de transit trouvé(s) :\n");
            for (OrdreTransit ot : page.getContent()) {
                sb.append("- OT n° ").append(ot.getNumeroOrdreTransit())
                        .append(" | commande : ").append(ot.getNumeroCommande() == null ? "-" : ot.getNumeroCommande())
                        .append(" | client : ").append(Resolveur.libelle(ot.getClient()))
                        .append(" | type : ").append(ot.getTypeProduction() != null ? ot.getTypeProduction().getNom() : "-")
                        .append(" | régime : ").append(ot.getTypeRegime() != null ? ot.getTypeRegime().getNom() : "-")
                        .append(" | statut : ").append(ot.getStatutOt() != null ? ot.getStatutOt().getNom() : "-")
                        .append(" | créé le ").append(OutilsDate.lisible(ot.getDateCreation()))
                        .append("\n");
            }
            return sb.toString();
        }
    }

    // ══════════════════════════ Détail d'un OT ══════════════════════════

    @Component
    public static class ObtenirOrdreTransit extends AbstractOutil {

        private final OrdreTransitRepository ordreTransitRepository;
        private final MarchandiseRepository marchandiseRepository;
        private final ContratTransportRepository contratRepository;
        private final InstructionRepository instructionRepository;
        private final DossierRepository dossierRepository;
        private final Resolveur resolveur;

        public ObtenirOrdreTransit(OrdreTransitRepository ordreTransitRepository,
                                   MarchandiseRepository marchandiseRepository,
                                   ContratTransportRepository contratRepository,
                                   InstructionRepository instructionRepository,
                                   DossierRepository dossierRepository,
                                   Resolveur resolveur) {
            super("obtenir_ordre_transit",
                    "Détail complet d'un ordre de transit : client, expéditeur, destinataire, régime, type de "
                            + "production, bureau de dédouanement, destination, marchandises declarées, contrat de "
                            + "transport, instructions, et dossier de transit rattaché s'il existe.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_OT))),
                            "reference"),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.ordreTransitRepository = ordreTransitRepository;
            this.marchandiseRepository = marchandiseRepository;
            this.contratRepository = contratRepository;
            this.instructionRepository = instructionRepository;
            this.dossierRepository = dossierRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Lire l'ordre de transit " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            OrdreTransit ot = resolveur.ordreTransit(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            StringBuilder sb = new StringBuilder("Ordre de transit n° ").append(ot.getNumeroOrdreTransit())
                    .append(" (identifiant ").append(ot.getId()).append(")\n");
            sb.append("- Client donneur d'ordre : ").append(Resolveur.libelle(ot.getClient())).append("\n");
            sb.append("- Expéditeur : ").append(Resolveur.libelle(ot.getExpediteur()))
                    .append(" | Destinataire : ").append(Resolveur.libelle(ot.getDestinataire()))
                    .append(" | Facture à : ").append(Resolveur.libelle(ot.getDestinationFacture())).append("\n");
            sb.append("- Numéro de commande : ").append(ot.getNumeroCommande() == null ? "-" : ot.getNumeroCommande()).append("\n");
            sb.append("- Type de production : ").append(ot.getTypeProduction() != null ? ot.getTypeProduction().getNom() : "-")
                    .append(" | Type de régime : ").append(ot.getTypeRegime() != null ? ot.getTypeRegime().getNom() : "-").append("\n");
            sb.append("- Bureau de dédouanement : ").append(ot.getBureauDedouanement() != null ? ot.getBureauDedouanement().getNom() : "-")
                    .append(" | Destination finale : ").append(ot.getDestinationFinale() == null ? "-" : ot.getDestinationFinale()).append("\n");
            sb.append("- Statut : ").append(ot.getStatutOt() != null ? ot.getStatutOt().getNom() : "-")
                    .append(" | Assuré : ").append(Boolean.TRUE.equals(ot.getEstAssure()) ? "oui" : "non").append("\n");
            sb.append("- Créé le ").append(OutilsDate.lisible(ot.getDateCreation()))
                    .append(" par ").append(ot.getUtilisateur() != null ? ot.getUtilisateur().getNomUtilisateur() : "-").append("\n");

            List<Marchandise> marchandises = marchandiseRepository.findByOrdreTransitId(ot.getId());
            sb.append("- Marchandises déclarées (").append(marchandises.size()).append(") :\n");
            if (marchandises.isEmpty()) {
                sb.append("    aucune\n");
            } else {
                for (Marchandise m : marchandises) {
                    sb.append("    · ").append(m.getNatureMarchandise() != null ? m.getNatureMarchandise().getNom() : "-")
                            .append(" | marque : ").append(m.getMarque() == null ? "-" : m.getMarque())
                            .append(" | modèle : ").append(m.getModele() == null ? "-" : m.getModele())
                            .append(" | quantité : ").append(m.getQuantite() == null ? "-" : m.getQuantite())
                            .append(m.getUniteMesure() != null ? " " + m.getUniteMesure().getNom() : "")
                            .append(" | poids : ").append(m.getPoids() == null ? "-" : m.getPoids())
                            .append(" | volume : ").append(m.getVolume() == null ? "-" : m.getVolume())
                            .append(m.getDescription() != null ? " — " + m.getDescription() : "")
                            .append("\n");
                }
            }

            contratRepository.findByOrdreTransitId(ot.getId()).ifPresent(c -> {
                sb.append("- Contrat de transport : ").append(c.getNumero() == null ? "-" : c.getNumero())
                        .append(" | compagnie : ").append(c.getCompagnie() != null ? c.getCompagnie().getNom() : "-")
                        .append(" | navire : ").append(c.getNavire() != null ? c.getNavire().getNom() : "-")
                        .append(" | vol n° ").append(c.getNumeroVol() == null ? "-" : c.getNumeroVol())
                        .append(" | arrivée prévue : ").append(OutilsDate.lisible(c.getDateArrivee()))
                        .append("\n");
            });

            List<Instruction> instructions = instructionRepository.findByOrdreTransitId(ot.getId());
            if (!instructions.isEmpty()) {
                sb.append("- Instructions (").append(instructions.size()).append(") :\n");
                instructions.forEach(i -> sb.append("    · ").append(i.getInstruction())
                        .append(" (le ").append(OutilsDate.lisible(i.getDateCreation())).append(")\n"));
            }

            List<Dossier> dossiers = dossierRepository.findByOrdreTransitId(ot.getId())
                    .map(List::of).orElse(List.of());
            sb.append("- Dossiers rattachés : ").append(dossiers.isEmpty() ? "aucun" : "").append("\n");
            dossiers.forEach(d -> sb.append("    · n° ").append(d.getNumeroDossier())
                    .append(" | phase : ").append(d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "-")
                    .append(" | statut : ").append(d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "-")
                    .append(" | agence : ").append(d.getAgence() != null ? d.getAgence().getNom() : "-")
                    .append("\n"));

            return sb.toString();
        }
    }

    // ══════════════════════════ Validation ══════════════════════════

    @Component
    public static class ValiderOrdreTransit extends AbstractOutil {

        private final OrdreTransitService ordreTransitService;
        private final OrdreTransitRepository ordreTransitRepository;
        private final Resolveur resolveur;

        public ValiderOrdreTransit(OrdreTransitService ordreTransitService,
                                   OrdreTransitRepository ordreTransitRepository,
                                   Resolveur resolveur) {
            super("valider_ordre_transit",
                    "Valide un ordre de transit (le fais passer en statut validé). Vérifie d'abord son état avec "
                            + "obtenir_ordre_transit. Opération qui écrit en base : elle est soumise à confirmation.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str(DESC_OT))),
                            "reference"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_VALIDATION);
            this.ordreTransitService = ordreTransitService;
            this.ordreTransitRepository = ordreTransitRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Valider l'ordre de transit " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            OrdreTransit ot = resolveur.ordreTransit(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            String avant = ot.getStatutOt() != null ? ot.getStatutOt().getNom() : "non défini";
            OrdreTransit valide = ordreTransitService.validateOrdreTransit(ot.getId());

            return "Ordre de transit n° " + ot.getNumeroOrdreTransit() + " validé (statut « " + avant + " » → « "
                    + (valide.getStatutOt() != null ? valide.getStatutOt().getNom() : "validé") + " »).";
        }
    }
}
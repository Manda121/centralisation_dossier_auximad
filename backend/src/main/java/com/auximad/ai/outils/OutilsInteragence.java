package com.auximad.ai.outils;

import com.auximad.ai.AbstractOutil;
import com.auximad.ai.ContexteChat;
import com.auximad.ai.NiveauDanger;
import com.auximad.ai.OutilsArgs;
import com.auximad.ai.OutilsDate;
import com.auximad.ai.Resolveur;
import com.auximad.dto.DemandeInteragenceReponseRequest;
import com.auximad.entity.DemandeInteragence;
import com.auximad.repository.DemandeInteragenceRepository;
import com.auximad.service.DemandeInteragenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Outils portant sur les demandes d'inter-agence : consultation et réponse.
 *
 * Le filtrage par agence est fait en base ({@code searchDemandes}) et une seconde vérification
 * est appliquée côté service métier au moment de la réponse.
 */
public final class OutilsInteragence {

    private OutilsInteragence() {
    }

    // ══════════════════════════ Consultation ══════════════════════════

    @Component
    public static class ListerDemandesInteragence extends AbstractOutil {

        private final DemandeInteragenceRepository demandeRepository;

        public ListerDemandesInteragence(DemandeInteragenceRepository demandeRepository) {
            super("lister_demandes_interagence",
                    "Liste les demandes d'inter-agence : dossier concerné, agence émettrice, agence destinataire, "
                            + "motif et statut (EN_ATTENTE, ACCEPTEE, REFUSEE). Par défaut seules les demandes de "
                            + "l'utilisateur connecté sont renvoyées ; utilise statut=\"EN_ATTENTE\" pour les "
                            + "demandes en attente de traitement. Appelle cet outil pour « les demandes en attente ». "
                            + "Outil de lecture, aucune confirmation n'est requise.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("statut", strEnum("Filtrer par statut : EN_ATTENTE, ACCEPTEE ou REFUSEE.")),
                            OutilsArgs.p("dossier", str("Numéro du dossier concerné.")),
                            OutilsArgs.p("agence", str("Restreindre à une agence (demandeuse ou destinataire).")),
                            OutilsArgs.p("date_min", str("Date minimale de la demande, format AAAA-MM-JJ.")),
                            OutilsArgs.p("date_max", str("Date maximale de la demande, format AAAA-MM-JJ.")),
                            OutilsArgs.p("limite", entierProp("Nombre maximum de demandes à renvoyer (max 50)."))),
                            new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.demandeRepository = demandeRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            StringBuilder sb = new StringBuilder("Lister les demandes d'inter-agence");
            String statut = OutilsArgs.texte(arguments, "statut");
            String dossier = OutilsArgs.texte(arguments, "dossier");
            if (statut != null) {
                sb.append(" au statut ").append(statut);
            }
            if (dossier != null) {
                sb.append(" du dossier ").append(dossier);
            }
            return sb.toString();
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            // Périmètre : l'utilisateur ne voit que les demandes de son agence, sauf s'il a
            // la portée toutes-agences (comme le fait déjà DemandeInteragenceService).
            boolean peutVoirTout = contexte.peutVoirToutesAgences();
            Integer agenceConcernee = peutVoirTout ? null : contexte.idAgence();

            Page<DemandeInteragence> page = demandeRepository.searchDemandes(
                    OutilsDate.iso(OutilsDate.debutJour(OutilsArgs.texte(arguments, "date_min"), null)),
                    OutilsDate.iso(OutilsDate.finJour(OutilsArgs.texte(arguments, "date_max"), null)),
                    OutilsArgs.texte(arguments, "statut"),
                    null, null, agenceConcernee,
                    OutilsArgs.texte(arguments, "dossier"),
                    PageRequest.of(0, Math.min(Math.max(OutilsArgs.entier(arguments, "limite", 20), 1), 50),
                            // Requête native : tri sur colonne SQL.
                            Sort.by(Sort.Direction.DESC, "date_demande")));

            if (page.getNumberOfElements() == 0) {
                return "Aucune demande d'inter-agence ne correspond aux critères demandés.";
            }

            long enAttente = demandeRepository.countByAgenceDestinataireIdAndStatutAndEstVueFalse(
                    contexte.idAgence() == null ? -1 : contexte.idAgence(), "EN_ATTENTE");

            StringBuilder sb = new StringBuilder((int) page.getTotalElements())
                    .append(" demande(s) d'inter-agence trouvée(s)")
                    .append(enAttente > 0 ? " — dont " + enAttente + " en attente non lue(s) pour votre agence." : ".")
                    .append("\n");

            for (DemandeInteragence d : page.getContent()) {
                sb.append("- #").append(d.getId())
                        .append(" | dossier : ")
                        .append(d.getDossier() != null ? d.getDossier().getNumeroDossier() : "-")
                        .append(" (")
                        .append(d.getDossier() != null && d.getDossier().getClient() != null
                                ? Resolveur.libelle(d.getDossier().getClient()) : "client inconnu")
                        .append(")")
                        .append("\n    ")
                        .append(d.getAgenceDemandeuse() != null ? d.getAgenceDemandeuse().getNom() : "?")
                        .append(" → ")
                        .append(d.getAgenceDestinataire() != null ? d.getAgenceDestinataire().getNom() : "?")
                        .append(" | statut : ").append(d.getStatut())
                        .append(" | demandée le ").append(OutilsDate.lisible(d.getDateDemande()));
                if (!"EN_ATTENTE".equalsIgnoreCase(d.getStatut())) {
                    sb.append(" | répondue le ").append(OutilsDate.lisible(d.getDateReponse()));
                }
                sb.append("\n    motif : ").append(d.getMotif() == null ? "-" : d.getMotif());
                if (d.getReponseMotif() != null) {
                    sb.append("\n    réponse : ").append(d.getReponseMotif());
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    // ══════════════════════════ Réponse ══════════════════════════

    @Component
    public static class RepondreDemandeInteragence extends AbstractOutil {

        private final DemandeInteragenceService demandeInteragenceService;
        private final Resolveur resolveur;

        public RepondreDemandeInteragence(DemandeInteragenceService demandeInteragenceService,
                                          Resolveur resolveur) {
            super("repondre_demande_interagence",
                    "Répond à une demande d'inter-agence en attente. Si elle est acceptée, un sous-dossier "
                            + "inter-agences est créé automatiquement pour l'agence destinataire ; si elle est "
                            + "refusée, le dossier reste inchangé. Seule l'agence destinataire peut répondre "
                            + "(sauf profil à portée nationale). Demande le motif de réponse à l'utilisateur avant "
                            + "d'appeler cet outil. Opération qui écrit en base : confirmation obligatoire.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("demande_id", entierProp("Identifiant numérique de la demande (le # affiché).")),
                            OutilsArgs.p("acceptee", booleenProp("true pour accepter, false pour refuser.")),
                            OutilsArgs.p("motif_reponse", str("Motif de la réponse, communiqué à l'agence émettrice."))),
                            "demande_id", "acceptee"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE_INTERAGENCE);
            this.demandeInteragenceService = demandeInteragenceService;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            Integer id = OutilsArgs.entier(arguments, "demande_id");
            boolean acceptee = OutilsArgs.booleen(arguments, "acceptee", false);
            return (acceptee ? "Accepter" : "Refuser") + " la demande d'inter-agence #"
                    + (id == null ? "?" : id);
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Integer idDemande = OutilsArgs.entierObligatoire(arguments, "demande_id");
            boolean acceptee = OutilsArgs.booleen(arguments, "acceptee", false);

            DemandeInteragence demande = resolveur.demande(idDemande, contexte);
            if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
                throw new IllegalArgumentException("La demande #" + idDemande + " a déjà été traitée (statut « "
                        + demande.getStatut() + " ») : elle n'est plus modifiable.");
            }

            String motif = OutilsArgs.texte(arguments, "motif_reponse");
            if (motif == null) {
                throw new IllegalArgumentException("Un motif de réponse est obligatoire : demande-le à l'utilisateur.");
            }

            DemandeInteragenceReponseRequest reponse = new DemandeInteragenceReponseRequest();
            reponse.setAcceptee(acceptee);
            reponse.setMotifReponse(motif);

            DemandeInteragence resultat = demandeInteragenceService.repondreDemande(idDemande, reponse);

            String numeroDossier = resultat.getDossier() != null ? resultat.getDossier().getNumeroDossier() : "-";
            if (acceptee) {
                return "Demande d'inter-agence #" + idDemande + " acceptée pour le dossier " + numeroDossier
                        + ". Un sous-dossier inter-agences a été créé pour l'agence "
                        + (resultat.getAgenceDestinataire() != null ? resultat.getAgenceDestinataire().getNom() : "-")
                        + ".";
            }
            return "Demande d'inter-agence #" + idDemande + " refusée pour le dossier " + numeroDossier
                    + ". Le dossier reste inchangé.";
        }
    }

    static Map<String, Object> strEnum(String description, String... valeurs) {
        return OutilsArgs.propEnum(description, valeurs);
    }
}
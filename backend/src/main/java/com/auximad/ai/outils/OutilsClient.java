package com.auximad.ai.outils;

import com.auximad.ai.AbstractOutil;
import com.auximad.ai.ContexteChat;
import com.auximad.ai.NiveauDanger;
import com.auximad.ai.OutilsArgs;
import com.auximad.ai.OutilsDate;
import com.auximad.ai.Resolveur;
import com.auximad.entity.Client;
import com.auximad.entity.Dossier;
import com.auximad.entity.OrdreTransit;
import com.auximad.entity.Pays;
import com.auximad.entity.TypeClient;
import com.auximad.repository.ClientRepository;
import com.auximad.repository.DossierRepository;
import com.auximad.repository.OrdreTransitRepository;
import com.auximad.repository.PaysRepository;
import com.auximad.repository.TypeClientRepository;
import com.auximad.service.ClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Outils portant sur les clients : recherche, fiche détaillée, création et suppression.
 */
public final class OutilsClient {

    private OutilsClient() {
    }

    // ══════════════════════════ Recherche ══════════════════════════

    @Component
    public static class RechercherClients extends AbstractOutil {

        private final ClientRepository clientRepository;
        private final DossierRepository dossierRepository;
        private final Resolveur resolveur;

        public RechercherClients(ClientRepository clientRepository,
                                 DossierRepository dossierRepository,
                                 Resolveur resolveur) {
            super("rechercher_clients",
                    "Recherche des clients par nom, prénom, identifiant, NIF, STAT, ville ou texte libre. "
                            + "Utilise cet outil quand l'utilisateur cite un nom de client, ou pour lister les clients.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("recherche", str("Texte libre : nom, prénom, NIF, STAT, ville.")),
                            OutilsArgs.p("ville", str("Filtrer par ville.")),
                            OutilsArgs.p("limite", entierProp("Nombre maximum de clients à renvoyer (max 50)."))),
                            new String[0]),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.clientRepository = clientRepository;
            this.dossierRepository = dossierRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Rechercher des clients";
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            String recherche = OutilsArgs.texte(arguments, "recherche");
            String ville = OutilsArgs.texte(arguments, "ville");
            int limite = Math.min(Math.max(OutilsArgs.entier(arguments, "limite", 20), 1), 50);

            Page<Client> page = clientRepository.searchClients(
                    recherche, null, null, null, null, null, null, null, null, ville, null, null, null,
                    PageRequest.of(0, limite, Sort.by("nom").ascending()));

            if (page.getNumberOfElements() == 0) {
                return "Aucun client ne correspond aux critères demandés.";
            }

            StringBuilder sb = new StringBuilder((int) page.getTotalElements()).append(" client(s) trouvé(s) :\n");
            for (Client c : page.getContent()) {
                sb.append("- id ").append(c.getId()).append(" | ").append(Resolveur.libelle(c))
                        .append(" | NIF : ").append(c.getNif() == null ? "-" : c.getNif())
                        .append(" | STAT : ").append(c.getStat() == null ? "-" : c.getStat())
                        .append(" | email : ").append(c.getEmail() == null ? "-" : c.getEmail())
                        .append(" | tél : ").append(c.getTelephone() == null ? "-" : c.getTelephone())
                        .append(" | ").append(c.getVille() == null ? "-" : c.getVille())
                        .append(c.getPays() != null ? " (" + c.getPays().getNom() + ")" : "")
                        .append(c.getTypeClient() != null ? " | type : " + c.getTypeClient().getNom() : "")
                        .append("\n");
            }
            return sb.toString();
        }
    }

    // ══════════════════════════ Fiche client ══════════════════════════

    @Component
    public static class ObtenirClient extends AbstractOutil {

        private final ClientRepository clientRepository;
        private final DossierRepository dossierRepository;
        private final OrdreTransitRepository ordreTransitRepository;
        private final Resolveur resolveur;

        public ObtenirClient(ClientRepository clientRepository,
                             DossierRepository dossierRepository,
                             OrdreTransitRepository ordreTransitRepository,
                             Resolveur resolveur) {
            super("obtenir_client",
                    "Fiche complète d'un client : coordonnées, NIF, puis ses dossiers de transit (avec phase et "
                            + "statut) et ses ordres de transit. Utilise cet outil dès que l'utilisateur pose une "
                            + "question sur un client précis (« combien de dossiers a X ? »).",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str("Nom, identifiant, NIF ou STAT du client."))),
                            "reference"),
                    NiveauDanger.LECTURE,
                    Resolveur.NIVEAU_LECTURE);
            this.clientRepository = clientRepository;
            this.dossierRepository = dossierRepository;
            this.ordreTransitRepository = ordreTransitRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Lire la fiche du client " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional(readOnly = true)
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Client c = resolveur.client(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            StringBuilder sb = new StringBuilder("Client : ").append(Resolveur.libelle(c))
                    .append(" (identifiant ").append(c.getId()).append(")\n");
            sb.append("- Email : ").append(c.getEmail() == null ? "-" : c.getEmail())
                    .append(" | Téléphone : ").append(c.getTelephone() == null ? "-" : c.getTelephone())
                    .append(" | Fax : ").append(c.getFax() == null ? "-" : c.getFax()).append("\n");
            sb.append("- NIF : ").append(c.getNif() == null ? "-" : c.getNif())
                    .append(" | STAT : ").append(c.getStat() == null ? "-" : c.getStat()).append("\n");
            sb.append("- Adresse : ").append(c.getAdresse() == null ? "-" : c.getAdresse()).append("\n");
            sb.append("- Ville : ").append(c.getVille() == null ? "-" : c.getVille())
                    .append(" | Code postal : ").append(c.getCpClient() == null ? "-" : c.getCpClient())
                    .append(" | Pays : ").append(c.getPays() != null ? c.getPays().getNom() : "-").append("\n");
            sb.append("- Type de client : ").append(c.getTypeClient() != null ? c.getTypeClient().getNom() : "-")
                    .append(" | Client depuis le ").append(OutilsDate.lisible(c.getDateCreation())).append("\n");

            List<Dossier> dossiers = dossierRepository.findAll().stream()
                    .filter(d -> d.getClient() != null && d.getClient().getId().equals(c.getId()))
                    .toList();
            sb.append("- Dossiers de transit rattachés : ").append(dossiers.size()).append("\n");
            dossiers.stream().limit(15).forEach(d -> sb.append("    · n° ").append(d.getNumeroDossier())
                    .append(" | phase : ").append(d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "-")
                    .append(" | statut : ").append(d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "-")
                    .append(" | échéance : ").append(OutilsDate.lisible(d.getEcheance()))
                    .append("\n"));
            if (dossiers.size() > 15) {
                sb.append("    … et ").append(dossiers.size() - 15).append(" autre(s).\n");
            }

            List<OrdreTransit> ots = ordreTransitRepository.findAll().stream()
                    .filter(ot -> ot.getClient() != null && ot.getClient().getId().equals(c.getId()))
                    .toList();
            sb.append("- Ordres de transit : ").append(ots.size()).append("\n");
            ots.stream().limit(15).forEach(ot -> sb.append("    · OT n° ").append(ot.getNumeroOrdreTransit())
                    .append(" | commande : ").append(ot.getNumeroCommande() == null ? "-" : ot.getNumeroCommande())
                    .append(" | statut : ").append(ot.getStatutOt() != null ? ot.getStatutOt().getNom() : "-")
                    .append(" | créé le ").append(OutilsDate.lisible(ot.getDateCreation()))
                    .append("\n"));

            return sb.toString();
        }
    }

    // ══════════════════════════ Création ══════════════════════════

    @Component
    public static class CreerClient extends AbstractOutil {

        private final ClientService clientService;
        private final PaysRepository paysRepository;
        private final TypeClientRepository typeClientRepository;

        public CreerClient(ClientService clientService,
                           PaysRepository paysRepository,
                           TypeClientRepository typeClientRepository) {
            super("creer_client",
                    "Crée une fiche client. Demande à l'utilisateur toutes les informations manquantes "
                            + "(nom, prénom pour une personne physique, email, téléphone, NIF, STAT, ville) "
                            + "avant d'appeler cet outil : n'invente jamais de données. Si l'email, le NIF ou le "
                            + "STAT existe déjà, la création échouera. Opération qui écrit en base : confirmation obligatoire.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("nom", str("Nom de famille ou raison sociale. Obligatoire.")),
                            OutilsArgs.p("prenom", str("Prénom (clients physiques).")),
                            OutilsArgs.p("email", str("Adresse e-mail (unique dans l'application).")),
                            OutilsArgs.p("telephone", str("Numéro de téléphone.")),
                            OutilsArgs.p("adresse", str("Adresse postale.")),
                            OutilsArgs.p("ville", str("Ville.")),
                            OutilsArgs.p("cp_client", str("Code postal.")),
                            OutilsArgs.p("nif", str("Numéro d'identification fiscale (unique).")),
                            OutilsArgs.p("stat", str("Numéro statistique (unique).")),
                            OutilsArgs.p("fax", str("Numéro de fax.")),
                            OutilsArgs.p("pays", str("Nom du pays.")),
                            OutilsArgs.p("type_client", str("Type de client (ex: « Importateur », « Exportateur »)."))),
                            "nom"),
                    NiveauDanger.MODIFICATION,
                    Resolveur.NIVEAU_ECRITURE);
            this.clientService = clientService;
            this.paysRepository = paysRepository;
            this.typeClientRepository = typeClientRepository;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Créer le client " + libelleArguments(arguments);
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Client client = new Client();
            client.setNom(OutilsArgs.texteObligatoire(arguments, "nom").trim());
            client.setPrenom(OutilsArgs.texte(arguments, "prenom"));
            client.setEmail(OutilsArgs.texte(arguments, "email"));
            client.setTelephone(OutilsArgs.texte(arguments, "telephone"));
            client.setAdresse(OutilsArgs.texte(arguments, "adresse"));
            client.setVille(OutilsArgs.texte(arguments, "ville"));
            client.setCpClient(OutilsArgs.texte(arguments, "cp_client"));
            client.setNif(OutilsArgs.texte(arguments, "nif"));
            client.setStat(OutilsArgs.texte(arguments, "stat"));
            client.setFax(OutilsArgs.texte(arguments, "fax"));

            String pays = OutilsArgs.texte(arguments, "pays");
            if (pays != null) {
                Pays p = paysRepository.findAll().stream()
                        .filter(x -> OutilsDossier.equalsIgnorantAccents(x.getNom(), pays))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Pays inconnu : « " + pays
                                + " ». Utilise lister_referentiels type=pays."));
                client.setPays(p);
            }

            String type = OutilsArgs.texte(arguments, "type_client");
            if (type != null) {
                TypeClient t = typeClientRepository.findAll().stream()
                        .filter(x -> OutilsDossier.equalsIgnorantAccents(x.getNom(), type))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Type de client inconnu : « " + type
                                + " ». Utilise lister_referentiels pour connaître les valeurs."));
                client.setTypeClient(t);
            }

            Client enregistre = clientService.createClient(client);

            StringBuilder sb = new StringBuilder("Client créé avec succès : ")
                    .append(Resolveur.libelle(enregistre)).append(" (identifiant ").append(enregistre.getId()).append(").");
            if (enregistre.getEmail() != null) {
                sb.append(" E-mail : ").append(enregistre.getEmail()).append(".");
            }
            sb.append(" Tu peux maintenant lui rattacher des dossiers et des ordres de transit.");
            return sb.toString();
        }
    }

    // ══════════════════════════ Suppression ══════════════════════════

    @Component
    public static class SupprimerClient extends AbstractOutil {

        private final ClientService clientService;
        private final DossierRepository dossierRepository;
        private final OrdreTransitRepository ordreTransitRepository;
        private final Resolveur resolveur;

        public SupprimerClient(ClientService clientService,
                               DossierRepository dossierRepository,
                               OrdreTransitRepository ordreTransitRepository,
                               Resolveur resolveur) {
            super("supprimer_client",
                    "Supprime une fiche client. Action irréversible et réservée aux profils disposant du droit de "
                            + "suppression : vérifie d'abord, avec obtenir_client, que le client n'a pas de dossier "
                            + "ni d'ordre de transit rattachés, et ne l'appelle qu'après demande explicite de l'utilisateur.",
                    schema(OutilsArgs.props(
                            OutilsArgs.p("reference", str("Nom, identifiant, NIF ou STAT du client."))),
                            "reference"),
                    NiveauDanger.CRITIQUE,
                    Resolveur.NIVEAU_SUPPRESSION);
            this.clientService = clientService;
            this.dossierRepository = dossierRepository;
            this.ordreTransitRepository = ordreTransitRepository;
            this.resolveur = resolveur;
        }

        @Override
        public String resume(Map<String, Object> arguments, ContexteChat contexte) {
            return "Supprimer définitivement le client " + OutilsArgs.texte(arguments, "reference", "?");
        }

        @Override
        @Transactional
        public String executer(Map<String, Object> arguments, ContexteChat contexte) {
            Client c = resolveur.client(OutilsArgs.texteObligatoire(arguments, "reference"), contexte);

            // Suppression non cascade en base : on refuse explicitement plutôt que de laisser
            // remonter une violation de clé étrangère à l'utilisateur.
            long dossiers = dossierRepository.findAll().stream()
                    .filter(d -> d.getClient() != null && d.getClient().getId().equals(c.getId()))
                    .count();
            if (dossiers > 0) {
                return "OPÉRATION IMPOSSIBLE : le client " + Resolveur.libelle(c)
                        + " porte encore " + dossiers + " dossier(s). "
                        + "Supprimez ou réaffectez ces dossiers avant de supprimer la fiche.";
            }
            long ordres = ordreTransitRepository.countByClientId(c.getId());
            if (ordres > 0) {
                return "OPÉRATION IMPOSSIBLE : le client " + Resolveur.libelle(c)
                        + " est encore rattaché à " + ordres + " ordre(s) de transit. "
                        + "La suppression d'une fiche client liée est interdite.";
            }

            clientService.deleteClient(c.getId());

            return "La fiche client " + Resolveur.libelle(c) + " (id " + c.getId() + ") a été supprimée.";
        }
    }

    static String libelleArguments(Map<String, Object> arguments) {
        String nom = OutilsArgs.texte(arguments, "nom", "?");
        String prenom = OutilsArgs.texte(arguments, "prenom");
        return prenom == null ? nom : nom + " " + prenom;
    }
}
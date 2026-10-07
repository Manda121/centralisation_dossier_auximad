package com.auximad.ai;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moteur RAG de l'application.
 *
 * Il assemble, pour chaque question de l'utilisateur :
 *  1. la connaissance documentaire de l'application (BaseConnaissance) ;
 *  2. un « instantané » des chiffres du jour ;
 *  3. les lignes réelles de la base correspondant à l'intention détectée
 *     (dossiers, clients, ordres de transit, documents, demandes inter-agences, historique…).
 *
 * Les valeurs extraites de la base sont envoyées telles quelles au modèle : c'est ce qui
 * garantit que les réponses portent sur l'état à jour du projet et non sur des approximations.
 */
@Service
public class RagService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int TAILLE_PAGE = 15;
    private static final int TAILLE_MAX_CARACTERES = 6500;

    private static final Pattern NUMERO_DOSSIER =
            Pattern.compile("\\b(\\d{1,3}-\\d{2}-\\d{2}-\\d-\\d{4})\\b");

    /** Mots vides ou de question, à retirer avant d'interroger la recherche plein texte. */
    private static final Set<String> MOTS_VIDES = Set.of(
            "combien", "combien?", "quel", "quelle", "quels", "quelles", "quoi", "qui", "que", "dont",
            "est", "sont", "etre", "etait", "avoir", "a", "as", "ont", "peut", "peux", "doit", "faut",
            "pour", "par", "dans", "sur", "avec", "sans", "sous", "vers", "chez", "entre", "depuis",
            "les", "des", "une", "un", "le", "la", "du", "au", "aux", "et", "ou", "en", "y", "ce",
            "cet", "cette", "ces", "mon", "ma", "mes", "ton", "ta", "tes", "son", "sa", "ses",
            "notre", "nos", "votre", "vos", "leur", "je", "tu", "il", "elle", "nous", "vous", "ils",
            "elles", "ne", "pas", "plus", "tres", "tout", "tous", "toute", "toutes", "autre",
            "autres", "meme", "aussi", "encore", "si", "car", "donc", "mais", "comme", "afin",
            "lors", "quand", "quelque", "quelques", "peu", "bien", "voir", "donne", "donnees",
            "donnee", "dis", "dire", "sais", "stp", "merci", "bonjour", "salut", "hello", "aide");

    /** Termes génériques d'entité, retirés de la requête FTS car absents des index. */
    private static final Set<String> TERMES_GENERIQUES = Set.of(
            "dossier", "dossiers", "client", "clients", "ordre", "ordres", "transit",
            "marchandise", "marchandises", "conteneur", "conteneurs", "document", "documents",
            "doc", "docs", "demande", "demandes", "historique", "agence", "agences",
            "interagence", "application", "appli", "systeme", "donnee", "donnees", "information");

    private final DossierRepository dossierRepository;
    private final ClientRepository clientRepository;
    private final OrdreTransitRepository ordreTransitRepository;
    private final MarchandiseRepository marchandiseRepository;
    private final ConteneurRepository conteneurRepository;
    private final DocumentRepository documentRepository;
    private final RemiseRepository remiseRepository;
    private final DemandeInteragenceRepository demandeInteragenceRepository;
    private final HistoriqueRepository historiqueRepository;
    private final ActionRepository actionRepository;
    private final PhaseDossierRepository phaseRepository;
    private final StatutDossierRepository statutRepository;
    private final AgenceRepository agenceRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final TypeRegimeRepository typeRegimeRepository;
    private final PaysRepository paysRepository;
    private final BureauRepository bureauRepository;
    private final IncontermeRepository incontermeRepository;
    private final NatureMarchandiseRepository natureRepository;
    private final UtilisateurRepository utilisateurRepository;

    public RagService(DossierRepository dossierRepository,
                      ClientRepository clientRepository,
                      OrdreTransitRepository ordreTransitRepository,
                      MarchandiseRepository marchandiseRepository,
                      ConteneurRepository conteneurRepository,
                      DocumentRepository documentRepository,
                      RemiseRepository remiseRepository,
                      DemandeInteragenceRepository demandeInteragenceRepository,
                      HistoriqueRepository historiqueRepository,
                      ActionRepository actionRepository,
                      PhaseDossierRepository phaseRepository,
                      StatutDossierRepository statutRepository,
                      AgenceRepository agenceRepository,
                      TypeProductionRepository typeProductionRepository,
                      TypeDocumentRepository typeDocumentRepository,
                      TypeRegimeRepository typeRegimeRepository,
                      PaysRepository paysRepository,
                      BureauRepository bureauRepository,
                      IncontermeRepository incontermeRepository,
                      NatureMarchandiseRepository natureRepository,
                      UtilisateurRepository utilisateurRepository) {
        this.dossierRepository = dossierRepository;
        this.clientRepository = clientRepository;
        this.ordreTransitRepository = ordreTransitRepository;
        this.marchandiseRepository = marchandiseRepository;
        this.conteneurRepository = conteneurRepository;
        this.documentRepository = documentRepository;
        this.remiseRepository = remiseRepository;
        this.demandeInteragenceRepository = demandeInteragenceRepository;
        this.historiqueRepository = historiqueRepository;
        this.actionRepository = actionRepository;
        this.phaseRepository = phaseRepository;
        this.statutRepository = statutRepository;
        this.agenceRepository = agenceRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.typeDocumentRepository = typeDocumentRepository;
        this.typeRegimeRepository = typeRegimeRepository;
        this.paysRepository = paysRepository;
        this.bureauRepository = bureauRepository;
        this.incontermeRepository = incontermeRepository;
        this.natureRepository = natureRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ══════════════════════════════ Point d'entrée ══════════════════════════════

    @Transactional(readOnly = true)
    public ContexteRag chercher(String question, ContexteChat contexte) {
        String norm = BaseConnaissance.normaliser(question);
        Intention intention = detecterIntention(norm);
        List<ContexteRag.Source> sources = new ArrayList<>();
        StringBuilder blocs = new StringBuilder();

        // 1. Connaissance de l'application
        ajouterConnaissance(blocs, sources, norm, question);

        // 2. Instantané des chiffres du jour
        ajouterInstantane(blocs, sources, contexte, intention);

        // 3. Données réelles correspondant à l'intention
        try {
            ajouterDonnees(blocs, sources, norm, question, contexte, intention);
        } catch (RuntimeException e) {
            blocs.append("\n[Les données en direct n'ont pas pu être récupérées : ").append(e.getMessage()).append("]\n");
        }

        return new ContexteRag(tronquer(blocs.toString(), TAILLE_MAX_CARACTERES), sources);
    }

    // ══════════════════════════════ Détection d'intention ══════════════════════════════

    /** Intentions détectées dans la question. */
    private record Intention(boolean dossier, boolean client, boolean transit, boolean marchandise,
                            boolean conteneur, boolean document, boolean interagence,
                            boolean historique, boolean referentiel, boolean statistique, boolean aide) {

        static Intention aucune() {
            return new Intention(false, false, false, false, false, false, false, false, false, false, false);
        }

        boolean vide() {
            return !(dossier || client || transit || marchandise || conteneur || document
                    || interagence || historique || referentiel || statistique || aide);
        }
    }

    private static Intention detecterIntention(String norm) {
        Intention base = Intention.aucune();
        if (contient(norm, "dossier", "dossiers", "dossiermer", "sousdossier")) {
            base = new Intention(true, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "client", "clients", "donneurdordre", "expediteur", "destinataire", "adherent")) {
            base = new Intention(base.dossier, true, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "ordre", "ordres", "transit", "ot ")) {
            base = new Intention(base.dossier, base.client, true, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "marchandise", "marchandises", "cargaison", "colis", "fret", "tonnage")) {
            base = new Intention(base.dossier, base.client, base.transit, true, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "conteneur", "conteneurs", "container")) {
            base = new Intention(base.dossier, base.client, base.transit, base.marchandise, true,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "document", "documents", "doc", "docs", "facture", "remise", "piece", "pdf", "upload")) {
            base = new Intention(base.dossier, base.client, base.transit, base.marchandise, base.conteneur,
                    true, base.interagence, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "interagence", "interagences", "interagenc", "demande", "demandes", "sousdossier", "dossiermere")) {
            base = new Intention(true, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, true, base.historique, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "historique", "journal", "audit", "trace", "connexion", "deconnexion")) {
            base = new Intention(base.dossier, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, true, base.referentiel, base.statistique, base.aide);
        }
        if (contient(norm, "phase", "phases", "statut", "statuts", "referentiel", "typeproduction",
                "typeregime", "incoterm", "bureau", "pays", "naturemarchandise", "unitemesure")) {
            base = new Intention(base.dossier, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, true, base.statistique, base.aide);
        }
        if (contient(norm, "combien", "nombre", "statistique", "statistiques", "total", "tableau", "resume",
                "situation", "etat", "chiffre", "indicateur", "classement", "top", "repartition", "etatdeslieux")) {
            base = new Intention(true, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, true, base.aide);
        }
        if (contient(norm, "comment", "pourquoi", "peut", "peux", "fonctionne", "aide", "aider", "explique",
                "procedure", "etapes", "procedure", "ou se trouve", "ou trouver")) {
            base = new Intention(base.dossier, base.client, base.transit, base.marchandise, base.conteneur,
                    base.document, base.interagence, base.historique, base.referentiel, base.statistique, true);
        }
        return base;
    }

    private static boolean contient(String norm, String... termes) {
        for (String t : termes) {
            if (norm.contains(t)) {
                return true;
            }
        }
        return false;
    }

    // ══════════════════════════════ Fragments ══════════════════════════════

    private void ajouterConnaissance(StringBuilder blocs, List<ContexteRag.Source> sources,
                                      String norm, String question) {
        StringBuilder sb = new StringBuilder("\n[FONCTIONNEMENT DE L'APPLICATION]\n");
        for (BaseConnaissance.Fragment f : BaseConnaissance.rechercher(norm, 4)) {
            sb.append("- ").append(f.titre()).append(" : ").append(f.texte()).append("\n");
            sources.add(new ContexteRag.Source("documentation", f.titre(), null));
        }
        blocs.append(sb);
    }

    private void ajouterInstantane(StringBuilder blocs, List<ContexteRag.Source> sources,
                                   ContexteChat contexte, Intention intention) {
        Integer agenceId = porteeDossiers(contexte);
        long total = dossierRepository.count();
        long depuis30 = dossierRepository.compterDossiersDepuis(agenceId,
                LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        long incomplet = dossierRepository.compterDossiersIncomplets(agenceId);
        long clients = clientRepository.count();
        long ot = ordreTransitRepository.count();
        long demandesEnAttente = demandeInteragenceRepository.count();

        StringBuilder sb = new StringBuilder("\n[ETAT ACTUEL DES DONNEES — relevé du ")
                .append(LocalDateTime.now().format(DATE_HEURE)).append("]\n");
        sb.append("- Nous sommes le ").append(LocalDate.now().format(DATE)).append(".\n");
        sb.append("- Utilisateur connecté : ").append(contexte.nomComplet())
                .append(" (rôle ").append(contexte.role())
                .append(", niveau d'autorisation ").append(contexte.niveauAutorisation()).append(")\n");
        sb.append("- Périmètre visible : ")
                .append(contexte.peutVoirToutesAgences() ? "toutes les agences" : "agence " + contexte.nomAgence()).append("\n");
        sb.append("- Dossiers : ").append(total).append(" au total")
                .append(contexte.peutVoirToutesAgences() ? "" : " dans le périmètre visible")
                .append(", dont ").append(depuis30).append(" créés sur les 30 derniers jours.\n");
        sb.append("- Clients : ").append(clients).append(" | Ordres de transit : ").append(ot)
                .append(" | Demandes inter-agences : ").append(demandesEnAttente).append(".\n");
        sb.append("- Dossiers incomplets (document obligatoire manquant) : ").append(incomplet).append("\n");

        blocs.append(sb);
        sources.add(new ContexteRag.Source("donnees", "État actuel de l'application",
                "relevé du " + LocalDateTime.now().format(DATE_HEURE)));
    }

    private void ajouterDonnees(StringBuilder blocs, List<ContexteRag.Source> sources,
                                String norm, String question, ContexteChat contexte, Intention intention) {
        // Si la question ne vise rien de précis, on ne surcharge pas le prompt.
        Intention i = intention;
        if (i.vide()) {
            return;
        }

        // Un numéro de dossier explicite dans la question → on va droit au but.
        String numero = extraireNumeroDossier(question);
        if (numero != null) {
            Optional<Dossier> d = dossierRepository.findByNumeroDossier(numero);
            if (d.isPresent()) {
                blocs.append("\n").append(decrireDossierComplet(d.get()));
                sources.add(new ContexteRag.Source("base", "Dossier " + numero, null));
                return;
            }
        }

        if (i.dossier()) {
            blocs.append(rechercheDossiers(norm, contexte, sources));
            if (i.statistique()) {
                blocs.append(repartitionDossiers(contexte, sources));
            }
        }
        if (i.client()) {
            blocs.append(rechercheClients(norm, sources));
        }
        if (i.transit()) {
            blocs.append(rechercheOrdresTransit(norm, sources));
        }
        if (i.marchandise()) {
            blocs.append(marchandisesRecentes(sources));
        }
        if (i.conteneur()) {
            blocs.append(conteneursRecents(sources));
        }
        if (i.document()) {
            blocs.append(etatDocuments(contexte, sources));
        }
        if (i.interagence()) {
            blocs.append(demandesInteragence(contexte, sources));
        }
        if (i.historique()) {
            blocs.append(activiteRecente(sources));
        }
        if (i.referentiel()) {
            blocs.append(fragmentsReferentiel(norm, sources));
        }
    }

    // ─────────────── Dossiers ───────────────

    private String rechercheDossiers(String norm, ContexteChat contexte, List<ContexteRag.Source> sources) {
        Integer agenceId = porteeDossiers(contexte);
        String requete = requeteFts(norm);
        Integer phaseId = resoudreIdParNom(norm, referentielsPhase());
        Integer statutId = resoudreIdParNom(norm, referentielsStatut());

        Page<Dossier> page = dossierRepository.searchDossiers(
                requete, phaseId, statutId, null, null, null, null, null, null, agenceId, null,
                PageRequest.of(0, TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "date_creation")));

        StringBuilder sb = new StringBuilder("\n[DOSSIERS");
        if (requete != null) {
            sb.append(" — recherche « ").append(requete).append("»");
        }
        if (phaseId != null) {
            sb.append(" — phase filtrée");
        }
        if (statutId != null) {
            sb.append(" — statut filtré");
        }
        sb.append("]\n");
        sb.append(page.getTotalElements()).append(" dossier(s) correspondent. ")
                .append("Affichage des ").append(page.getNumberOfElements()).append(" plus récents :\n");

        for (Dossier d : page.getContent()) {
            sb.append("- ").append(decrireDossier(d)).append("\n");
        }
        sources.add(new ContexteRag.Source("base", "Dossiers", page.getTotalElements() + " résultat(s)"));
        return sb.toString();
    }

    private String repartitionDossiers(ContexteChat contexte, List<ContexteRag.Source> sources) {
        Integer agenceId = porteeDossiers(contexte);
        StringBuilder sb = new StringBuilder("\n[REPARTITION DES DOSSIERS]\n");

        sb.append("Par phase : ");
        List<Object[]> phases = dossierRepository.compterDossiersParPhase(agenceId);
        if (phases.isEmpty()) {
            sb.append("aucune donnée");
        }
        for (Object[] l : phases) {
            sb.append(l[0]).append(" = ").append(l[1]).append(" ; ");
        }

        sb.append("\nPar statut : ");
        List<Object[]> statuts = dossierRepository.compterDossiersParStatut(agenceId);
        for (Object[] l : statuts) {
            sb.append(l[0]).append(" = ").append(l[1]).append(" ; ");
        }

        if (contexte.peutVoirToutesAgences()) {
            sb.append("\nPar agence : ");
            for (Object[] l : dossierRepository.compterDossiersParAgence()) {
                sb.append(l[0]).append(" = ").append(l[1]).append(" ; ");
            }
        }

        LocalDate today = LocalDate.now();
        sb.append("\nCréations : aujourd'hui = ")
                .append(dossierRepository.compterDossiersDepuis(agenceId, today.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(" ; ce mois-ci = ")
                .append(dossierRepository.compterDossiersDepuis(agenceId, today.withDayOfMonth(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(" ; sur 30 jours = ")
                .append(dossierRepository.compterDossiersDepuis(agenceId, LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append(".");

        sources.add(new ContexteRag.Source("base", "Répartition des dossiers", null));
        return sb.toString();
    }

    private String decrireDossierComplet(Dossier d) {
        StringBuilder sb = new StringBuilder("\n[DETAIL D'UN DOSSIER]\n");
        sb.append(decrireDossier(d)).append("\n");
        sb.append("  - Type de production : ").append(d.getTypeProduction() != null ? d.getTypeProduction().getNom() : "non défini").append("\n");
        sb.append("  - Pays origine : ").append(d.getPaysOrigine() != null ? d.getPaysOrigine().getNom() : "-")
                .append(" | Pays destination : ").append(d.getPaysDestination() != null ? d.getPaysDestination().getNom() : "-").append("\n");
        sb.append("  - Incoterm : ").append(d.getInconterme() != null ? d.getInconterme().getAbreviation() : "-").append("\n");
        sb.append("  - Départ : ").append(valeur(d.getDepart())).append(" | Lieu de paiement du fret : ")
                .append(valeur(d.getLieuPaiementFret())).append("\n");
        sb.append("  - Pour le compte de : ").append(valeur(d.getPourCompte())).append("\n");
        sb.append("  - Prépayé : ").append(Boolean.TRUE.equals(d.getEstPrepaye()) ? "oui" : "non")
                .append(" | Provisionné : ").append(Boolean.TRUE.equals(d.getEstProvisionne()) ? "oui" : "non")
                .append(" | Provision demandée : ").append(valeur(d.getProvisionDemandee())).append("\n");
        if (Boolean.TRUE.equals(d.getEstDossierMere())) {
            sb.append("  - C'est un DOSSIÈRE MÈRE (il a des sous-dossiers).\n");
        }
        if (Boolean.TRUE.equals(d.getEstSousDossier())) {
            sb.append("  - C'est un SOUS-DOSSIER du dossier mère ")
                    .append(valeur(d.getNumeroDossierMere())).append(" — motif : ").append(valeur(d.getMotifInteragence())).append("\n");
        }

        List<Document> documents = documentRepository.findByDossierIdAndEstActifTrue(d.getId());
        sb.append("  - Documents remis (").append(documents.size()).append(") : ");
        if (documents.isEmpty()) {
            sb.append("aucun");
        }
        Map<Integer, LocalDateTime> dates = new HashMap<>();
        for (Remise r : remiseRepository.findByDossierId(d.getId())) {
            dates.put(r.getTypeDocument().getId(), r.getDateRemise());
        }
        for (Document doc : documents) {
            Integer tid = doc.getTypeDocument().getId();
            sb.append(doc.getTypeDocument().getNom())
                    .append(dates.containsKey(tid) ? " (remis le " + fmtDate(dates.get(tid)) + ")" : "")
                    .append(doc.getNomFichier() != null ? " [fichier: " + doc.getNomFichier() + "]" : "")
                    .append(" | ");
        }
        sb.append("\n");

        List<Conteneur> conteneurs = conteneurRepository.findByDossierId(d.getId());
        sb.append("  - Conteneurs : ");
        if (conteneurs.isEmpty()) {
            sb.append("aucun");
        }
        for (Conteneur c : conteneurs) {
            sb.append(c.getNumeroContainer())
                    .append(c.getTypeContainer() != null ? " (" + c.getTypeContainer().getNom() + ")" : "")
                    .append(" | ");
        }
        sb.append("\n");

        List<DemandeInteragence> demandes = demandeInteragenceRepository.findByDossierIdOrderByDateDemandeDesc(d.getId());
        sb.append("  - Demandes inter-agences : ");
        if (demandes.isEmpty()) {
            sb.append("aucune");
        }
        for (DemandeInteragence di : demandes) {
            sb.append("vers ").append(di.getAgenceDestinataire() != null ? di.getAgenceDestinataire().getNom() : "?")
                    .append(" — ").append(di.getStatut())
                    .append(" (demandée le ").append(fmtDate(di.getDateDemande())).append(") | ");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String decrireDossier(Dossier d) {
        return "Dossier n° " + valeur(d.getNumeroDossier())
                + " | id " + d.getId()
                + " | client : " + nomClient(d.getClient())
                + " | agence : " + (d.getAgence() != null ? d.getAgence().getNom() : "-")
                + " | phase : " + (d.getPhaseDossier() != null ? d.getPhaseDossier().getNom() : "aucune")
                + " | statut : " + (d.getStatutDossier() != null ? d.getStatutDossier().getNom() : "aucun")
                + " | OT : " + (d.getOrdreTransit() != null ? String.valueOf(d.getOrdreTransit().getNumeroOrdreTransit()) : "aucun")
                + " | échéance : " + fmtDate(d.getEcheance())
                + " | livraison : " + fmtDate(d.getDateLivraison())
                + " | groupage : " + (Boolean.TRUE.equals(d.getEstGroupage()) ? "oui" : "non")
                + " | créé le " + fmtDate(d.getDateCreation());
    }

    // ─────────────── Clients ───────────────

    private String rechercheClients(String norm, List<ContexteRag.Source> sources) {
        String requete = requeteFts(norm);
        Page<Client> page = clientRepository.searchClients(
                requete, null, null, null, null, null, null, null, null, null, null, null, null,
                PageRequest.of(0, TAILLE_PAGE, Sort.by("nom").ascending()));

        StringBuilder sb = new StringBuilder("\n[CLIENTS");
        if (requete != null) {
            sb.append(" — recherche « ").append(requete).append("»");
        }
        sb.append("]\n").append(page.getTotalElements()).append(" client(s) trouvé(s) :\n");
        for (Client c : page.getContent()) {
            sb.append("- id ").append(c.getId()).append(" | ").append(nomClient(c))
                    .append(" | email : ").append(valeur(c.getEmail()))
                    .append(" | tél : ").append(valeur(c.getTelephone()))
                    .append(" | NIF : ").append(valeur(c.getNif()))
                    .append(" | STAT : ").append(valeur(c.getStat()))
                    .append(" | ville : ").append(valeur(c.getVille()))
                    .append(" | pays : ").append(c.getPays() != null ? c.getPays().getNom() : "-")
                    .append(" | type : ").append(c.getTypeClient() != null ? c.getTypeClient().getNom() : "-")
                    .append("\n");
        }
        sources.add(new ContexteRag.Source("base", "Clients", page.getTotalElements() + " résultat(s)"));
        return sb.toString();
    }

    // ─────────────── Ordres de transit ───────────────

    private String rechercheOrdresTransit(String norm, List<ContexteRag.Source> sources) {
        String requete = requeteFts(norm);
        Integer numero = extraireEntier(norm);

        Page<OrdreTransit> page = ordreTransitRepository.searchOrdreTransits(
                requete, numero, null, null, null, null, null, null, null, null, null,
                // Requête native : le tri porte sur une colonne SQL.
                PageRequest.of(0, TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "date_creation")));

        StringBuilder sb = new StringBuilder("\n[ORDRES DE TRANSIT]\n")
                .append(page.getTotalElements()).append(" OT trouvé(s) :\n");
        for (OrdreTransit ot : page.getContent()) {
            sb.append("- OT n° ").append(valeur(ot.getNumeroOrdreTransit()))
                    .append(" | commande : ").append(valeur(ot.getNumeroCommande()))
                    .append(" | client : ").append(nomClient(ot.getClient()))
                    .append(" | expéditeur : ").append(nomClient(ot.getExpediteur()))
                    .append(" | destinataire : ").append(nomClient(ot.getDestinataire()))
                    .append(" | type de production : ").append(ot.getTypeProduction() != null ? ot.getTypeProduction().getNom() : "-")
                    .append(" | régime : ").append(ot.getTypeRegime() != null ? ot.getTypeRegime().getNom() : "-")
                    .append(" | statut : ").append(ot.getStatutOt() != null ? ot.getStatutOt().getNom() : "-")
                    .append(" | bureau : ").append(ot.getBureauDedouanement() != null ? ot.getBureauDedouanement().getNom() : "-")
                    .append(" | destination : ").append(valeur(ot.getDestinationFinale()))
                    .append(" | assuré : ").append(Boolean.TRUE.equals(ot.getEstAssure()) ? "oui" : "non")
                    .append(" | créé le ").append(fmtDate(ot.getDateCreation()))
                    .append("\n");
        }
        sources.add(new ContexteRag.Source("base", "Ordres de transit", page.getTotalElements() + " résultat(s)"));
        return sb.toString();
    }

    // ─────────────── Marchandises / conteneurs ───────────────

    private String marchandisesRecentes(List<ContexteRag.Source> sources) {
        StringBuilder sb = new StringBuilder("\n[MARCHANDISES]\n");
        List<Marchandise> toutes = marchandiseRepository.findAll();
        sb.append(toutes.size()).append(" marchandise(s) enregistrée(s). Dernières entrées :\n");
        toutes.stream().limit(TAILLE_PAGE).forEach(m -> sb.append("- OT n° ")
                .append(m.getOrdreTransit() != null ? m.getOrdreTransit().getNumeroOrdreTransit() : "?")
                .append(" | ").append(m.getOrdreTransit() != null && m.getOrdreTransit().getNatureMarchandise() != null
                        ? m.getOrdreTransit().getNatureMarchandise().getNom() : "-")
                .append(" | marque : ").append(valeur(m.getMarque()))
                .append(" | modèle : ").append(valeur(m.getModele()))
                .append(" | quantité : ").append(valeur(m.getQuantite()))
                .append(m.getUniteMesure() != null ? " " + m.getUniteMesure().getNom() : "")
                .append(" | poids : ").append(valeur(m.getPoids()))
                .append(" | volume : ").append(valeur(m.getVolume()))
                .append("\n"));
        sources.add(new ContexteRag.Source("base", "Marchandises", toutes.size() + " enregistrement(s)"));
        return sb.toString();
    }

    private String conteneursRecents(List<ContexteRag.Source> sources) {
        StringBuilder sb = new StringBuilder("\n[CONTENEURS]\n");
        List<Conteneur> conteneurs = conteneurRepository.findAll();
        sb.append(conteneurs.size()).append(" conteneur(s) enregistré(s) :\n");
        conteneurs.stream().limit(TAILLE_PAGE).forEach(c -> sb.append("- ").append(c.getNumeroContainer())
                .append(c.getTypeContainer() != null ? " (" + c.getTypeContainer().getNom() + ")" : "")
                .append(valeur(c.getDescription()) != null ? " — " + c.getDescription() : "")
                .append("\n"));
        sources.add(new ContexteRag.Source("base", "Conteneurs", conteneurs.size() + " conteneur(s)"));
        return sb.toString();
    }

    // ─────────────── Documents ───────────────

    private String etatDocuments(ContexteChat contexte, List<ContexteRag.Source> sources) {
        Integer agenceId = porteeDossiers(contexte);
        StringBuilder sb = new StringBuilder("\n[ETAT DES DOCUMENTS]\n");
        sb.append("Documents enregistrés : ").append(documentRepository.count()).append(".\n");

        List<Dossier> incomplets = dossierRepository.findDossiersSansDocumentsComplets(agenceId);
        sb.append(incomplets.size()).append(" dossier(s) ont au moins un document obligatoire manquant :\n");
        incomplets.stream().limit(TAILLE_PAGE).forEach(d -> sb.append("- ").append(valeur(d.getNumeroDossier()))
                .append(" | client : ").append(nomClient(d.getClient()))
                .append(" | type de production : ").append(d.getTypeProduction() != null ? d.getTypeProduction().getNom() : "-")
                .append("\n"));

        LocalDateTime maintenant = LocalDateTime.now();
        List<Remise> remisesUrgentes = agenceId != null
                ? remiseRepository.findRemisesUrgentesParAgence(maintenant, maintenant.plusDays(7), agenceId)
                : remiseRepository.findRemisesUrgentes(maintenant, maintenant.plusDays(7));
        sb.append("\nRemises attendues sous 7 jours (").append(remisesUrgentes.size()).append(") :\n");
        for (Remise r : remisesUrgentes) {
            if (r == null) {
                continue;
            }
            sb.append("- Dossier ").append(valeur(r.getDossier() != null ? r.getDossier().getNumeroDossier() : null))
                    .append(" | ").append(r.getTypeDocument() != null ? r.getTypeDocument().getNom() : "?")
                    .append(" | remise le ").append(fmtDate(r.getDateRemise()))
                    .append("\n");
            if (sb.length() > 20000) {
                break;
            }
        }

        sources.add(new ContexteRag.Source("base", "Documents et remises",
                incomplets.size() + " dossier(s) incomplet(s), " + remisesUrgentes.size() + " remise(s) sous 7 jours"));
        return sb.toString();
    }

    // ─────────────── Inter-agences ───────────────

    private String demandesInteragence(ContexteChat contexte, List<ContexteRag.Source> sources) {
        Integer agenceConcernee = contexte.peutVoirToutesAgences() ? null : contexte.idAgence();
        Page<DemandeInteragence> page = demandeInteragenceRepository.searchDemandes(
                null, null, null, null, null, agenceConcernee, null,
                PageRequest.of(0, TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "dateDemande")));

        StringBuilder sb = new StringBuilder("\n[DEMANDES INTER-AGENCES]\n")
                .append(page.getTotalElements()).append(" demande(s) dans le périmètre visible :\n");
        for (DemandeInteragence di : page.getContent()) {
            sb.append("- #").append(di.getId())
                    .append(" | dossier : ").append(di.getDossier() != null ? valeur(di.getDossier().getNumeroDossier()) : "?")
                    .append(" | de ").append(di.getAgenceDemandeuse() != null ? di.getAgenceDemandeuse().getNom() : "?")
                    .append(" vers ").append(di.getAgenceDestinataire() != null ? di.getAgenceDestinataire().getNom() : "?")
                    .append(" | ").append(di.getStatut())
                    .append(" | demandée le ").append(fmtDate(di.getDateDemande()))
                    .append(" | demandeur : ").append(di.getUtilisateurDemande() != null ? di.getUtilisateurDemande().getNomUtilisateur() : "-")
                    .append(" | motif : ").append(valeur(di.getMotif()))
                    .append("\n");
        }
        sources.add(new ContexteRag.Source("base", "Demandes inter-agences", page.getTotalElements() + " demande(s)"));
        return sb.toString();
    }

    // ─────────────── Historique ───────────────

    private String activiteRecente(List<ContexteRag.Source> sources) {
        Page<Historique> page = historiqueRepository.searchHistoriques(
                null, null, null, null, null, null, null, null, null, null,
                PageRequest.of(0, TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "dateAction")));

        StringBuilder sb = new StringBuilder("\n[ACTIVITE RECENTE — JOURNAL D'AUDIT]\n")
                .append(page.getTotalElements()).append(" entrée(s) au total. ")
                .append(page.getNumberOfElements()).append(" plus récentes :\n");
        for (Historique h : page.getContent()) {
            sb.append("- ").append(fmtDate(h.getDateAction()))
                    .append(" | ").append(h.getUtilisateur() != null ? h.getUtilisateur().getNomUtilisateur() : "anonyme")
                    .append(" | ").append(h.getAction() != null ? h.getAction().getNom() : "?")
                    .append(" | ").append(valeur(h.getEntite()))
                    .append(" | ").append(valeur(h.getDetails()))
                    .append("\n");
        }
        sources.add(new ContexteRag.Source("base", "Historique", page.getTotalElements() + " entrée(s)"));
        return sb.toString();
    }

    // ─────────────── Référentiels ───────────────

    private String fragmentsReferentiel(String norm, List<ContexteRag.Source> sources) {
        StringBuilder sb = new StringBuilder("\n[REFERENTIELS]\n");
        boolean utile = false;

        if (contient(norm, "phase", "phases")) {
            utile = true;
            sb.append("Phases de dossier : ");
            phaseRepository.findAll().forEach(p -> sb.append(p.getId()).append("=").append(p.getNom())
                    .append(p.getDescription() != null ? " (" + p.getDescription() + ")" : "").append(" | "));
        }
        if (contient(norm, "statut", "statuts")) {
            utile = true;
            sb.append("\nStatuts de dossier : ");
            statutRepository.findAll().forEach(s -> sb.append(s.getId()).append("=").append(s.getNom()).append(" | "));
        }
        if (contient(norm, "agence", "agences")) {
            utile = true;
            sb.append("\nAgences : ");
            agenceRepository.findAll().forEach(a -> sb.append(a.getId()).append("=").append(a.getNom())
                    .append(" (code ").append(a.getCode()).append(")").append(" | "));
        }
        if (contient(norm, "typeproduction", "type de production", "types de production", "mode de transport", "modetransport")) {
            utile = true;
            sb.append("\nTypes de production : ");
            typeProductionRepository.findAll().forEach(t -> sb.append(t.getId()).append("=").append(t.getNom())
                    .append(" (code ").append(t.getCode())
                    .append(t.getModeTransport() != null ? ", " + t.getModeTransport().getNom() : "").append(") | "));
        }
        if (contient(norm, "typeregime", "regime", "douane", "tarif")) {
            utile = true;
            sb.append("\nTypes de régime : ");
            typeRegimeRepository.findAll().forEach(t -> sb.append(t.getId()).append("=").append(t.getNom())
                    .append(" (").append(t.getCode()).append(") | "));
        }
        if (contient(norm, "typedocument", "types de document", "type de document", "document")) {
            utile = true;
            sb.append("\nTypes de documents : ");
            typeDocumentRepository.findAll().forEach(t -> sb.append(t.getId()).append("=").append(t.getNom())
                    .append(Boolean.TRUE.equals(t.getEstObligatoire()) ? " (obligatoire)" : "")
                    .append(t.getTypeProduction() != null ? " [TP " + t.getTypeProduction().getNom() + "]" : "")
                    .append(" | "));
        }
        if (contient(norm, "naturemarchandise", "nature", "natures")) {
            utile = true;
            sb.append("\nNatures de marchandises : ");
            natureRepository.findAll().forEach(n -> sb.append(n.getId()).append("=").append(n.getNom()).append(" | "));
        }
        if (contient(norm, "pays")) {
            utile = true;
            sb.append("\nPays : ");
            paysRepository.findAll().forEach(p -> sb.append(p.getNom()).append(" (").append(p.getCode()).append(") | "));
        }
        if (contient(norm, "bureau", "bureaux", "dedouanement")) {
            utile = true;
            sb.append("\nBureaux de dédouanement : ");
            bureauRepository.findAll().forEach(b -> sb.append(b.getId()).append("=").append(b.getNom())
                    .append(valeur(b.getVille()) != null ? " (" + b.getVille() + ")" : "").append(" | "));
        }
        if (contient(norm, "incoterm")) {
            utile = true;
            sb.append("\nIncoterms : ");
            incontermeRepository.findAll().forEach(i -> sb.append(i.getAbreviation())
                    .append(" = ").append(i.getNom()).append(" | "));
        }

        if (utile) {
            sources.add(new ContexteRag.Source("base", "Référentiels", null));
            return sb.toString();
        }
        return "";
    }

    // ══════════════════════════════ Utilitaires ══════════════════════════════

    /** Portée agence pour la lecture des dossiers : null si l'utilisateur voit tout. */
    public Integer porteeDossiers(ContexteChat contexte) {
        return contexte.peutVoirToutesAgences() ? null : contexte.idAgence();
    }

    /** Construit une requête plein texte à partir de la question, en retirant les mots inutiles. */
    public static String requeteFts(String questionNormalisee) {
        StringBuilder sb = new StringBuilder();
        for (String mot : questionNormalisee.split("[^a-z0-9]+")) {
            if (mot.length() < 3 || MOTS_VIDES.contains(mot) || TERMES_GENERIQUES.contains(mot)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(mot);
        }
        String requete = sb.toString().trim();
        return requete.isEmpty() ? null : requete;
    }

    /** Extrait un numéro de dossier du type 30-70-01-6-0001 dans la question originale. */
    public static String extraireNumeroDossier(String question) {
        Matcher m = NUMERO_DOSSIER.matcher(question == null ? "" : question);
        return m.find() ? m.group(1) : null;
    }

    private static Integer extraireEntier(String norm) {
        Matcher m = Pattern.compile("\\b(\\d{4,6})\\b").matcher(norm);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /** Retrouve l'identifiant d'un référentiel à partir de son nom mentionné dans la question. */
    private static Integer resoudreIdParNom(String norm, List<Map.Entry<Integer, String>> referentiels) {
        for (Map.Entry<Integer, String> r : referentiels) {
            String nom = BaseConnaissance.normaliser(r.getValue());
            if (nom.length() >= 4 && norm.contains(nom)) {
                return r.getKey();
            }
        }
        return null;
    }

    private List<Map.Entry<Integer, String>> referentielsPhase() {
        return phaseRepository.findAll().stream().map(p -> Map.entry(p.getId(), p.getNom())).toList();
    }

    private List<Map.Entry<Integer, String>> referentielsStatut() {
        return statutRepository.findAll().stream().map(s -> Map.entry(s.getId(), s.getNom())).toList();
    }

    private static String nomClient(Client c) {
        if (c == null) {
            return "inconnu";
        }
        String nom = valeur(c.getNom());
        String prenom = valeur(c.getPrenom());
        if (nom == null && prenom == null) {
            return "client #" + c.getId();
        }
        return (nom == null ? "" : nom) + (prenom == null ? "" : " " + prenom) + " (#" + c.getId() + ")";
    }

    private static String valeur(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDateTime dt) {
            return dt.format(DATE_HEURE);
        }
        if (v instanceof LocalDate d) {
            return d.format(DATE);
        }
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    private String fmtDate(LocalDateTime d) {
        return d == null ? "non définie" : d.format(DATE);
    }

    private static String tronquer(String texte, int max) {
        if (texte == null) {
            return "";
        }
        return texte.length() <= max ? texte : texte.substring(0, max) + "\n[… contexte tronqué …]";
    }
}

package com.auximad.ai;

import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.security.AutorisationHelper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Résolution des références citées par l'utilisateur (« le dossier 30-70-01-6-0001 »,
 * « le client RAHARISON », « l'OT 10234 ») vers les entités réelles, en vérifiant systématiquement
 * que la donnée appartient bien au périmètre de l'utilisateur connecté.
 */
@Component
public class Resolveur {

    private final DossierRepository dossierRepository;
    private final ClientRepository clientRepository;
    private final OrdreTransitRepository ordreTransitRepository;
    private final PhaseDossierRepository phaseRepository;
    private final StatutDossierRepository statutRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final DemandeInteragenceRepository demandeRepository;

    public Resolveur(DossierRepository dossierRepository,
                     ClientRepository clientRepository,
                     OrdreTransitRepository ordreTransitRepository,
                     PhaseDossierRepository phaseRepository,
                     StatutDossierRepository statutRepository,
                     TypeDocumentRepository typeDocumentRepository,
                     TypeProductionRepository typeProductionRepository,
                     DemandeInteragenceRepository demandeRepository) {
        this.dossierRepository = dossierRepository;
        this.clientRepository = clientRepository;
        this.ordreTransitRepository = ordreTransitRepository;
        this.phaseRepository = phaseRepository;
        this.statutRepository = statutRepository;
        this.typeDocumentRepository = typeDocumentRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.demandeRepository = demandeRepository;
    }

    /** Numéro de dossier exact (recherche directe), sans contrôle de périmètre. */
    public Optional<Dossier> dossierParNumero(String numero) {
        if (numero == null || numero.isBlank()) {
            return Optional.empty();
        }
        return dossierRepository.findByNumeroDossier(numero.trim());
    }

    /**
     * Résout un dossier à partir d'un numéro ou d'un identifiant, en vérifiant le périmètre.
     *
     * @throws IllegalArgumentException si aucune correspondance
     * @throws SecurityException       si le dossier est hors du périmètre de l'utilisateur
     */
    public Dossier dossier(String reference, ContexteChat contexte) {
        String ref = reference == null ? null : reference.trim();
        if (ref == null || ref.isEmpty()) {
            throw new IllegalArgumentException("Indiquez le numéro ou l'identifiant du dossier.");
        }

        Optional<Dossier> trouve = dossierParNumero(ref);
        if (trouve.isEmpty()) {
            Integer id = null;
            try {
                id = Integer.valueOf(ref);
            } catch (NumberFormatException ignored) {
                // référence non numérique : on cherche aussi par numéro « approchant »
            }
            if (id != null) {
                trouve = dossierRepository.findById(id);
            }
        }
        if (trouve.isEmpty()) {
            // Numéro sans séparateurs (ex : 307001600001)
            String compact = ref.replaceAll("\\D", "");
            if (compact.length() >= 8) {
                trouve = dossierParNumero(compact);
            }
        }

        if (trouve.isEmpty()) {
            throw new IllegalArgumentException("Aucun dossier ne correspond à « " + ref + " ».");
        }

        Dossier d = trouve.get();
        if (!contexte.peutVoirToutesAgences()
                && d.getAgence() != null
                && contexte.idAgence() != null
                && !contexte.idAgence().equals(d.getAgence().getId())) {
            throw new SecurityException("Le dossier " + d.getNumeroDossier()
                    + " appartient à l'agence " + d.getAgence().getNom()
                    + " et ne vous est pas accessible.");
        }
        return d;
    }

    public Client client(String reference, ContexteChat contexte) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Indiquez le nom, l'identifiant, le NIF ou le STAT du client.");
        }
        String ref = reference.trim();

        List<Client> candidats = clientRepository.suggestions(ref, 5);
        if (candidats.isEmpty() && ref.chars().allMatch(Character::isDigit)) {
            candidats = clientRepository.suggestions(ref, 5);
        }
        if (candidats.isEmpty()) {
            throw new IllegalArgumentException("Aucun client ne correspond à « " + ref + " ».");
        }
        if (candidats.size() > 1) {
            String liste = candidats.stream()
                    .map(c -> "#" + c.getId() + " " + libelle(c))
                    .reduce((a, b) -> a + " ; " + b)
                    .orElse("");
            throw new IllegalArgumentException("« " + ref + " » correspond à plusieurs clients, précise lequel : " + liste);
        }
        return candidats.get(0);
    }

    public OrdreTransit ordreTransit(String reference, ContexteChat contexte) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Indiquez le numéro ou l'identifiant de l'ordre de transit.");
        }
        String ref = reference.trim();
        List<OrdreTransit> candidats = ordreTransitRepository.suggestions(ref, 5);
        if (candidats.isEmpty()) {
            candidats = ordreTransitRepository.suggestions(ref.replaceAll("\\D", ""), 5);
        }
        if (candidats.isEmpty()) {
            throw new IllegalArgumentException("Aucun ordre de transit ne correspond à « " + ref + " ».");
        }
        if (candidats.size() > 1) {
            String liste = candidats.stream()
                    .map(o -> "OT n° " + o.getNumeroOrdreTransit() + " (commande "
                            + (o.getNumeroCommande() == null ? "-" : o.getNumeroCommande()) + ")")
                    .reduce((a, b) -> a + " ; " + b)
                    .orElse("");
            throw new IllegalArgumentException("« " + ref + " » correspond à plusieurs ordres de transit : " + liste);
        }
        return candidats.get(0);
    }

    /** Résout une phase par son nom ; si le nom est absent, renvoie la première du référentiel. */
    public PhaseDossier phase(String nom) {
        if (nom == null || nom.isBlank()) {
            return premier(phaseRepository.findAll(), PhaseDossier::getNom, "phase");
        }
        String cible = BaseConnaissance.normaliser(nom.trim());
        for (PhaseDossier p : phaseRepository.findAll()) {
            if (BaseConnaissance.normaliser(p.getNom()).equals(cible)) {
                return p;
            }
        }
        // Correspondance partielle si le modèle a abrégé (« transit » pour « En transit »)
        for (PhaseDossier p : phaseRepository.findAll()) {
            String n = BaseConnaissance.normaliser(p.getNom());
            if (!n.isEmpty() && cible.length() >= 4 && (cible.contains(n) || n.contains(cible))) {
                return p;
            }
        }
        throw new IllegalArgumentException("Phase inconnue : « " + nom + " ». Phases disponibles : "
                + String.join(", ", phaseRepository.findAll().stream().map(PhaseDossier::getNom).toList()));
    }

    /** Résout un statut par son nom ; par défaut « Ouvert » s'il existe. */
    public StatutDossier statut(String nom) {
        if (nom == null || nom.isBlank()) {
            List<StatutDossier> tous = statutRepository.findAll();
            return tous.stream()
                    .filter(s -> s.getNom() != null && s.getNom().equalsIgnoreCase("Ouvert"))
                    .findFirst()
                    .orElseGet(() -> tous.isEmpty() ? null : tous.get(0));
        }
        String cible = BaseConnaissance.normaliser(nom.trim());
        for (StatutDossier s : statutRepository.findAll()) {
            if (BaseConnaissance.normaliser(s.getNom()).equals(cible)) {
                return s;
            }
        }
        for (StatutDossier s : statutRepository.findAll()) {
            String n = BaseConnaissance.normaliser(s.getNom());
            if (!n.isEmpty() && cible.length() >= 4 && (cible.contains(n) || n.contains(cible))) {
                return s;
            }
        }
        throw new IllegalArgumentException("Statut inconnu : « " + nom + " ». Statuts disponibles : "
                + String.join(", ", statutRepository.findAll().stream().map(StatutDossier::getNom).toList()));
    }

    private static <T> T premier(List<T> liste, java.util.function.Function<T, String> nom, String libelle) {
        if (liste.isEmpty()) {
            throw new IllegalArgumentException("Le référentiel des " + libelle + "s est vide : "
                    + "contactez l'administrateur pour le renseigner.");
        }
        return liste.get(0);
    }

    public TypeDocument typeDocument(String nom) {
        String cible = BaseConnaissance.normaliser(nom == null ? "" : nom);
        for (TypeDocument t : typeDocumentRepository.findAll()) {
            if (BaseConnaissance.normaliser(t.getNom()).equals(cible)) {
                return t;
            }
        }
        for (TypeDocument t : typeDocumentRepository.findAll()) {
            String n = BaseConnaissance.normaliser(t.getNom());
            if (!n.isEmpty() && (cible.contains(n) || (n.contains(cible) && cible.length() >= 4))) {
                return t;
            }
        }
        throw new IllegalArgumentException("Type de document inconnu : « " + nom + " ». Utilise l'outil "
                + "lister_referentiels avec le type \"documents\" pour voir la liste.");
    }

    public TypeProduction typeProduction(String nom) {
        String cible = BaseConnaissance.normaliser(nom == null ? "" : nom);
        for (TypeProduction t : typeProductionRepository.findAll()) {
            if (BaseConnaissance.normaliser(t.getNom()).equals(cible)) {
                return t;
            }
        }
        for (TypeProduction t : typeProductionRepository.findAll()) {
            if (cible.contains(BaseConnaissance.normaliser(t.getNom()))) {
                return t;
            }
        }
        throw new IllegalArgumentException("Type de production inconnu : « " + nom + " ». Types disponibles : "
                + String.join(", ", typeProductionRepository.findAll().stream().map(TypeProduction::getNom).toList()));
    }

    public DemandeInteragence demande(Integer id, ContexteChat contexte) {
        DemandeInteragence d = demandeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande inter-agence #" + id + " introuvable."));
        if (!contexte.peutVoirToutesAgences()
                && contexte.idAgence() != null
                && d.getAgenceDestinataire() != null
                && !contexte.idAgence().equals(d.getAgenceDestinataire().getId())
                && (d.getAgenceDemandeuse() == null || !contexte.idAgence().equals(d.getAgenceDemandeuse().getId()))) {
            throw new SecurityException("Cette demande ne concerne pas votre agence.");
        }
        return d;
    }

    /** Niveaux d'autorisation utilisés par les outils (formule portée + action). */
    public static final int NIVEAU_LECTURE =
            AutorisationHelper.PORTEE_PROPRE_LECTURE + AutorisationHelper.ACTION_OT;
    public static final int NIVEAU_ECRITURE =
            AutorisationHelper.PORTEE_PROPRE_ECRITURE + AutorisationHelper.ACTION_OT;
    public static final int NIVEAU_ECRITURE_INTERAGENCE =
            AutorisationHelper.PORTEE_PROPRE_ECRITURE + AutorisationHelper.ACTION_SOUS_DOSSIER;
    public static final int NIVEAU_SUPPRESSION =
            AutorisationHelper.PORTEE_PROPRE_SUPPRESSION + AutorisationHelper.ACTION_OT;
    public static final int NIVEAU_VALIDATION =
            AutorisationHelper.PORTEE_PROPRE_VALIDATION + AutorisationHelper.ACTION_OT;

    public static String libelle(Client c) {
        if (c == null) {
            return "client inconnu";
        }
        StringBuilder sb = new StringBuilder(c.getNom() == null ? "" : c.getNom());
        if (c.getPrenom() != null && !c.getPrenom().isBlank()) {
            sb.append(' ').append(c.getPrenom());
        }
        return sb.toString().isBlank() ? "client #" + c.getId() : sb.toString();
    }

    /** Fiche compacte d'un dossier, réutilisée par plusieurs outils. */
    public static Map<String, Object> ficheDossier(Dossier d) {
        Map<String, Object> fiche = new LinkedHashMap<>();
        fiche.put("id", d.getId());
        fiche.put("numero", d.getNumeroDossier());
        if (d.getClient() != null) {
            fiche.put("client", libelle(d.getClient()));
        }
        if (d.getAgence() != null) {
            fiche.put("agence", d.getAgence().getNom());
        }
        if (d.getPhaseDossier() != null) {
            fiche.put("phase", d.getPhaseDossier().getNom());
        }
        if (d.getStatutDossier() != null) {
            fiche.put("statut", d.getStatutDossier().getNom());
        }
        if (d.getTypeProduction() != null) {
            fiche.put("typeProduction", d.getTypeProduction().getNom());
        }
        if (d.getOrdreTransit() != null) {
            fiche.put("ordreTransit", d.getOrdreTransit().getNumeroOrdreTransit());
        }
        return fiche;
    }
}
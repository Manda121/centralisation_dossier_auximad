package com.auximad.service;

import com.auximad.dto.PageResponse;
import com.auximad.dto.DossierFiltres;
import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auximad.security.JwtSecurityContext;

import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DossierService {

    private final SuppressionService suppressionService;

    /**
     * Colonnes de tri autorisées pour la recherche native des dossiers.
     * Les clés sont les propriétés envoyées par le front, les valeurs sont les colonnes SQL
     * réellement présentes dans la requête native (Spring Data ajoute le préfixe d'alias « d. »).
     */
    private static final Map<String, String> COLONNES_TRI = Map.ofEntries(
            Map.entry("numeroDossier", "numero_dossier"),
            Map.entry("numero_dossier", "numero_dossier"),
            Map.entry("dateCreation", "date_creation"),
            Map.entry("date_creation", "date_creation"),
            Map.entry("estGroupage", "est_groupage"),
            Map.entry("est_groupage", "est_groupage"),
            // Colonnes des tables jointes : le préfixe d'alias est obligatoire en SQL natif
            Map.entry("client.nom", "c.nom"),
            Map.entry("phaseDossier.nom", "ph.nom"),
            Map.entry("statutDossier.nom", "sd.nom")
    );

    private static final String COLONNE_TRI_DEFAUT = "date_creation";

    private final OrdreTransitService ordreTransitService;
    private final DossierRepository dossierRepository;
    private final PhaseDossierRepository phaseDossierRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final IncontermeRepository incontermeRepository;
    private final AgenceRepository agenceRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final DossierInteragenceRepository interagenceRepository;
    private final DocumentRepository documentRepository;
    private final RemiseRepository remiseRepository;
    private final DemandeInteragenceRepository demandeInteragenceRepository;

    public DossierService(DossierRepository dossierRepository,
                          PhaseDossierRepository phaseDossierRepository,
                          StatutDossierRepository statutDossierRepository,
                          IncontermeRepository incontermeRepository,
                          AgenceRepository agenceRepository,
                          TypeProductionRepository typeProductionRepository,
                          DossierInteragenceRepository interagenceRepository,
                          DocumentRepository documentRepository,
                          RemiseRepository remiseRepository,
                          DemandeInteragenceRepository demandeInteragenceRepository, OrdreTransitService ordreTransitService, SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
        this.ordreTransitService = ordreTransitService;
        this.dossierRepository = dossierRepository;
        this.phaseDossierRepository = phaseDossierRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.incontermeRepository = incontermeRepository;
        this.agenceRepository = agenceRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.interagenceRepository = interagenceRepository;
        this.documentRepository = documentRepository;
        this.remiseRepository = remiseRepository;
        this.demandeInteragenceRepository = demandeInteragenceRepository;
    }

    /**
     * Remplit les champs transitoires d'interagence (estDossierMere, estSousDossier, etc.)
     */
    public void enrichirInteragence(Dossier d) {
        if (d == null) return;
        // 1. Est-ce un sous-dossier ?
        Optional<DossierInteragence> sousDossierLien = interagenceRepository.findFirstBySousDossierIdOrderByIdAsc(d.getId());
        if (sousDossierLien.isPresent()) {
            d.setEstSousDossier(true);
            d.setMotifInteragence(sousDossierLien.get().getMotif());
            if (sousDossierLien.get().getDossierMere() != null) {
                d.setNumeroDossierMere(sousDossierLien.get().getDossierMere().getNumeroDossier());
            }
        }
        // 2. Est-ce un dossier mère ?
        List<DossierInteragence> sousDossiers = interagenceRepository.findByDossierMereId(d.getId());
        if (!sousDossiers.isEmpty()) {
            d.setEstDossierMere(true);
        }
    }

    /**
     * Recherche paginée et filtrée avec FTS
     */
    @Transactional(readOnly = true)
    public PageResponse<Dossier> searchDossiers(int page, int size, String sortBy, String sortDir,
                                                 String query, Integer phaseId, Integer statutId,
                                                 Integer clientId, Integer typeProductionId,
                                                 Integer otId, Boolean estGroupage,
                                                 String dateMin, String dateMax,
                                                 Integer agenceId, String typeDossier) {
        return searchDossiers(page, size, sortBy, sortDir, query, phaseId, statutId,
                clientId, typeProductionId, otId, estGroupage, dateMin, dateMax,
                agenceId, typeDossier, new DossierFiltres());
    }

    @Transactional(readOnly = true)
    public PageResponse<Dossier> searchDossiers(int page, int size, String sortBy, String sortDir,
                                                 String query, Integer phaseId, Integer statutId,
                                                 Integer clientId, Integer typeProductionId,
                                                 Integer otId, Boolean estGroupage,
                                                 String dateMin, String dateMax,
                                                 Integer agenceId, String typeDossier, DossierFiltres filtres) {
        // Requête native : le tri doit porter sur des colonnes SQL, pas sur des propriétés JPA.
        // Toute clé inconnue retombe sur le tri par défaut (aucune erreur 500 côté SQL).
        String colonneTri = COLONNES_TRI.getOrDefault(sortBy == null ? "" : sortBy, COLONNE_TRI_DEFAUT);
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? Sort.by(colonneTri).ascending()
                : Sort.by(colonneTri).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Dossier> result = dossierRepository.searchDossiersFiltres(
                (query == null || query.isBlank()) ? null : query,
                phaseId, statutId, clientId, typeProductionId, otId,
                estGroupage, dateMin, dateMax, agenceId, typeDossier,
                normaliserFiltre(filtres.getNumeroDossier()),
                normaliserFiltre(filtres.getClientNom()),
                normaliserFiltre(filtres.getCpClient()),
                filtres.getNumeroOt(),
                filtres.getPaysOrigineId(),
                filtres.getPaysDestinationId(),
                filtres.getIncontermeId(),
                normaliserFiltre(filtres.getPourCompte()),
                normaliserFiltre(filtres.getDepart()),
                normaliserFiltre(filtres.getAdresseLivraison()),
                normaliserFiltre(filtres.getLieuPaiementFret()),
                filtres.getEstPrepaye(),
                filtres.getEstProvisionne(),
                filtres.getProvisionMin(),
                filtres.getProvisionMax(),
                normaliserFiltre(filtres.getEcheanceMin()),
                normaliserFiltre(filtres.getEcheanceMax()),
                normaliserFiltre(filtres.getDateLivraisonMin()),
                normaliserFiltre(filtres.getDateLivraisonMax()), pageable
        );

        // Enrichir chaque dossier récupéré avec ses données inter-agences
        result.getContent().forEach(this::enrichirInteragence);

        return PageResponse.of(result);
    }

    private String normaliserFiltre(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    @Transactional(readOnly = true)
    public Optional<Dossier> getById(Integer id) {
        Optional<Dossier> dossierOpt = dossierRepository.findById(id);
        dossierOpt.ifPresent(this::enrichirInteragence);
        return dossierOpt;
    }

    /**
     * Génère automatiquement un numéro de dossier selon la nomenclature :
     * CodeTypeProduction-CodeAgence-MMA-NNNN
     * (TypeProd - Agence - Mois 2 chiffres + dernier chiffre année - Chronologie 4 chiffres)
     */
    private String genererNumeroDossier(Dossier d) {
        LocalDateTime now = LocalDateTime.now();
        String dernierChiffreAnnee = String.valueOf(now.getYear() % 10);
        String mois = String.format("%02d", now.getMonthValue());

        Agence agence = d.getAgence();
        if (agence != null && agence.getId() != null && agence.getCode() == null) {
            agence = agenceRepository.findById(agence.getId()).orElse(agence);
            d.setAgence(agence);
        }

        TypeProduction typeProduction = d.getTypeProduction();
        if (typeProduction != null && typeProduction.getId() != null && typeProduction.getCode() == null) {
            typeProduction = typeProductionRepository.findById(typeProduction.getId()).orElse(typeProduction);
            d.setTypeProduction(typeProduction);
        }

        if (agence == null || agence.getCode() == null) {
            throw new IllegalArgumentException("Impossible de générer le numéro de dossier : code agence manquant.");
        }
        if (typeProduction == null || typeProduction.getCode() == null) {
            throw new IllegalArgumentException("Impossible de générer le numéro de dossier : code type production manquant.");
        }

        String codeAgence = String.format("%02d", agence.getCode());
        String codeProd = String.valueOf(typeProduction.getCode());
        int chronologie = dossierRepository.findMaxChronologie() + 1;
        if (chronologie > 9999) {
            throw new IllegalArgumentException("La chronologie des dossiers a atteint la limite de 9999.");
        }
        String seq = String.format("%04d", chronologie);

        return codeProd + "" + codeAgence + "" + mois + dernierChiffreAnnee + "" + seq;
    }

    private void verifierOtDisponible(Dossier dossier) {
        if (dossier.getOrdreTransit() == null || dossier.getOrdreTransit().getId() == null) {
            return;
        }
        Integer otId = dossier.getOrdreTransit().getId();
        Integer agenceId = dossier.getAgence() != null ? dossier.getAgence().getId() : null;

        if (agenceId == null) {
            agenceId = JwtSecurityContext.getIdAgence();
        }

        Optional<Dossier> existant = (agenceId != null)
                ? dossierRepository.findByOrdreTransitIdAndAgenceId(otId, agenceId)
                : dossierRepository.findByOrdreTransitId(otId);

        if (existant.isPresent() && (dossier.getId() == null || !existant.get().getId().equals(dossier.getId()))) {
            throw new IllegalArgumentException(
                    "Un dossier existe déjà pour cet ordre de transit au sein de cette agence (" +
                            (existant.get().getNumeroDossier() != null ? existant.get().getNumeroDossier() : existant.get().getId()) +
                            ")."
            );
        }
    }

    @Transactional
    public Dossier saveDossier(Dossier dossier) {
        if (dossier.getId() == null) {
            // Le verrou est conservé jusqu'au commit de la transaction, pas seulement au calcul.
            dossierRepository.verrouillerNumerotation();
        }
        verifierOtDisponible(dossier);
        if (dossier.getOrdreTransit() != null) {
            Integer otId = dossier.getOrdreTransit().getId();
            if (otId == null) {
                throw new IllegalArgumentException("L'ordre de transit doit exister avant de créer le dossier.");
            }
            OrdreTransit ot = ordreTransitService.getOrdreTransitById(otId)
                    .orElseThrow(() -> new IllegalArgumentException("Ordre de transit introuvable : " + otId));
            if (ot.getStatutOt() == null || !Integer.valueOf(3).equals(ot.getStatutOt().getId())) {
                throw new IllegalArgumentException("Veuillez confirmer la validation de l'OT avant de créer le dossier.");
            }
            dossier.setOrdreTransit(ot);
        } else if (dossier.getId() == null) {
            if (dossier.getUtilisateur() == null || dossier.getAgence() == null) {
                throw new IllegalArgumentException("L'utilisateur et l'agence sont requis pour créer l'OT lié.");
            }
            OrdreTransit ot = new OrdreTransit();
            ot.setClient(dossier.getClient());
            ot.setTypeProduction(dossier.getTypeProduction());
            Utilisateur auteur = new Utilisateur();
            auteur.setId(dossier.getUtilisateur().getId());
            auteur.setAgence(dossier.getAgence());
            OrdreTransit cree = ordreTransitService.createOrdreTransit(ot, auteur);
            dossier.setOrdreTransit(ordreTransitService.validateOrdreTransit(cree.getId()));
        }
        if (dossier.getId() == null) {
            dossier.setNumeroDossier(genererNumeroDossier(dossier));
            dossier.setDateCreation(LocalDateTime.now());
        }
        return dossierRepository.save(dossier);
    }

    @Transactional
    public void deleteDossier(Integer id) {
        suppressionService.supprimer("dossier", id);
    }

    // Référentiels
    @Transactional(readOnly = true)
    public List<PhaseDossier> getAllPhases() {
        return phaseDossierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PhaseDossier> getPhasesByModeTransport(Integer modeId) {
        return phaseDossierRepository.findByModeTransportId(modeId);
    }

    @Transactional(readOnly = true)
    public List<StatutDossier> getAllStatuts() {
        return statutDossierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inconterme> getAllIncontermes() {
        return incontermeRepository.findAll();
    }

    /** Dossiers ayant des documents obligatoires non remis */
    @Transactional(readOnly = true)
    public List<Dossier> getDossiersSansDocumentsComplets(Integer agenceId) {
        return dossierRepository.findDossiersSansDocumentsComplets(agenceId);
    }

}

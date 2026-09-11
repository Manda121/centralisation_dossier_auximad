package com.auximad.service;

import com.auximad.dto.PageResponse;
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
import java.util.Optional;

@Service
public class DossierService {

    private final DossierRepository dossierRepository;
    private final PhaseDossierRepository phaseDossierRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final IncontermeRepository incontermeRepository;
    private final AgenceRepository agenceRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final DossierInteragenceRepository interagenceRepository;

    public DossierService(DossierRepository dossierRepository,
                          PhaseDossierRepository phaseDossierRepository,
                          StatutDossierRepository statutDossierRepository,
                          IncontermeRepository incontermeRepository,
                          AgenceRepository agenceRepository,
                          TypeProductionRepository typeProductionRepository,
                          DossierInteragenceRepository interagenceRepository) {
        this.dossierRepository = dossierRepository;
        this.phaseDossierRepository = phaseDossierRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.incontermeRepository = incontermeRepository;
        this.agenceRepository = agenceRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.interagenceRepository = interagenceRepository;
    }

    /**
     * Remplit les champs transitoires d'interagence (estDossierMere, estSousDossier, etc.)
     */
    public void enrichirInteragence(Dossier d) {
        if (d == null) return;
        // 1. Est-ce un sous-dossier ?
        Optional<DossierInteragence> sousDossierLien = interagenceRepository.findBySousDossierId(d.getId());
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
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy != null ? sortBy : "date_creation").ascending()
                : Sort.by(sortBy != null ? sortBy : "date_creation").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Dossier> result = dossierRepository.searchDossiers(
                (query == null || query.isBlank()) ? null : query,
                phaseId, statutId, clientId, typeProductionId, otId,
                estGroupage, dateMin, dateMax, agenceId, typeDossier, pageable
        );

        // Enrichir chaque dossier récupéré avec ses données inter-agences
        result.getContent().forEach(this::enrichirInteragence);

        return PageResponse.of(result);
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
        verifierOtDisponible(dossier);
        if (dossier.getId() == null) {
            dossier.setNumeroDossier(genererNumeroDossier(dossier));
            dossier.setDateCreation(LocalDateTime.now());
        }
        return dossierRepository.save(dossier);
    }

    @Transactional
    public void deleteDossier(Integer id) {
        dossierRepository.deleteById(id);
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

    /** Dossiers avec date limite de remise dans les N prochains jours */
    @Transactional(readOnly = true)
    public List<Dossier> getDossiersRemiseUrgente(int joursAvant, Integer agenceId) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.plusDays(joursAvant);
        return dossierRepository.findDossiersAvecRemiseUrgente(maintenant, limite, agenceId);
    }
}

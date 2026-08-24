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

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

@Service
public class DossierService {

    private final DossierRepository dossierRepository;
    private final PhaseDossierRepository phaseDossierRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final IncontermeRepository incontermeRepository;

    public DossierService(DossierRepository dossierRepository,
                          PhaseDossierRepository phaseDossierRepository,
                          StatutDossierRepository statutDossierRepository,
                          IncontermeRepository incontermeRepository) {
        this.dossierRepository = dossierRepository;
        this.phaseDossierRepository = phaseDossierRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.incontermeRepository = incontermeRepository;
    }

    /**
     * Recherche paginée et filtrée avec FTS
     */
    @Transactional(readOnly = true)
    public PageResponse<Dossier> searchDossiers(int page, int size, String sortBy, String sortDir,
                                                 String query, Integer phaseId, Integer statutId,
                                                 Integer clientId, Integer typeProductionId,
                                                 Integer otId, Boolean estGroupage,
                                                 String dateMin, String dateMax) {
        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy != null ? sortBy : "date_creation").ascending()
                : Sort.by(sortBy != null ? sortBy : "date_creation").descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Dossier> result = dossierRepository.searchDossiers(
                (query == null || query.isBlank()) ? null : query,
                phaseId, statutId, clientId, typeProductionId, otId,
                estGroupage, dateMin, dateMax, pageable
        );

        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public Optional<Dossier> getById(Integer id) {
        return dossierRepository.findById(id);
    }

        /**
     * Génère automatiquement un numéro de dossier selon la nomenclature :
     * CodeTypeProduction-CodeAgence-MM-A-NNNN (TypeProd-Agence-Mois-DernierChiffreAnnée-Séquence)
     */
    private String genererNumeroDossier(Dossier d) {
        LocalDateTime now = LocalDateTime.now();
        String annee = String.valueOf(now.getYear());
        String dernierChiffreAnnee = annee.substring(annee.length() - 1);
        String mois = String.format("%02d", now.getMonthValue());
        String codeAgence = d.getAgence() != null ? String.format("%02d", d.getAgence().getCode()) : "00";
        String codeProd = d.getTypeProduction() != null ? String.valueOf(d.getTypeProduction().getCode()) : "0";
        long count = dossierRepository.count() + 1;
        String seq = String.format("%04d", count);
        return codeProd + "-" + codeAgence + "-" + mois + dernierChiffreAnnee + "-" + seq;
    }

    @Transactional
    public Dossier saveDossier(Dossier dossier) {
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
}

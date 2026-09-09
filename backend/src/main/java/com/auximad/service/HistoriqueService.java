package com.auximad.service;

import com.auximad.dto.PageResponse;
import com.auximad.entity.Historique;
import com.auximad.repository.HistoriqueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HistoriqueService {

    /** Champs de tri autorisés (protection contre l'injection dans Sort). */
    private static final java.util.Set<String> CHAMPS_TRI = java.util.Set.of(
            "id", "dateAction", "url", "hostname", "logiciel", "adresseIp",
            "methodeHttp", "statutHttp", "entite");

    private final HistoriqueRepository historiqueRepository;

    public HistoriqueService(HistoriqueRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Historique> searchHistoriques(int page, int size, String sortBy, String sortDir,
                                                      String query, Integer utilisateurId, Integer actionId,
                                                      String adresseIp, String hostname, String logiciel,
                                                      String methodeHttp, Integer statutHttp,
                                                      LocalDateTime dateMin, LocalDateTime dateMax) {
        String champTri = (sortBy != null && CHAMPS_TRI.contains(sortBy)) ? sortBy : "dateAction";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(champTri).ascending()
                : Sort.by(champTri).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Historique> result = historiqueRepository.searchHistoriques(
                (query == null || query.isBlank()) ? null : query,
                utilisateurId, actionId,
                (adresseIp == null || adresseIp.isBlank()) ? null : adresseIp,
                (hostname == null || hostname.isBlank()) ? null : hostname,
                (logiciel == null || logiciel.isBlank()) ? null : logiciel,
                (methodeHttp == null || methodeHttp.isBlank()) ? null : methodeHttp,
                statutHttp, dateMin, dateMax,
                pageable
        );

        return PageResponse.of(result);
    }
}

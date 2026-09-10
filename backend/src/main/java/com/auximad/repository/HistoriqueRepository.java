package com.auximad.repository;

import com.auximad.entity.Historique;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoriqueRepository extends JpaRepository<Historique, Integer> {

    /**
     * Recherche paginée multi-critères sur l'historique.
     * Tous les filtres sont optionnels (NULL = ignoré).
     */
    @Query(value = """
        SELECT h FROM Historique h
        LEFT JOIN h.utilisateur u
        LEFT JOIN h.action a
        WHERE (:query IS NULL OR :query = ''
               OR LOWER(COALESCE(h.url, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(h.details, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(h.hostname, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(h.logiciel, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(h.entite, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.nomUtilisateur, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.nom, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.prenom, '')) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (CAST(:utilisateurId AS integer) IS NULL OR u.id = :utilisateurId)
          AND (CAST(:actionId AS integer) IS NULL OR a.id = :actionId)
          AND (:adresseIp IS NULL OR :adresseIp = '' OR h.adresseIp = :adresseIp)
          AND (:hostname IS NULL OR :hostname = '' OR LOWER(h.hostname) LIKE LOWER(CONCAT('%', :hostname, '%')))
          AND (:logiciel IS NULL OR :logiciel = '' OR LOWER(h.logiciel) LIKE LOWER(CONCAT('%', :logiciel, '%')))
          AND (:methodeHttp IS NULL OR :methodeHttp = '' OR h.methodeHttp = :methodeHttp)
          AND (CAST(:statutHttp AS integer) IS NULL OR h.statutHttp = :statutHttp)
          AND (CAST(:dateMin AS timestamp) IS NULL OR h.dateAction >= :dateMin)
          AND (CAST(:dateMax AS timestamp) IS NULL OR h.dateAction <= :dateMax)
        """)
    Page<Historique> searchHistoriques(
            @Param("query") String query,
            @Param("utilisateurId") Integer utilisateurId,
            @Param("actionId") Integer actionId,
            @Param("adresseIp") String adresseIp,
            @Param("hostname") String hostname,
            @Param("logiciel") String logiciel,
            @Param("methodeHttp") String methodeHttp,
            @Param("statutHttp") Integer statutHttp,
            @Param("dateMin") LocalDateTime dateMin,
            @Param("dateMax") LocalDateTime dateMax,
            Pageable pageable
    );

    /** Distinct hostnames déjà enregistrés (pour les filtres du frontend). */
    @Query("SELECT DISTINCT h.hostname FROM Historique h WHERE h.hostname IS NOT NULL ORDER BY h.hostname")
    List<String> findDistinctHostnames();

    /** Distinct logiciels déjà enregistrés (pour les filtres du frontend). */
    @Query("SELECT DISTINCT h.logiciel FROM Historique h WHERE h.logiciel IS NOT NULL ORDER BY h.logiciel")
    List<String> findDistinctLogiciels();

    /** Distinct adresses IP déjà enregistrées (pour les filtres du frontend). */
    @Query("SELECT DISTINCT h.adresseIp FROM Historique h WHERE h.adresseIp IS NOT NULL ORDER BY h.adresseIp")
    List<String> findDistinctAdresseIps();
}

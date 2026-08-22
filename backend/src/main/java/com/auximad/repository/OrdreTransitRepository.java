package com.auximad.repository;

import com.auximad.entity.OrdreTransit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OrdreTransitRepository extends JpaRepository<OrdreTransit, Integer> {

    @Query(value = "SELECT ot.* FROM ordre_transit ot " +
            "INNER JOIN clients c ON ot.id_client = c.id " +
            "LEFT JOIN clients exp ON ot.id_expediteur = exp.id " +
            "LEFT JOIN clients dest ON ot.id_destinataire = dest.id " +
            "LEFT JOIN type_production tp ON ot.id_type_production = tp.id " +
            "WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "to_tsvector('french', coalesce(CAST(ot.numero_ordre_transit AS text), '') || ' ' || coalesce(ot.numero_commande, '') || ' ' || coalesce(c.nom, '') || ' ' || coalesce(exp.nom, '') || ' ' || coalesce(dest.nom, '')) @@ plainto_tsquery('french', :query)) " +
            "AND (:numero IS NULL OR ot.numero_ordre_transit = :numero) " +
            "AND (cast(:dateMin as timestamp) IS NULL OR ot.date_creation >= :dateMin) " +
            "AND (cast(:dateMax as timestamp) IS NULL OR ot.date_creation <= :dateMax) " +
            "AND (:expediteurId IS NULL OR ot.id_expediteur = :expediteurId) " +
            "AND (:destinataireId IS NULL OR ot.id_destinataire = :destinataireId) " +
            "AND (:typeRegimeId IS NULL OR ot.id_type_regime = :typeRegimeId) " +
            "AND (:estAssure IS NULL OR ot.est_assure = :estAssure) " +
            "AND (:typeProductionId IS NULL OR ot.id_type_production = :typeProductionId) " +
            "AND (:modeTransportId IS NULL OR tp.id_mode_transport = :modeTransportId) " +
            "AND (:clientId IS NULL OR ot.id_client = :clientId) " +
            "AND (ot.id_statut_ot IS NULL OR ot.id_statut_ot != 1)", // Exclure statut 1 (Brouillon) dans la liste principale
            countQuery = "SELECT count(*) FROM ordre_transit ot " +
            "INNER JOIN clients c ON ot.id_client = c.id " +
            "LEFT JOIN clients exp ON ot.id_expediteur = exp.id " +
            "LEFT JOIN clients dest ON ot.id_destinataire = dest.id " +
            "LEFT JOIN type_production tp ON ot.id_type_production = tp.id " +
            "WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "to_tsvector('french', coalesce(CAST(ot.numero_ordre_transit AS text), '') || ' ' || coalesce(ot.numero_commande, '') || ' ' || coalesce(c.nom, '') || ' ' || coalesce(exp.nom, '') || ' ' || coalesce(dest.nom, '')) @@ plainto_tsquery('french', :query)) " +
            "AND (:numero IS NULL OR ot.numero_ordre_transit = :numero) " +
            "AND (cast(:dateMin as timestamp) IS NULL OR ot.date_creation >= :dateMin) " +
            "AND (cast(:dateMax as timestamp) IS NULL OR ot.date_creation <= :dateMax) " +
            "AND (:expediteurId IS NULL OR ot.id_expediteur = :expediteurId) " +
            "AND (:destinataireId IS NULL OR ot.id_destinataire = :destinataireId) " +
            "AND (:typeRegimeId IS NULL OR ot.id_type_regime = :typeRegimeId) " +
            "AND (:estAssure IS NULL OR ot.est_assure = :estAssure) " +
            "AND (:typeProductionId IS NULL OR ot.id_type_production = :typeProductionId) " +
            "AND (:modeTransportId IS NULL OR tp.id_mode_transport = :modeTransportId) " +
            "AND (:clientId IS NULL OR ot.id_client = :clientId) " +
            "AND (ot.id_statut_ot IS NULL OR ot.id_statut_ot != 1)",
            nativeQuery = true)
    Page<OrdreTransit> searchOrdreTransits(
            @Param("query") String query,
            @Param("numero") Integer numero,
            @Param("dateMin") LocalDateTime dateMin,
            @Param("dateMax") LocalDateTime dateMax,
            @Param("expediteurId") Integer expediteurId,
            @Param("destinataireId") Integer destinataireId,
            @Param("typeRegimeId") Integer typeRegimeId,
            @Param("estAssure") Boolean estAssure,
            @Param("typeProductionId") Integer typeProductionId,
            @Param("modeTransportId") Integer modeTransportId,
            @Param("clientId") Integer clientId,
            Pageable pageable
    );

    @Query("SELECT coalesce(max(ot.numeroOrdreTransit), 10000) FROM OrdreTransit ot")
    Integer findMaxNumeroOrdreTransit();
}

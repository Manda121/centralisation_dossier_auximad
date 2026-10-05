package com.auximad.repository;

import com.auximad.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    /** Tous les documents actifs d'un dossier */
    List<Document> findByDossierIdAndEstActifTrue(Integer dossierId);

    /** Tous les documents d'un dossier (actifs et inactifs) */
    List<Document> findByDossierId(Integer dossierId);

    /** Supprimer tous les documents associés à un dossier */
    void deleteByDossierId(Integer dossierId);

    /** Vérifie si un type de document est déjà remis (actif) pour un dossier */
    boolean existsByDossierIdAndTypeDocumentIdAndEstActifTrue(Integer dossierId, Integer typeDocumentId);

    /** Récupère le document actif d'un dossier pour un type donné */
    java.util.Optional<Document> findByDossierIdAndTypeDocumentIdAndEstActifTrue(Integer dossierId, Integer typeDocumentId);
}

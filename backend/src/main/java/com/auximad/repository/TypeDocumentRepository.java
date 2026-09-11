package com.auximad.repository;

import com.auximad.entity.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeDocumentRepository extends JpaRepository<TypeDocument, Integer> {

    /** Tous les types de document d'un type de production */
    List<TypeDocument> findByTypeProductionId(Integer typeProductionId);

    /** Types obligatoires d'un type de production */
    List<TypeDocument> findByTypeProductionIdAndEstObligatoireTrue(Integer typeProductionId);

    /** Types de document sans type de production spécifique (génériques) */
    List<TypeDocument> findByTypeProductionIsNull();
}

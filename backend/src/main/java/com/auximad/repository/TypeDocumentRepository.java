package com.auximad.repository;

import com.auximad.entity.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
        SELECT td.* FROM type_document td
        WHERE td.id_type_production = :typeProductionId
          AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = :natureMarchandiseId)
        ORDER BY td.nom
    """, nativeQuery = true)
    List<TypeDocument> findApplicables(@Param("typeProductionId") Integer typeProductionId,
                                       @Param("natureMarchandiseId") Integer natureMarchandiseId);
}

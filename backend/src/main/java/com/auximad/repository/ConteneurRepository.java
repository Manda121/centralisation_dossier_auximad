package com.auximad.repository;

import com.auximad.entity.Conteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConteneurRepository extends JpaRepository<Conteneur, Integer> {

    @Query("SELECT c FROM Conteneur c JOIN c.dossiers d WHERE d.id = :dossierId")
    List<Conteneur> findByDossierId(@Param("dossierId") Integer dossierId);

    Optional<Conteneur> findByNumeroContainer(String numeroContainer);

    boolean existsByNumeroContainer(String numeroContainer);
}

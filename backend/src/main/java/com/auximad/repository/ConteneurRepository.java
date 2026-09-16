package com.auximad.repository;

import com.auximad.entity.Conteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConteneurRepository extends JpaRepository<Conteneur, Integer> {

    List<Conteneur> findByDossierId(Integer dossierId);

    Optional<Conteneur> findByNumeroContainer(String numeroContainer);

    boolean existsByNumeroContainer(String numeroContainer);

    boolean existsByNumeroContainerAndIdNot(String numeroContainer, Integer id);
}

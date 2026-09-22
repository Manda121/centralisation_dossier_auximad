package com.auximad.repository;

import com.auximad.entity.OperationDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationDossierRepository extends JpaRepository<OperationDossier, Integer> {

    List<OperationDossier> findByTypeProductionIdOrderByNiveauOperationAscIdAsc(Integer typeProductionId);

    List<OperationDossier> findByTypeProductionIdAndPhaseDossierIdOrderByNiveauOperationAscIdAsc(Integer typeProductionId, Integer phaseId);

    List<OperationDossier> findAllByOrderByTypeProductionIdAscNiveauOperationAscIdAsc();
}

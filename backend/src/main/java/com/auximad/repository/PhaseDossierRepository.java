package com.auximad.repository;

import com.auximad.entity.PhaseDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseDossierRepository extends JpaRepository<PhaseDossier, Integer> {
    List<PhaseDossier> findByModeTransportId(Integer modeTransportId);
}

package com.auximad.repository;

import com.auximad.entity.StatutDossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatutDossierRepository extends JpaRepository<StatutDossier, Integer> {
}

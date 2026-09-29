package com.auximad.repository;

import com.auximad.entity.DocumentMvt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMvtRepository extends JpaRepository<DocumentMvt, Integer> {
    List<DocumentMvt> findByMvtDossierId(Integer mvtDossierId);
    Optional<DocumentMvt> findFirstByMvtDossierIdOrderByIdDesc(Integer mvtDossierId);
}

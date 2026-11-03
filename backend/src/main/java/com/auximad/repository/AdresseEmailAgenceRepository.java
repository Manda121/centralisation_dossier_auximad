package com.auximad.repository;

import com.auximad.entity.AdresseEmailAgence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdresseEmailAgenceRepository extends JpaRepository<AdresseEmailAgence, Integer> {
    List<AdresseEmailAgence> findAllByOrderByAgenceNomAscEmailAsc();
    List<AdresseEmailAgence> findByAgenceIdAndActifTrueOrderByEmailAsc(Integer agenceId);
    boolean existsByAgenceIdAndEmailAndIdNot(Integer agenceId, String email, Integer id);
}

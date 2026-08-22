package com.auximad.repository;

import com.auximad.entity.Autorisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutorisationRepository extends JpaRepository<Autorisation, Integer> {

    Optional<Autorisation> findByUtilisateurId(Integer utilisateurId);
}

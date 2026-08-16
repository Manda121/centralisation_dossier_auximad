package com.auximad.repository;

import com.auximad.entity.SuspensionConnexion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuspensionConnexionRepository extends JpaRepository<SuspensionConnexion, Integer> {

    Optional<SuspensionConnexion> findByUtilisateurId(Integer idUtilisateur);
}

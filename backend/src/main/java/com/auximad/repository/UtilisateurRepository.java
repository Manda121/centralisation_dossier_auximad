package com.auximad.repository;

import com.auximad.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    @Query("SELECT u FROM Utilisateur u WHERE u.nomUtilisateur = :nomUtilisateur AND u.agence.code = :codeAgence")
    Optional<Utilisateur> findByNomUtilisateurAndAgenceCode(
            @Param("nomUtilisateur") String nomUtilisateur,
            @Param("codeAgence") Integer codeAgence
    );
}

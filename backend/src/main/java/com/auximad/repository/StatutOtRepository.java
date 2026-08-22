package com.auximad.repository;

import com.auximad.entity.StatutOt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutOtRepository extends JpaRepository<StatutOt, Integer> {

    Optional<StatutOt> findByNom(String nom);
}

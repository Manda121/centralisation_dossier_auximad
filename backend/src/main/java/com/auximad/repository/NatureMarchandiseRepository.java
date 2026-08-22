package com.auximad.repository;

import com.auximad.entity.NatureMarchandise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NatureMarchandiseRepository extends JpaRepository<NatureMarchandise, Integer> {
    Optional<NatureMarchandise> findByNom(String nom);
}

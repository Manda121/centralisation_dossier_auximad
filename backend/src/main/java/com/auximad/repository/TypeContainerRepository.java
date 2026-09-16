package com.auximad.repository;

import com.auximad.entity.TypeContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeContainerRepository extends JpaRepository<TypeContainer, Integer> {
    Optional<TypeContainer> findByNom(String nom);
}

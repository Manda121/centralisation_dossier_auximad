package com.auximad.repository;

import com.auximad.entity.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeclarationRepository extends JpaRepository<Declaration, Integer> {
    Optional<Declaration> findByDossierId(Integer dossierId);
    Optional<Declaration> findByNumeroDeclaration(String numeroDeclaration);
}

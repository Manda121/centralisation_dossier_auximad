package com.auximad.repository;

import com.auximad.entity.TypeDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeDeclarationRepository extends JpaRepository<TypeDeclaration, Integer> {
}

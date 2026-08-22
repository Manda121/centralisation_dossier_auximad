package com.auximad.repository;

import com.auximad.entity.TypeRegime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeRegimeRepository extends JpaRepository<TypeRegime, Integer> {
}

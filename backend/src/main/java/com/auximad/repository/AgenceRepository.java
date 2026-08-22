package com.auximad.repository;

import com.auximad.entity.Agence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgenceRepository extends JpaRepository<Agence, Integer> {
    Optional<Agence> findByCode(Integer code);
}

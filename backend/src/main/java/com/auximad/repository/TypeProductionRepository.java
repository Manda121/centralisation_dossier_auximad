package com.auximad.repository;

import com.auximad.entity.TypeProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeProductionRepository extends JpaRepository<TypeProduction, Integer> {

    List<TypeProduction> findByModeTransportId(Integer modeTransportId);
}

package com.auximad.repository;

import com.auximad.entity.Marchandise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarchandiseRepository extends JpaRepository<Marchandise, Integer> {
    List<Marchandise> findByOrdreTransitId(Integer ordreTransitId);
}

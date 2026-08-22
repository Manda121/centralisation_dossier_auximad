package com.auximad.repository;

import com.auximad.entity.Compagnie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompagnieRepository extends JpaRepository<Compagnie, Integer> {

    List<Compagnie> findByModeTransportId(Integer modeTransportId);
}

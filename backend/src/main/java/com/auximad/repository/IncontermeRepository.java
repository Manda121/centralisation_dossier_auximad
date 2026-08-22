package com.auximad.repository;

import com.auximad.entity.Inconterme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncontermeRepository extends JpaRepository<Inconterme, Integer> {
}

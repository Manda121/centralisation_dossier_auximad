package com.auximad.repository;

import com.auximad.entity.Navire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NavireRepository extends JpaRepository<Navire, Integer> {

    List<Navire> findByCompagnieId(Integer compagnieId);
}

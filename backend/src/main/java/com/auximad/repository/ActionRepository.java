package com.auximad.repository;

import com.auximad.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Integer> {

    Optional<Action> findByNom(String nom);
}

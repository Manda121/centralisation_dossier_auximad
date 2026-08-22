package com.auximad.repository;

import com.auximad.entity.Instruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructionRepository extends JpaRepository<Instruction, Integer> {

    List<Instruction> findByOrdreTransitId(Integer ordreTransitId);
}

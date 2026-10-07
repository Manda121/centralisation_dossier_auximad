package com.auximad.repository;

import com.auximad.entity.MvtAnnulee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MvtAnnuleeRepository extends JpaRepository<MvtAnnulee, Integer> {
}

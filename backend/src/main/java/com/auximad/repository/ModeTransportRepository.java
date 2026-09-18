package com.auximad.repository;

import com.auximad.entity.ModeTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModeTransportRepository extends JpaRepository<ModeTransport, Integer> {
}

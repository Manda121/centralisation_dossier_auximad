package com.auximad.service;

import com.auximad.entity.Agence;
import com.auximad.repository.AgenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AgenceService {

    private final AgenceRepository agenceRepository;

    public AgenceService(AgenceRepository agenceRepository) {
        this.agenceRepository = agenceRepository;
    }

    @Transactional(readOnly = true)
    public List<Agence> getAllAgences() {
        return agenceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Agence> getAgenceById(Integer id) {
        return agenceRepository.findById(id);
    }
}

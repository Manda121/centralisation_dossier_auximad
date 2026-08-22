package com.auximad.service;

import com.auximad.entity.Marchandise;
import com.auximad.entity.NatureMarchandise;
import com.auximad.entity.UniteMesure;
import com.auximad.repository.MarchandiseRepository;
import com.auximad.repository.NatureMarchandiseRepository;
import com.auximad.repository.UniteMesureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MarchandiseService {

    private final MarchandiseRepository marchandiseRepository;
    private final UniteMesureRepository uniteMesureRepository;
    private final NatureMarchandiseRepository natureMarchandiseRepository;

    public MarchandiseService(MarchandiseRepository marchandiseRepository,
                              UniteMesureRepository uniteMesureRepository,
                              NatureMarchandiseRepository natureMarchandiseRepository) {
        this.marchandiseRepository = marchandiseRepository;
        this.uniteMesureRepository = uniteMesureRepository;
        this.natureMarchandiseRepository = natureMarchandiseRepository;
    }

    @Transactional(readOnly = true)
    public List<Marchandise> getMarchandisesByOt(Integer otId) {
        return marchandiseRepository.findByOrdreTransitId(otId);
    }

    @Transactional(readOnly = true)
    public Optional<Marchandise> getById(Integer id) {
        return marchandiseRepository.findById(id);
    }

    @Transactional
    public Marchandise saveMarchandise(Marchandise m) {
        return marchandiseRepository.save(m);
    }

    @Transactional
    public void deleteMarchandise(Integer id) {
        marchandiseRepository.deleteById(id);
    }

    // Référentiels
    @Transactional(readOnly = true)
    public List<UniteMesure> getAllUniteMesures() {
        return uniteMesureRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<NatureMarchandise> getAllNatureMarchandises() {
        return natureMarchandiseRepository.findAll();
    }
}

package com.auximad.service;

import com.auximad.dto.PageResponse;
import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrdreTransitService {

    private final OrdreTransitRepository ordreTransitRepository;
    private final ContratTransportRepository contratTransportRepository;
    private final InstructionRepository instructionRepository;
    private final StatutOtRepository statutOtRepository;
    private final CompagnieRepository compagnieRepository;
    private final NavireRepository navireRepository;
    private final TypeProductionRepository typeProductionRepository;
    private final TypeRegimeRepository typeRegimeRepository;
    private final BureauRepository bureauRepository;

    public OrdreTransitService(OrdreTransitRepository ordreTransitRepository,
                               ContratTransportRepository contratTransportRepository,
                               InstructionRepository instructionRepository,
                               StatutOtRepository statutOtRepository,
                               CompagnieRepository compagnieRepository,
                               NavireRepository navireRepository,
                               TypeProductionRepository typeProductionRepository,
                               TypeRegimeRepository typeRegimeRepository,
                               BureauRepository bureauRepository) {
        this.ordreTransitRepository = ordreTransitRepository;
        this.contratTransportRepository = contratTransportRepository;
        this.instructionRepository = instructionRepository;
        this.statutOtRepository = statutOtRepository;
        this.compagnieRepository = compagnieRepository;
        this.navireRepository = navireRepository;
        this.typeProductionRepository = typeProductionRepository;
        this.typeRegimeRepository = typeRegimeRepository;
        this.bureauRepository = bureauRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrdreTransit> searchOrdreTransits(
            String query, Integer numero, LocalDateTime dateMin, LocalDateTime dateMax,
            Integer expediteurId, Integer destinataireId, Integer typeRegimeId,
            Boolean estAssure, Integer typeProductionId, Integer modeTransportId,
            Integer clientId, int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(Sort.Direction.DESC, "date_creation");
        if (sortBy != null && !sortBy.trim().isEmpty() && !sortBy.equals("dateCreation")) {
            sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy)
                    .and(Sort.by(Sort.Direction.DESC, "date_creation"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrdreTransit> otPage = ordreTransitRepository.searchOrdreTransits(
                query, numero, dateMin, dateMax, expediteurId, destinataireId, typeRegimeId,
                estAssure, typeProductionId, modeTransportId, clientId, pageable
        );
        return PageResponse.of(otPage);
    }

    @Transactional(readOnly = true)
    public Optional<OrdreTransit> getOrdreTransitById(Integer id) {
        return ordreTransitRepository.findById(id);
    }

    @Transactional
    public OrdreTransit createOrdreTransit(OrdreTransit ot, Utilisateur connectedUser) {
        // Associer l'utilisateur connecté
        ot.setUtilisateur(connectedUser);
        ot.setAgence(connectedUser.getAgence());

        // Générer le numéro d'ordre de transit séquentiellement
        Integer maxNum = ordreTransitRepository.findMaxNumeroOrdreTransit();
        ot.setNumeroOrdreTransit(maxNum != null ? maxNum + 1 : 10001);

        // Statut par défaut : Non Validé (id 2) ou rechercher statut 'Non valide'
        StatutOt statutNonValide = statutOtRepository.findById(2)
                .orElseGet(() -> statutOtRepository.findByNom("Non valide")
                .orElseGet(() -> statutOtRepository.save(new StatutOt(2, "Non valide", "Ordre de transit en attente de validation"))));
        ot.setStatutOt(statutNonValide);

        return ordreTransitRepository.save(ot);
    }

    @Transactional
    public OrdreTransit validateOrdreTransit(Integer id) {
        OrdreTransit ot = ordreTransitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordre de transit introuvable pour l'id: " + id));

        StatutOt statutValide = statutOtRepository.findById(3)
                .orElseGet(() -> statutOtRepository.findByNom("Valide")
                .orElseGet(() -> statutOtRepository.save(new StatutOt(3, "Valide", "Ordre de transit validé"))));
        ot.setStatutOt(statutValide);

        return ordreTransitRepository.save(ot);
    }

    @Transactional
    public void deleteOrdreTransit(Integer id) {
        OrdreTransit ot = ordreTransitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordre de transit introuvable pour l'id: " + id));

        // Au lieu de supprimer physiquement, si on a un statut Brouillon, on peut le supprimer ou le soft-supprimer. Let's delete instructions et contrat associés puis supprimer
        contratTransportRepository.findByOrdreTransitId(id).ifPresent(contratTransportRepository::delete);
        List<Instruction> instructions = instructionRepository.findByOrdreTransitId(id);
        instructionRepository.deleteAll(instructions);

        ordreTransitRepository.delete(ot);
    }

    // ========== Contrat de Transport ==========

    @Transactional(readOnly = true)
    public Optional<ContratTransport> getContratByTransitOrderId(Integer transitOrderId) {
        return contratTransportRepository.findByOrdreTransitId(transitOrderId);
    }

    @Transactional
    public ContratTransport saveContrat(Integer transitOrderId, ContratTransport contrat) {
        OrdreTransit ot = ordreTransitRepository.findById(transitOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordre de transit introuvable pour l'id: " + transitOrderId));

        Optional<ContratTransport> existing = contratTransportRepository.findByOrdreTransitId(transitOrderId);
        if (existing.isPresent()) {
            ContratTransport current = existing.get();
            current.setNumero(contrat.getNumero());
            current.setCompagnie(contrat.getCompagnie());
            current.setNavire(contrat.getNavire());
            current.setNumeroVol(contrat.getNumeroVol());
            current.setLieuEmission(contrat.getLieuEmission());
            current.setNotify(contrat.getNotify());
            current.setDatePrevue(contrat.getDatePrevue());
            current.setDateArrivee(contrat.getDateArrivee());
            return contratTransportRepository.save(current);
        } else {
            contrat.setOrdreTransit(ot);
            return contratTransportRepository.save(contrat);
        }
    }

    // ========== Instructions ==========

    @Transactional(readOnly = true)
    public List<Instruction> getInstructionsByTransitOrderId(Integer transitOrderId) {
        return instructionRepository.findByOrdreTransitId(transitOrderId);
    }

    @Transactional
    public Instruction addInstruction(Integer transitOrderId, Instruction inst) {
        OrdreTransit ot = ordreTransitRepository.findById(transitOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordre de transit introuvable pour l'id: " + transitOrderId));

        inst.setOrdreTransit(ot);
        return instructionRepository.save(inst);
    }

    // ========== Référentiels ==========

    @Transactional(readOnly = true)
    public List<Compagnie> getCompagniesByMode(Integer modeId) {
        return compagnieRepository.findByModeTransportId(modeId);
    }

    @Transactional(readOnly = true)
    public List<Navire> getNaviresByCompagnie(Integer compagnieId) {
        return navireRepository.findByCompagnieId(compagnieId);
    }

    @Transactional(readOnly = true)
    public List<TypeProduction> getTypeProductionsByMode(Integer modeId) {
        return typeProductionRepository.findByModeTransportId(modeId);
    }

    @Transactional(readOnly = true)
    public List<TypeRegime> getAllTypeRegimes() {
        return typeRegimeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Bureau> getAllBureaux() {
        return bureauRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TypeProduction> getAllTypeProductions() {
        return typeProductionRepository.findAll();
    }
}

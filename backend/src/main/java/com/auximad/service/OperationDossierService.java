package com.auximad.service;

import com.auximad.entity.OperationDossier;
import com.auximad.entity.PhaseDossier;
import com.auximad.entity.StatutDossier;
import com.auximad.entity.TypeProduction;
import com.auximad.repository.OperationDossierRepository;
import com.auximad.repository.PhaseDossierRepository;
import com.auximad.repository.StatutDossierRepository;
import com.auximad.repository.TypeProductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class OperationDossierService {

    private final SuppressionService suppressionService;

    private final OperationDossierRepository operationDossierRepository;
    private final PhaseDossierRepository phaseDossierRepository;
    private final StatutDossierRepository statutDossierRepository;
    private final TypeProductionRepository typeProductionRepository;

    public OperationDossierService(OperationDossierRepository operationDossierRepository,
                                  PhaseDossierRepository phaseDossierRepository,
                                  StatutDossierRepository statutDossierRepository,
                                  TypeProductionRepository typeProductionRepository, SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
        this.operationDossierRepository = operationDossierRepository;
        this.phaseDossierRepository = phaseDossierRepository;
        this.statutDossierRepository = statutDossierRepository;
        this.typeProductionRepository = typeProductionRepository;
    }

    @Transactional(readOnly = true)
    public List<OperationDossier> getAllOperations() {
        return operationDossierRepository.findAllByOrderByTypeProductionIdAscNiveauOperationAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<OperationDossier> getOperationsByTypeProduction(Integer typeProductionId) {
        return operationDossierRepository.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(typeProductionId);
    }

    @Transactional
    public OperationDossier saveOperation(OperationDossier op) {
        return operationDossierRepository.save(op);
    }

    @Transactional
    public OperationDossier updateOperationFromMap(Integer id, Map<String, Object> body) {
        OperationDossier op = operationDossierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Opération introuvable : " + id));

        if (body.containsKey("description")) op.setDescription((String) body.get("description"));
        if (body.containsKey("niveauAutorisation")) op.setNiveauAutorisation(parseId(body.get("niveauAutorisation")));
        if (body.containsKey("niveauValidation")) op.setNiveauValidation(parseId(body.get("niveauValidation")));
        if (body.containsKey("niveauOperation")) op.setNiveauOperation(parseId(body.get("niveauOperation")));
        if (body.containsKey("estActif")) op.setEstActif((Boolean) body.get("estActif"));
        if (body.containsKey("estObligatoire")) op.setEstObligatoire((Boolean) body.get("estObligatoire"));

        if (body.containsKey("idPhase")) {
            Integer idPhase = parseId(body.get("idPhase"));
            op.setPhaseDossier(idPhase != null ? phaseDossierRepository.findById(idPhase).orElse(null) : null);
        }

        if (body.containsKey("idStatut")) {
            Integer idStatut = parseId(body.get("idStatut"));
            op.setStatutDossier(idStatut != null ? statutDossierRepository.findById(idStatut).orElse(null) : null);
        }

        if (body.containsKey("idTypeProduction")) {
            Integer idTp = parseId(body.get("idTypeProduction"));
            op.setTypeProduction(idTp != null ? typeProductionRepository.findById(idTp).orElse(null) : null);
        }

        return operationDossierRepository.save(op);
    }

    @Transactional
    public void deleteOperation(Integer id) {
        suppressionService.supprimer("operation_dossier", id);
    }

    @Transactional
    public void deleteBatch(List<Integer> ids) {
        suppressionService.supprimerLot("operation_dossier", ids);
    }

    @Transactional
    public void reorderOperations(List<Integer> operationIds) {
        if (operationIds == null) return;
        for (int i = 0; i < operationIds.size(); i++) {
            Integer opId = operationIds.get(i);
            OperationDossier op = operationDossierRepository.findById(opId).orElse(null);
            if (op != null) {
                op.setNiveauOperation(i + 1);
                operationDossierRepository.save(op);
            }
        }
    }

    private Integer parseId(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }
}

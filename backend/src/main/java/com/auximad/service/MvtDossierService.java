package com.auximad.service;

import com.auximad.dto.MvtDossierRequest;
import com.auximad.dto.OperationDossierStatusDTO;
import com.auximad.dto.PageResponse;
import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.security.JwtSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MvtDossierService {

    private final MvtDossierRepository mvtDossierRepository;
    private final DossierRepository dossierRepository;
    private final OperationDossierRepository operationDossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DeclarationRepository declarationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    public MvtDossierService(MvtDossierRepository mvtDossierRepository,
                             DossierRepository dossierRepository,
                             OperationDossierRepository operationDossierRepository,
                             UtilisateurRepository utilisateurRepository,
                             DeclarationRepository declarationRepository,
                             DocumentRepository documentRepository,
                             DocumentService documentService) {
        this.mvtDossierRepository = mvtDossierRepository;
        this.dossierRepository = dossierRepository;
        this.operationDossierRepository = operationDossierRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.declarationRepository = declarationRepository;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public PageResponse<MvtDossier> searchMouvements(
            int page, int size, String sortBy, String sortDir, String query,
            Integer dossierId, String numeroDossier, String cpClient,
            Integer agenceId, Integer typeProductionId, Integer phaseId,
            Integer statutId, Integer operationId, Integer utilisateurId,
            String dateMinStr, String dateMaxStr
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "dateMvt");
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, sortBy);
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        LocalDateTime dateMin = parseDateMin(dateMinStr);
        LocalDateTime dateMax = parseDateMax(dateMaxStr);

        Page<MvtDossier> resultPage = mvtDossierRepository.searchMouvements(
                query, dossierId, numeroDossier, cpClient, agenceId,
                typeProductionId, phaseId, statutId, operationId, utilisateurId,
                dateMin, dateMax, pageable
        );

        // Charger document associé pour chaque mouvement si présent
        for (MvtDossier mvt : resultPage.getContent()) {
            if (mvt.getDossier() != null) {
                List<Document> docs = documentRepository.findByDossierIdAndEstActifTrue(mvt.getDossier().getId());
                if (!docs.isEmpty()) {
                    mvt.setDocument(docs.get(docs.size() - 1));
                }
            }
        }

        return PageResponse.of(resultPage);
    }

    @Transactional(readOnly = true)
    public List<OperationDossierStatusDTO> getOperationsStatusForDossier(Integer dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + dossierId));

        if (dossier.getTypeProduction() == null) {
            return Collections.emptyList();
        }

        Integer tpId = dossier.getTypeProduction().getId();
        List<OperationDossier> operations = operationDossierRepository
                .findByTypeProductionIdOrderByNiveauOperationAscIdAsc(tpId);

        List<MvtDossier> mouvements = mvtDossierRepository.findByDossierIdOrderByDateMvtAscIdAsc(dossierId);
        Map<Integer, MvtDossier> mvtByOpId = new HashMap<>();
        for (MvtDossier mvt : mouvements) {
            if (mvt.getOperation() != null) {
                mvtByOpId.put(mvt.getOperation().getId(), mvt);
            }
        }

        Optional<Declaration> declarationOpt = declarationRepository.findByDossierId(dossierId);
        List<Document> activeDocs = documentRepository.findByDossierIdAndEstActifTrue(dossierId);

        List<OperationDossierStatusDTO> statusList = new ArrayList<>();
        boolean unprevMandatoryIncomplete = false;
        OperationDossier lastIncompleteMandatoryOp = null;

        for (OperationDossier op : operations) {
            OperationDossierStatusDTO dto = new OperationDossierStatusDTO();
            dto.setOperation(op);

            MvtDossier mvt = mvtByOpId.get(op.getId());
            if (mvt != null) {
                dto.setEstFait(true);
                dto.setMvtId(mvt.getId());
                dto.setDateMvt(mvt.getDateMvt());
            } else {
                dto.setEstFait(false);
            }

            // Déterminer le blocage si l'opération précédente obligatoire n'est pas faite
            if (unprevMandatoryIncomplete && !dto.getEstFait()) {
                dto.setEstBloque(true);
                dto.setOperationPrecedenteObligatoire(lastIncompleteMandatoryOp);
            } else {
                dto.setEstBloque(false);
            }

            // Si cette opération est obligatoire et n'est pas encore faite, elle bloquera les suivantes
            if (Boolean.TRUE.equals(op.getEstObligatoire()) && !dto.getEstFait()) {
                unprevMandatoryIncomplete = true;
                if (lastIncompleteMandatoryOp == null) {
                    lastIncompleteMandatoryOp = op;
                }
            }

            // Attacher déclaration si l'opération est une déclaration
            if (isDeclarationOperation(op) && declarationOpt.isPresent()) {
                dto.setDeclaration(declarationOpt.get());
            }

            // Attacher document si présent pour ce dossier
            if (!activeDocs.isEmpty()) {
                dto.setDocument(activeDocs.get(activeDocs.size() - 1));
            }

            statusList.add(dto);
        }

        return statusList;
    }

    @Transactional
    public MvtDossier creerMouvement(MvtDossierRequest req, MultipartFile file, String clientIp) throws IOException {
        Dossier dossier = dossierRepository.findById(req.getDossierId())
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + req.getDossierId()));

        OperationDossier operation = operationDossierRepository.findById(req.getOperationId())
                .orElseThrow(() -> new IllegalArgumentException("Opération introuvable : " + req.getOperationId()));

        // Vérification de la contrainte : opération obligatoire précédente
        List<OperationDossierStatusDTO> statusList = getOperationsStatusForDossier(dossier.getId());
        for (OperationDossierStatusDTO status : statusList) {
            if (status.getOperation().getId().equals(operation.getId())) {
                if (Boolean.TRUE.equals(status.getEstBloque())) {
                    String opObligNom = status.getOperationPrecedenteObligatoire() != null
                            ? status.getOperationPrecedenteObligatoire().getDescription() : "précédente";
                    throw new IllegalArgumentException(
                            "Impossible d'effectuer ce mouvement : l'opération obligatoire '" + opObligNom + "' n'est pas encore complétée."
                    );
                }
                break;
            }
        }

        Utilisateur u = null;
        Integer userId = JwtSecurityContext.getIdUtilisateur();
        if (userId != null) {
            u = utilisateurRepository.findById(userId).orElse(null);
        }
        if (u == null) {
            u = dossier.getUtilisateur();
        }

        MvtDossier mvt = new MvtDossier();
        mvt.setDossier(dossier);
        mvt.setOperation(operation);
        mvt.setAnciennePhase(dossier.getPhaseDossier());
        mvt.setAncienStatut(dossier.getStatutDossier());
        mvt.setNouvellePhase(operation.getPhaseDossier() != null ? operation.getPhaseDossier() : dossier.getPhaseDossier());
        mvt.setNouveauStatut(operation.getStatutDossier() != null ? operation.getStatutDossier() : dossier.getStatutDossier());
        mvt.setDateMvt(req.getDateMvt() != null ? req.getDateMvt() : LocalDateTime.now());
        mvt.setCommentaire(req.getCommentaire());
        mvt.setUtilisateur(u);
        mvt.setIpUtilisateur(clientIp != null ? clientIp : "127.0.0.1");

        // Traiter upload fichier si fourni
        if (file != null && !file.isEmpty()) {
            List<TypeDocument> typeDocs = documentService.getTypeDocumentsByTypeProduction(
                    dossier.getTypeProduction() != null ? dossier.getTypeProduction().getId() : null);
            Integer typeDocId = typeDocs.isEmpty() ? null : typeDocs.get(0).getId();
            if (typeDocId != null) {
                Document doc = documentService.saveDocument(dossier.getId(), typeDocId, file);
                mvt.setDocument(doc);
            }
        }

        MvtDossier savedMvt = mvtDossierRepository.save(mvt);

        // Mise à jour de la phase et du statut du dossier
        if (operation.getPhaseDossier() != null) {
            dossier.setPhaseDossier(operation.getPhaseDossier());
        }
        if (operation.getStatutDossier() != null) {
            dossier.setStatutDossier(operation.getStatutDossier());
        }
        dossierRepository.save(dossier);

        return savedMvt;
    }

    private boolean isDeclarationOperation(OperationDossier op) {
        if (op == null || op.getDescription() == null) return false;
        return op.getDescription().toLowerCase().contains("declaration")
                || op.getDescription().toLowerCase().contains("déclaration");
    }

    private LocalDateTime parseDateMin(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().atStartOfDay(); // Aujourd'hui 00:00:00
        }
        try {
            if (dateStr.length() == 10) {
                return LocalDate.parse(dateStr).atStartOfDay();
            }
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDate.now().atStartOfDay();
        }
    }

    private LocalDateTime parseDateMax(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().atTime(LocalTime.MAX); // Aujourd'hui 23:59:59
        }
        try {
            if (dateStr.length() == 10) {
                return LocalDate.parse(dateStr).atTime(LocalTime.MAX);
            }
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
    }
}

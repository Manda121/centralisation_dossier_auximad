package com.auximad.service;

import com.auximad.dto.DeclarationRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeclarationService {

    private final DeclarationRepository declarationRepository;
    private final DossierRepository dossierRepository;
    private final BureauRepository bureauRepository;
    private final TypeDeclarationRepository typeDeclarationRepository;
    private final DocumentService documentService;

    public DeclarationService(DeclarationRepository declarationRepository,
                              DossierRepository dossierRepository,
                              BureauRepository bureauRepository,
                              TypeDeclarationRepository typeDeclarationRepository,
                              DocumentService documentService) {
        this.declarationRepository = declarationRepository;
        this.dossierRepository = dossierRepository;
        this.bureauRepository = bureauRepository;
        this.typeDeclarationRepository = typeDeclarationRepository;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public List<Declaration> getAllDeclarations() {
        return declarationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Declaration> getByDossierId(Integer dossierId) {
        return declarationRepository.findByDossierId(dossierId);
    }

    @Transactional(readOnly = true)
    public List<TypeDeclaration> getAllTypeDeclarations() {
        return typeDeclarationRepository.findAll();
    }

    @Transactional
    public Declaration saveDeclaration(DeclarationRequest req, MultipartFile file) throws IOException {
        Dossier dossier = dossierRepository.findById(req.getDossierId())
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + req.getDossierId()));

        Optional<Declaration> opt = declarationRepository.findByDossierId(dossier.getId());
        Declaration declaration = opt.orElseGet(Declaration::new);

        declaration.setDossier(dossier);
        declaration.setNumeroDeclaration(req.getNumeroDeclaration());
        declaration.setDateDeclaration(req.getDateDeclaration() != null ? req.getDateDeclaration() : LocalDateTime.now());
        declaration.setDescription(req.getDescription());

        if (req.getBureauId() != null) {
            Bureau b = bureauRepository.findById(req.getBureauId()).orElse(null);
            declaration.setBureau(b);
        }

        if (req.getTypeDeclarationId() != null) {
            TypeDeclaration td = typeDeclarationRepository.findById(req.getTypeDeclarationId()).orElse(null);
            declaration.setTypeDeclaration(td);
        }

        // Si un fichier est fourni, créer ou associer le document
        if (file != null && !file.isEmpty()) {
            List<TypeDocument> typeDocs = documentService.getTypeDocumentsByTypeProduction(
                    dossier.getTypeProduction() != null ? dossier.getTypeProduction().getId() : null);
            Integer typeDocId = typeDocs.isEmpty() ? null : typeDocs.get(0).getId();
            if (typeDocId != null) {
                Document doc = documentService.saveDocument(dossier.getId(), typeDocId, file);
                declaration.setDocument(doc);
            }
        }

        return declarationRepository.save(declaration);
    }
}

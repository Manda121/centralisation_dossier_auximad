package com.auximad.service;

import com.auximad.dto.MvtDossierRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MouvementDocumentsRulesTest {
    @Mock MvtDossierRepository mvtDossierRepository;
    @Mock DossierRepository dossierRepository;
    @Mock OperationDossierRepository operationDossierRepository;
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock DeclarationRepository declarationRepository;
    @Mock DocumentRepository documentRepository;
    @Mock DocumentMvtRepository documentMvtRepository;
    @Mock DocumentService documentService;
    @InjectMocks MvtDossierService service;

    @Test void missingDocumentsPreventAllMovementWrites() {
        Dossier d = new Dossier(); d.setId(1);
        MvtDossierRequest req = new MvtDossierRequest(); req.setDossierId(1); req.setOperationId(2);
        when(dossierRepository.findById(1)).thenReturn(Optional.of(d));
        when(documentService.tousDocsObligatoiresRemis(1)).thenReturn(false);
        when(documentService.getDocsObligatoiresManquants(1)).thenReturn(List.of());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.creerMouvement(req, null, null));
        assertTrue(error.getMessage().contains("documents obligatoires"));
        verifyNoInteractions(mvtDossierRepository, operationDossierRepository, documentMvtRepository);
        verify(dossierRepository, never()).save(any());
    }

    @Test void completeDocumentsAllowMovement() throws Exception {
        Dossier d = new Dossier(); d.setId(1);
        OperationDossier op = new OperationDossier(); op.setId(2);
        MvtDossierRequest req = new MvtDossierRequest(); req.setDossierId(1); req.setOperationId(2);
        when(dossierRepository.findById(1)).thenReturn(Optional.of(d));
        when(documentService.tousDocsObligatoiresRemis(1)).thenReturn(true);
        when(operationDossierRepository.findById(2)).thenReturn(Optional.of(op));
        when(mvtDossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertSame(d, service.creerMouvement(req, null, null).getDossier());
        verify(mvtDossierRepository).save(any());
        verify(dossierRepository).save(d);
    }
}
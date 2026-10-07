package com.auximad.service;

import com.auximad.dto.MvtDossierRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void missingDocumentsPreventAllMovementWrites() {
        Dossier d = new Dossier(); d.setId(1);
        OperationDossier op = new OperationDossier(); op.setId(2); op.setDescription("Declaration");
        MvtDossierRequest req = new MvtDossierRequest(); req.setDossierId(1); req.setOperationId(2);
        when(dossierRepository.findById(1)).thenReturn(Optional.of(d));
        when(operationDossierRepository.findById(2)).thenReturn(Optional.of(op));
        when(documentService.operationDocsObligatoiresRemis(1, 2)).thenReturn(false);
        when(documentService.getDocsManquantsPourOperation(1, 2)).thenReturn(List.of());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.creerMouvement(req, null));
        assertTrue(error.getMessage().contains("documents obligatoires"));
        verifyNoInteractions(mvtDossierRepository, documentMvtRepository);
        verify(dossierRepository, never()).save(any());
    }

    @Test void completeDocumentsAllowMovement() throws Exception {
        Dossier d = new Dossier(); d.setId(1);
        OperationDossier op = new OperationDossier(); op.setId(2);
        Utilisateur utilisateur = new Utilisateur(); utilisateur.setId(42);
        var authentication = new UsernamePasswordAuthenticationToken("user", null);
        authentication.setDetails(Jwts.claims().add("userId", 42).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MvtDossierRequest req = new MvtDossierRequest(); req.setDossierId(1); req.setOperationId(2);
        when(dossierRepository.findById(1)).thenReturn(Optional.of(d));
        when(documentService.operationDocsObligatoiresRemis(1, 2)).thenReturn(true);
        when(operationDossierRepository.findById(2)).thenReturn(Optional.of(op));
        when(utilisateurRepository.findById(42)).thenReturn(Optional.of(utilisateur));
        when(mvtDossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MvtDossier mouvement = service.creerMouvement(req, null);
        assertSame(d, mouvement.getDossier());
        assertSame(utilisateur, mouvement.getUtilisateur());
        verify(mvtDossierRepository).save(any());
        verify(dossierRepository).save(d);
    }
}

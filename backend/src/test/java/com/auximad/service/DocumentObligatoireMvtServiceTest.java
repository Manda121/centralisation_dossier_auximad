package com.auximad.service;

import com.auximad.entity.Dossier;
import com.auximad.entity.TypeDocument;
import com.auximad.entity.TypeProduction;
import com.auximad.repository.DossierRepository;
import com.auximad.repository.DocumentObligatoireMvtRepository;
import com.auximad.repository.DocumentRepository;
import com.auximad.repository.RemiseRepository;
import com.auximad.repository.TypeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Règle métier : le mouvement d'une opération n'est possible que si les
 * documents obligatoires de CETTE opération (table document_obligatoire_mvt)
 * sont remis. Une opération sans document requis reste réalisable.
 */
@ExtendWith(MockitoExtension.class)
class DocumentObligatoireMvtServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock TypeDocumentRepository typeDocumentRepository;
    @Mock DossierRepository dossierRepository;
    @Mock RemiseRepository remiseRepository;
    @Mock DocumentObligatoireMvtRepository documentObligatoireMvtRepository;
    @Mock SuppressionService suppressionService;

    @InjectMocks DocumentService service;

    @Test
    void mouvementBloqueSiDocumentObligatoireDeLOperationManquant() {
        TypeDocument manquant = new TypeDocument();
        manquant.setId(10);
        manquant.setNom("Connaissement maritime");
        when(documentObligatoireMvtRepository.countManquants(1, 20)).thenReturn(1L);
        when(documentObligatoireMvtRepository.findManquants(1, 20)).thenReturn(List.of(manquant));

        assertFalse(service.operationDocsObligatoiresRemis(1, 20));
        assertEquals(List.of(manquant), service.getDocsManquantsPourOperation(1, 20));
        verify(documentObligatoireMvtRepository).findManquants(1, 20);
    }

    @Test
    void operationSansDocumentRequisResteRealisable() {
        when(documentObligatoireMvtRepository.countManquants(1, 21)).thenReturn(0L);

        assertTrue(service.operationDocsObligatoiresRemis(1, 21));
        verify(documentObligatoireMvtRepository).countManquants(1, 21);
    }

    @Test
    void verifGlobaleDossierCompteToutesLesOperationsDuTypeProduction() {
        Dossier d = new Dossier();
        d.setId(2);
        TypeProduction tp = new TypeProduction();
        tp.setId(5);
        d.setTypeProduction(tp);
        when(dossierRepository.findById(2)).thenReturn(Optional.of(d));
        when(documentObligatoireMvtRepository.countManquantsDossier(2, 5)).thenReturn(1L);

        assertFalse(service.tousDocsObligatoiresRemis(2));
        verify(documentObligatoireMvtRepository).countManquantsDossier(2, 5);
    }

    @Test
    void dossierInconnuRefuse() {
        when(dossierRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.tousDocsObligatoiresRemis(99));
    }
}

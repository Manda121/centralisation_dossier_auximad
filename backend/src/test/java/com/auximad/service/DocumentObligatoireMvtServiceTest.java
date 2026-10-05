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
 * Règle métier : un mouvement de dossier n'est possible que si tous les
 * documents obligatoires (table document_obligatoire_mvt) sont remis.
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

    private Dossier dossier(int id, Integer typeProductionId) {
        Dossier d = new Dossier();
        d.setId(id);
        if (typeProductionId != null) {
            TypeProduction tp = new TypeProduction();
            tp.setId(typeProductionId);
            d.setTypeProduction(tp);
        }
        return d;
    }

    @Test
    void mouvementBloqueSiDocumentObligatoireManquant() {
        when(dossierRepository.findById(1)).thenReturn(Optional.of(dossier(1, 5)));
        TypeDocument manquant = new TypeDocument();
        manquant.setId(10);
        manquant.setNom("Facture commerciale");
        when(documentObligatoireMvtRepository.findManquants(1, 5)).thenReturn(List.of(manquant));

        assertFalse(service.tousDocsObligatoiresRemis(1));
        assertEquals(List.of(manquant), service.getDocsObligatoiresManquants(1));
        verify(documentObligatoireMvtRepository).findManquants(1, 5);
    }

    @Test
    void mouvementAutoriseSiAucunDocumentObligatoireManquant() {
        when(dossierRepository.findById(2)).thenReturn(Optional.of(dossier(2, 5)));
        when(documentObligatoireMvtRepository.findManquants(2, 5)).thenReturn(List.of());

        assertTrue(service.tousDocsObligatoiresRemis(2));
    }

    @Test
    void dossierSansTypeProductionUtiliseSeulementLesDocumentsGlobaux() {
        when(dossierRepository.findById(3)).thenReturn(Optional.of(dossier(3, null)));
        when(documentObligatoireMvtRepository.findManquants(3, null)).thenReturn(List.of());

        assertTrue(service.tousDocsObligatoiresRemis(3));
        verify(documentObligatoireMvtRepository).findManquants(3, null);
    }

    @Test
    void dossierInconnuRefuse() {
        when(dossierRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.tousDocsObligatoiresRemis(99));
    }
}

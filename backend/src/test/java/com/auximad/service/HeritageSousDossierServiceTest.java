package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeritageSousDossierServiceTest {
    private final ConteneurRepository conteneurs = mock(ConteneurRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final RemiseRepository remises = mock(RemiseRepository.class);
    private final ConteneurService eligibilite = mock(ConteneurService.class);
    private final HeritageSousDossierService service = new HeritageSousDossierService(
            conteneurs, documents, remises, eligibilite);

    private Dossier dossier(int id) {
        Dossier dossier = new Dossier(); dossier.setId(id); return dossier;
    }

    @Test void sharesTheSameContainersWithoutRemovingParentOrOtherAgencies() {
        Dossier mere = dossier(1), enfant = dossier(2), autre = dossier(3);
        Conteneur conteneur = new Conteneur(); conteneur.setId(10); conteneur.setNumeroContainer("CONT-001");
        conteneur.getDossiers().add(mere); conteneur.getDossiers().add(autre);
        when(conteneurs.findByDossierId(1)).thenReturn(List.of(conteneur));
        when(eligibilite.estEligibleConteneur(enfant)).thenReturn(true);

        service.copierRessources(mere, enfant);

        assertEquals(3, conteneur.getDossiers().size());
        assertTrue(conteneur.getDossiers().containsAll(List.of(mere, enfant, autre)));
        verify(conteneurs).saveAll(List.of(conteneur));
        verify(conteneurs, never()).save(any());
    }

    @Test void copiesActiveDocumentsAndRemiseDatesWithNewRowsAndTheSameFiles() {
        Dossier mere = dossier(1), enfant = dossier(2);
        TypeDocument type = new TypeDocument(); type.setId(5);
        LocalDateTime ajout = LocalDateTime.of(2026, 10, 1, 8, 30);
        LocalDateTime remiseDate = ajout.minusDays(2);
        Document original = new Document(); original.setId(12); original.setDossier(mere);
        original.setTypeDocument(type); original.setNombre(2); original.setCheminFichier("1/facture.pdf");
        original.setNomFichier("facture.pdf"); original.setEstActif(true); original.setDateAjout(ajout);
        Remise remise = new Remise(); remise.setId(13); remise.setDossier(mere);
        remise.setTypeDocument(type); remise.setDateRemise(remiseDate);
        when(documents.findByDossierIdAndEstActifTrue(1)).thenReturn(List.of(original));
        when(remises.findByDossierId(1)).thenReturn(List.of(remise));

        service.copierRessources(mere, enfant);

        ArgumentCaptor<Iterable<Document>> copies = ArgumentCaptor.forClass(Iterable.class);
        verify(documents).saveAll(copies.capture());
        Document copie = copies.getValue().iterator().next();
        assertNotSame(original, copie); assertNull(copie.getId()); assertSame(enfant, copie.getDossier());
        assertSame(type, copie.getTypeDocument()); assertEquals(2, copie.getNombre());
        assertEquals(original.getCheminFichier(), copie.getCheminFichier());
        assertEquals("facture.pdf", copie.getNomFichier()); assertEquals(ajout, copie.getDateAjout());
        assertTrue(copie.getEstActif()); assertSame(mere, original.getDossier());
        ArgumentCaptor<Iterable<Remise>> copiesRemises = ArgumentCaptor.forClass(Iterable.class);
        verify(remises).saveAll(copiesRemises.capture());
        Remise copieRemise = copiesRemises.getValue().iterator().next();
        assertNotSame(remise, copieRemise); assertNull(copieRemise.getId());
        assertSame(enfant, copieRemise.getDossier()); assertSame(type, copieRemise.getTypeDocument());
        assertEquals(remiseDate, copieRemise.getDateRemise()); assertSame(mere, remise.getDossier());
    }

    @Test void emptyParentDoesNotInventResources() {
        service.copierRessources(dossier(1), dossier(2));
        verify(conteneurs).findByDossierId(1);
        verify(documents).findByDossierIdAndEstActifTrue(1);
        verify(remises).findByDossierId(1);
        verifyNoInteractions(eligibilite);
    }

    @Test void nonMaritimeChildCannotInheritContainersAndNothingIsWritten() {
        Dossier mere = dossier(1), enfant = dossier(2);
        when(conteneurs.findByDossierId(1)).thenReturn(List.of(new Conteneur()));
        when(eligibilite.estEligibleConteneur(enfant)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.copierRessources(mere, enfant));

        verify(conteneurs, never()).saveAll(any());
        verifyNoInteractions(documents, remises);
    }
}

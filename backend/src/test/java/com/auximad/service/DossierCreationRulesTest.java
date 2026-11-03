package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierCreationRulesTest {
    @Mock DossierRepository dossierRepository;
    @Mock OrdreTransitService ordreTransitService;
    @Mock PhaseDossierRepository phaseDossierRepository;
    @Mock StatutDossierRepository statutDossierRepository;
    @Mock IncontermeRepository incontermeRepository;
    @Mock AgenceRepository agenceRepository;
    @Mock TypeProductionRepository typeProductionRepository;
    @Mock DossierInteragenceRepository interagenceRepository;
    @Mock DocumentRepository documentRepository;
    @Mock RemiseRepository remiseRepository;
    @Mock DemandeInteragenceRepository demandeInteragenceRepository;
    @Mock DossierWorkflowService workflowService;
    @Mock ConteneurService conteneurService;
    @InjectMocks DossierService service;

    private Dossier dossier() {
        Dossier d = new Dossier();
        Agence a = new Agence(); a.setId(1); a.setCode(1); d.setAgence(a);
        TypeProduction tp = new TypeProduction(); tp.setId(1); tp.setCode(1); d.setTypeProduction(tp);
        Utilisateur u = new Utilisateur(); u.setId(1); d.setUtilisateur(u);
        Client c = new Client(); c.setId(1); d.setClient(c);
        return d;
    }

    @Test void rejectsUnvalidatedOtEvenWhenRequestClaimsItIsValidated() {
        Dossier d = dossier();
        OrdreTransit supplied = new OrdreTransit(); supplied.setId(7);
        StatutOt validated = new StatutOt(); validated.setId(3); supplied.setStatutOt(validated);
        d.setOrdreTransit(supplied);
        OrdreTransit persisted = new OrdreTransit(); persisted.setId(7);
        StatutOt pending = new StatutOt(); pending.setId(2); persisted.setStatutOt(pending);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(persisted));
        assertThrows(IllegalArgumentException.class, () -> service.saveDossier(d));
        verify(dossierRepository, never()).save(any());
        verify(ordreTransitService, never()).validateOrdreTransit(anyInt());
    }

    @Test void manualCreationCreatesAndLinksValidatedOt() {
        Dossier d = dossier();
        NatureMarchandise nature = new NatureMarchandise(); nature.setId(3); d.setNatureMarchandiseCreation(nature);
        OrdreTransit created = new OrdreTransit(); created.setId(7);
        when(ordreTransitService.createOrdreTransit(any(), any())).thenAnswer(invocation -> {
            OrdreTransit ot = invocation.getArgument(0);
            assertSame(d.getClient(), ot.getClient());
            assertSame(nature, ot.getNatureMarchandise());
            assertSame(d.getTypeProduction(), ot.getTypeProduction());
            Utilisateur author = invocation.getArgument(1);
            assertEquals(d.getUtilisateur().getId(), author.getId());
            assertSame(d.getAgence(), author.getAgence());
            return created;
        });
        when(ordreTransitService.validateOrdreTransit(7)).thenReturn(created);
        when(dossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Dossier saved = service.saveDossier(d);
        assertSame(created, saved.getOrdreTransit());
        assertNotNull(saved.getNumeroDossier());
        verify(ordreTransitService).validateOrdreTransit(7);
    }

    @Test void validatedOtAllowsCreationWithoutDocuments() {
        Dossier d = dossier();
        OrdreTransit ot = new OrdreTransit(); ot.setId(7);
        StatutOt status = new StatutOt(); status.setId(3); ot.setStatutOt(status); d.setOrdreTransit(ot);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(ot));
        when(dossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertSame(ot, service.saveDossier(d).getOrdreTransit());
        verifyNoInteractions(documentRepository, remiseRepository);
    }

    @Test void numberingContinuesFromExistingCounterUnderLock() {
        Dossier d = dossier();
        OrdreTransit ot = new OrdreTransit(); ot.setId(7);
        StatutOt status = new StatutOt(); status.setId(3); ot.setStatutOt(status); d.setOrdreTransit(ot);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(ot));
        when(dossierRepository.findMaxChronologie()).thenReturn(42);
        when(dossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(service.saveDossier(d).getNumeroDossier().endsWith("0043"));
        var order = inOrder(dossierRepository);
        order.verify(dossierRepository).verrouillerNumerotation();
        order.verify(dossierRepository).findMaxChronologie();
        order.verify(dossierRepository).save(d);
    }

    @Test void exhaustedCounterDoesNotGenerateAnInvalidNumber() {
        Dossier d = dossier();
        OrdreTransit ot = new OrdreTransit(); ot.setId(7);
        StatutOt status = new StatutOt(); status.setId(3); ot.setStatutOt(status); d.setOrdreTransit(ot);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(ot));
        when(dossierRepository.findMaxChronologie()).thenReturn(9999);
        assertThrows(IllegalArgumentException.class, () -> service.saveDossier(d));
        verify(dossierRepository, never()).save(any());
    }

    @Test void updatePreservesNumberWithoutAllocatingAnotherCounter() {
        Dossier d = dossier(); d.setId(8); d.setNumeroDossier("1211060001");
        when(dossierRepository.findById(8)).thenReturn(Optional.of(d));
        when(dossierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("1211060001", service.saveDossier(d).getNumeroDossier());
        verify(dossierRepository, never()).verrouillerNumerotation();
        verify(dossierRepository, never()).findMaxChronologie();
    }

    @Test void creationAssignsContainerOnlyToTheNewDossier() {
        Dossier d = dossierAvecConteneur();
        Dossier autre = new Dossier(); autre.setId(999);
        d.getConteneurCreation().setDossier(autre);
        preparerCreationAvecConteneur(d);
        when(dossierRepository.save(any())).thenAnswer(call -> {
            Dossier saved = call.getArgument(0); saved.setId(42); return saved;
        });
        service.saveDossier(d);
        var capture = org.mockito.ArgumentCaptor.forClass(Conteneur.class);
        verify(conteneurService).saveConteneur(capture.capture());
        Conteneur assigned = capture.getValue();
        assertEquals("MSCU1234567", assigned.getNumeroContainer());
        assertEquals(1, assigned.getDossiers().size());
        assertSame(d, assigned.getDossier());
        assertNull(d.getConteneurCreation());
        var ordre = inOrder(dossierRepository, conteneurService);
        ordre.verify(dossierRepository).save(d);
        ordre.verify(conteneurService).saveConteneur(assigned);
    }

    @Test void creationCarriesTheExistingContainerReferenceToAssignment() {
        Dossier d = dossierAvecConteneur(); d.getConteneurCreation().setId(7);
        preparerCreationAvecConteneur(d);
        when(dossierRepository.save(any())).thenAnswer(call -> {
            Dossier saved = call.getArgument(0); saved.setId(42); return saved;
        });
        service.saveDossier(d);
        verify(conteneurService).saveConteneur(argThat(c -> Integer.valueOf(7).equals(c.getId())
                && c.getDossiers().size() == 1 && c.getDossier().getId() == 42));
    }

    @Test void creationRejectsContainerOnNonMaritimeDossierBeforeSaving() {
        Dossier d = dossierAvecConteneur();
        when(typeProductionRepository.findById(1)).thenReturn(Optional.of(d.getTypeProduction()));
        assertThrows(IllegalArgumentException.class, () -> service.saveDossier(d));
        verify(dossierRepository, never()).save(any());
        verify(conteneurService, never()).saveConteneur(any());
        verifyNoInteractions(ordreTransitService);
    }

    @Test void creationRejectsContainerWithoutType() {
        Dossier d = dossierAvecConteneur(); d.getConteneurCreation().setTypeContainer(null);
        when(typeProductionRepository.findById(1)).thenReturn(Optional.of(d.getTypeProduction()));
        when(conteneurService.estEligibleConteneur(d)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.saveDossier(d));
        verify(dossierRepository, never()).save(any());
        verifyNoInteractions(ordreTransitService);
    }

    @Test void creationWithoutContainerDoesNotRequestAssignment() {
        Dossier d = dossier();
        OrdreTransit ot = new OrdreTransit(); ot.setId(7);
        StatutOt status = new StatutOt(); status.setId(3); ot.setStatutOt(status); d.setOrdreTransit(ot);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(ot));
        when(dossierRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.saveDossier(d);
        verifyNoInteractions(conteneurService);
    }

    private Dossier dossierAvecConteneur() {
        Dossier d = dossier();
        Conteneur c = new Conteneur(); c.setNumeroContainer(" MSCU1234567 ");
        TypeContainer type = new TypeContainer(); type.setId(3); c.setTypeContainer(type);
        d.setConteneurCreation(c);
        return d;
    }

    private void preparerCreationAvecConteneur(Dossier d) {
        when(typeProductionRepository.findById(1)).thenReturn(Optional.of(d.getTypeProduction()));
        when(conteneurService.estEligibleConteneur(d)).thenReturn(true);
        OrdreTransit ot = new OrdreTransit(); ot.setId(7);
        StatutOt status = new StatutOt(); status.setId(3); ot.setStatutOt(status); d.setOrdreTransit(ot);
        when(ordreTransitService.getOrdreTransitById(7)).thenReturn(Optional.of(ot));
    }
}

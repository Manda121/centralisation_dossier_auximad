package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConteneurEligibilityTest {
    private final ConteneurRepository conteneurs = mock(ConteneurRepository.class);
    private final TypeContainerRepository types = mock(TypeContainerRepository.class);
    private final DossierRepository dossiers = mock(DossierRepository.class);
    private final ConteneurService service = new ConteneurService(conteneurs, types, dossiers, mock(SuppressionService.class));

    private TypeProduction production(String mode) {
        ModeTransport transport = new ModeTransport(); transport.setNom(mode);
        TypeProduction production = new TypeProduction(); production.setModeTransport(transport);
        return production;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Aerien", "Terrestre", "combine"})
    void rejectsNonMaritimeAssignmentEvenWhenOtIsMaritime(String mode) {
        Dossier dossier = new Dossier(); dossier.setId(1); dossier.setTypeProduction(production(mode));
        OrdreTransit ot = new OrdreTransit(); ot.setTypeProduction(production("Maritime")); dossier.setOrdreTransit(ot);
        when(dossiers.findById(1)).thenReturn(Optional.of(dossier));
        Conteneur demande = new Conteneur(); demande.setDossier(dossier); demande.setNumeroContainer("TEST-001");
        assertFalse(service.estEligibleConteneur(dossier));
        assertThrows(IllegalArgumentException.class, () -> service.saveConteneur(demande));
        verifyNoInteractions(conteneurs, types);
    }

    @Test void acceptsMaritimeAndPersistsTheAssignment() {
        Dossier dossier = new Dossier(); dossier.setId(1); dossier.setTypeProduction(production(" Maritime "));
        when(dossiers.findById(1)).thenReturn(Optional.of(dossier));
        when(conteneurs.save(any())).thenAnswer(call -> call.getArgument(0));
        Conteneur demande = new Conteneur(); demande.setDossier(dossier); demande.setNumeroContainer(" TEST-001 ");
        Conteneur resultat = service.saveConteneur(demande);
        assertEquals("TEST-001", resultat.getNumeroContainer());
        assertTrue(resultat.getDossiers().contains(dossier));
    }

    @Test void fallsBackToOtOnlyWhenDossierModeIsAbsent() {
        Dossier dossier = new Dossier();
        OrdreTransit ot = new OrdreTransit(); ot.setTypeProduction(production("Maritime")); dossier.setOrdreTransit(ot);
        assertTrue(service.estEligibleConteneur(dossier));
        dossier.setTypeProduction(new TypeProduction());
        assertTrue(service.estEligibleConteneur(dossier));
    }

    @Test void productionNameAloneDoesNotOverrideMissingTransport() {
        Dossier dossier = new Dossier(); TypeProduction production = new TypeProduction();
        production.setNom("Import maritime"); dossier.setTypeProduction(production);
        assertFalse(service.estEligibleConteneur(dossier));
        assertFalse(service.estEligibleConteneur(null));
    }

    @Test void ambiguousMultipleDossiersAreRejectedBeforeChoosingOne() {
        Dossier maritime = new Dossier(); maritime.setId(1); maritime.setTypeProduction(production("Maritime"));
        Dossier aerien = new Dossier(); aerien.setId(2); aerien.setTypeProduction(production("Aerien"));
        Conteneur demande = new Conteneur(); demande.setDossier(maritime); demande.getDossiers().add(aerien);
        assertThrows(IllegalArgumentException.class, () -> service.saveConteneur(demande));
        verifyNoInteractions(dossiers, conteneurs, types);
    }

    @Test void assigningAnExistingContainerPreservesItsOtherAssociations() {
        Dossier precedent = new Dossier(); precedent.setId(1);
        Dossier nouveau = new Dossier(); nouveau.setId(2); nouveau.setTypeProduction(production("Maritime"));
        Conteneur existant = new Conteneur(); existant.setId(10); existant.setDossier(precedent);
        when(dossiers.findById(2)).thenReturn(Optional.of(nouveau));
        when(conteneurs.findByNumeroContainer("TEST-001")).thenReturn(Optional.of(existant));
        when(conteneurs.save(any())).thenAnswer(call -> call.getArgument(0));
        Conteneur demande = new Conteneur(); demande.setDossier(nouveau); demande.setNumeroContainer("TEST-001");
        assertSame(existant, service.saveConteneur(demande));
        assertTrue(existant.getDossiers().contains(precedent));
        assertTrue(existant.getDossiers().contains(nouveau));
        assertEquals(2, existant.getDossiers().size());
    }
}

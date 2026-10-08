package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierWorkflowServiceTest {
    @Mock DossierRepository dossiers;
    @Mock PhaseDossierRepository phases;
    @Mock StatutDossierRepository statuts;
    @Mock OperationDossierRepository operations;
    @Mock MvtDossierRepository mouvements;
    @Mock MvtAnnuleeRepository annulations;
    @Mock TypeDocumentRepository typesDocuments;
    @Mock DocumentRepository documents;
    @Mock UtilisateurRepository utilisateurs;
    @InjectMocks DossierWorkflowService workflow;

    private PhaseDossier phase(int id, String nom) {
        PhaseDossier phase = new PhaseDossier(); phase.setId(id); phase.setNom(nom); return phase;
    }

    private OperationDossier operation(int id, PhaseDossier phase) {
        OperationDossier op = new OperationDossier(); op.setId(id); op.setPhaseDossier(phase);
        op.setEstActif(true); op.setEstObligatoire(true); return op;
    }

    @Test void lastMandatoryPreparationMovementOpensNextPhase() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        PhaseDossier traitement = phase(2, "TRAITEMENT");
        TypeProduction production = new TypeProduction(); production.setId(3);
        Dossier dossier = new Dossier(); dossier.setTypeProduction(production); dossier.setPhaseDossier(preparation);
        OperationDossier premiere = operation(10, preparation);
        OperationDossier derniere = operation(11, preparation);
        OperationDossier suivante = operation(12, traitement);
        when(operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(3))
                .thenReturn(List.of(premiere, derniere, suivante));
        MvtDossier dejaFait = new MvtDossier(); dejaFait.setOperation(premiere);
        assertSame(traitement, workflow.phaseApresOperation(dossier, derniere, List.of(dejaFait)));
        assertSame(preparation, workflow.phaseApresOperation(dossier, premiere, List.of()));
    }

    @Test void cancellingAMovementAlsoCancelsFollowingMovementsAndRestoresState() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        PhaseDossier traitement = phase(2, "TRAITEMENT");
        StatutDossier ouvert = new StatutDossier(); ouvert.setId(1); ouvert.setNom("Ouvert");
        StatutDossier cloture = new StatutDossier(); cloture.setId(2); cloture.setNom("Clôturé");
        Utilisateur auteur = new Utilisateur(); auteur.setId(7);
        Dossier dossier = new Dossier(); dossier.setId(4); dossier.setUtilisateur(auteur);
        TypeProduction production = new TypeProduction(); production.setId(3); dossier.setTypeProduction(production);
        dossier.setPhaseDossier(traitement); dossier.setStatutDossier(cloture);
        OperationDossier opPremier = operation(10, preparation);
        OperationDossier opSecond = operation(11, traitement);
        OperationDossier opTroisieme = operation(12, traitement);
        MvtDossier premier = new MvtDossier(); premier.setId(20); premier.setDossier(dossier);
        premier.setOperation(opPremier);
        premier.setNouvellePhase(preparation); premier.setNouveauStatut(ouvert);
        MvtDossier second = new MvtDossier(); second.setId(21); second.setDossier(dossier);
        second.setOperation(opSecond);
        second.setNouvellePhase(traitement); second.setNouveauStatut(ouvert);
        MvtDossier troisieme = new MvtDossier(); troisieme.setId(22); troisieme.setDossier(dossier);
        troisieme.setOperation(opTroisieme);
        troisieme.setNouvellePhase(traitement); troisieme.setNouveauStatut(cloture);
        List<MvtDossier> tous = List.of(premier, second, troisieme);
        when(operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(3))
                .thenReturn(List.of(opPremier, opSecond, opTroisieme));
        when(mouvements.findById(21)).thenReturn(Optional.of(second));
        when(mouvements.findActifsByDossierIdOrderByIdAsc(4))
                .thenAnswer(call -> tous.stream().filter(m -> !Boolean.TRUE.equals(m.getEstAnnule())).toList());
        when(utilisateurs.findById(7)).thenReturn(Optional.of(auteur));
        workflow.annulerMouvement(21, "Erreur de saisie", true);
        assertNull(premier.getAnnulation());
        assertNotNull(second.getAnnulation());
        assertNotNull(troisieme.getAnnulation());
        assertEquals("Erreur de saisie", second.getAnnulation().getMotifAnnulation());
        assertTrue(second.getEstAnnule());
        assertTrue(troisieme.getEstAnnule());
        verify(annulations, times(2)).save(any(MvtAnnulee.class));
        assertSame(preparation, dossier.getPhaseDossier());
        assertSame(ouvert, dossier.getStatutDossier());
        verify(dossiers).save(dossier);
    }

    @Test void cancellingOptionalOperationKeepsFollowingMovementAndRequiresConfirmationForMandatory() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        StatutDossier ouvert = new StatutDossier(); ouvert.setId(1); ouvert.setNom("Ouvert");
        TypeProduction production = new TypeProduction(); production.setId(3);
        Utilisateur auteur = new Utilisateur(); auteur.setId(7);
        Dossier dossier = new Dossier(); dossier.setId(4); dossier.setTypeProduction(production);
        dossier.setUtilisateur(auteur); dossier.setPhaseDossier(preparation); dossier.setStatutDossier(ouvert);
        OperationDossier facultative = operation(10, preparation); facultative.setEstObligatoire(false);
        OperationDossier obligatoire = operation(11, preparation);
        facultative.setDescription("Option facultative"); obligatoire.setDescription("Validation obligatoire");
        MvtDossier premier = new MvtDossier(); premier.setId(20); premier.setDossier(dossier);
        premier.setOperation(facultative); premier.setNouvellePhase(preparation); premier.setNouveauStatut(ouvert);
        MvtDossier second = new MvtDossier(); second.setId(21); second.setDossier(dossier);
        second.setOperation(obligatoire); second.setNouvellePhase(preparation); second.setNouveauStatut(ouvert);
        when(mouvements.findById(20)).thenReturn(Optional.of(premier));
        when(mouvements.findActifsByDossierIdOrderByIdAsc(4))
                .thenAnswer(call -> List.of(premier, second).stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getEstAnnule())).toList());
        when(utilisateurs.findById(7)).thenReturn(Optional.of(auteur));

        DossierWorkflowService.ImpactAnnulation impact = workflow.prevoirAnnulation(20);
        assertEquals(List.of(20), impact.mouvementIds());
        assertEquals(List.of(obligatoire.getDescription()), impact.operationsObligatoiresSuivantes());
        assertThrows(ConfirmationAnnulationException.class, () -> workflow.annulerMouvement(20, null));
        verify(annulations, never()).save(any());

        workflow.annulerMouvement(20, null, true);
        assertTrue(premier.getEstAnnule());
        assertFalse(second.getEstAnnule());
        assertNull(second.getAnnulation());
        verify(annulations).save(any(MvtAnnulee.class));
    }

    @Test void completingAndRemovingRequiredDocumentsMovesAndRestoresPreparation() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        PhaseDossier traitement = phase(2, "TRAITEMENT");
        StatutDossier ouvert = new StatutDossier(); ouvert.setId(1); ouvert.setNom("Ouvert");
        TypeProduction production = new TypeProduction(); production.setId(3);
        Utilisateur auteur = new Utilisateur(); auteur.setId(7);
        Dossier dossier = new Dossier(); dossier.setId(4); dossier.setTypeProduction(production);
        dossier.setPhaseDossier(preparation); dossier.setStatutDossier(ouvert); dossier.setUtilisateur(auteur);
        OperationDossier premiere = operation(10, preparation); premiere.setStatutDossier(ouvert);
        OperationDossier suivante = operation(11, traitement);
        TypeDocument requis = new TypeDocument(); requis.setId(8); requis.setEstObligatoire(true);
        Document remis = new Document(); remis.setTypeDocument(requis);
        List<MvtDossier> enregistres = new ArrayList<>();
        when(dossiers.findById(4)).thenReturn(Optional.of(dossier));
        when(operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(3))
                .thenReturn(List.of(premiere, suivante));
        when(typesDocuments.findApplicables(3, null)).thenReturn(List.of(requis));
        when(documents.findByDossierIdAndEstActifTrue(4))
                .thenReturn(List.of(remis), List.of());
        when(mouvements.findActifsByDossierIdOrderByIdAsc(4))
                .thenAnswer(call -> enregistres.stream().filter(m -> !Boolean.TRUE.equals(m.getEstAnnule())).toList());
        when(mouvements.save(any())).thenAnswer(call -> {
            MvtDossier mvt = call.getArgument(0);
            if (!enregistres.contains(mvt)) enregistres.add(mvt);
            return mvt;
        });
        when(utilisateurs.findById(7)).thenReturn(Optional.of(auteur));
        when(statuts.findAll()).thenReturn(List.of(ouvert));

        workflow.synchroniserDocuments(4);
        assertSame(traitement, dossier.getPhaseDossier());
        assertEquals("DOCUMENTS", enregistres.get(0).getOrigine());

        workflow.synchroniserDocuments(4);
        assertNotNull(enregistres.get(0).getAnnulation());
        assertSame(preparation, dossier.getPhaseDossier());
        assertSame(ouvert, dossier.getStatutDossier());
    }

    @Test void manualPhaseAdvanceCanBeReversedWithoutDeletingHistory() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        PhaseDossier traitement = phase(2, "TRAITEMENT");
        PhaseDossier comptabilite = phase(3, "COMPTABILITE");
        StatutDossier ouvert = new StatutDossier(); ouvert.setId(1); ouvert.setNom("Ouvert");
        TypeProduction production = new TypeProduction(); production.setId(3);
        Utilisateur auteur = new Utilisateur(); auteur.setId(7);
        Dossier dossier = new Dossier(); dossier.setId(4); dossier.setTypeProduction(production);
        dossier.setPhaseDossier(preparation); dossier.setStatutDossier(ouvert); dossier.setUtilisateur(auteur);
        OperationDossier opPreparation = operation(10, preparation); opPreparation.setStatutDossier(ouvert);
        OperationDossier opTraitement = operation(11, traitement); opTraitement.setStatutDossier(ouvert);
        OperationDossier opComptabilite = operation(12, comptabilite); opComptabilite.setStatutDossier(ouvert);
        List<MvtDossier> historique = new ArrayList<>();
        when(dossiers.findById(4)).thenReturn(Optional.of(dossier));
        when(operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(3))
                .thenReturn(List.of(opPreparation, opTraitement, opComptabilite));
        when(mouvements.findActifsByDossierIdOrderByIdAsc(4))
                .thenAnswer(call -> historique.stream().filter(m -> !Boolean.TRUE.equals(m.getEstAnnule())).toList());
        when(mouvements.save(any())).thenAnswer(call -> {
            MvtDossier mvt = call.getArgument(0);
            if (!historique.contains(mvt)) { mvt.setId(20 + historique.size()); historique.add(mvt); }
            return mvt;
        });
        when(utilisateurs.findById(7)).thenReturn(Optional.of(auteur));

        workflow.changerPhase(4, 3);
        assertSame(comptabilite, dossier.getPhaseDossier());
        assertEquals(3, historique.size());
        assertEquals("AJUSTEMENT", historique.get(0).getOrigine());

        workflow.changerPhase(4, 2);
        assertNull(historique.get(0).getAnnulation());
        assertNotNull(historique.get(1).getAnnulation());
        assertNotNull(historique.get(2).getAnnulation());
        assertSame(traitement, dossier.getPhaseDossier());
    }

    @Test void returningFromManualClosureCancelsItsGeneratedMovements() {
        PhaseDossier preparation = phase(1, "PREPARATION");
        PhaseDossier comptabilite = phase(2, "COMPTABILITE");
        StatutDossier ouvert = new StatutDossier(); ouvert.setId(1); ouvert.setNom("Ouvert");
        StatutDossier cloture = new StatutDossier(); cloture.setId(2); cloture.setNom("Clôturé");
        TypeProduction production = new TypeProduction(); production.setId(3);
        Utilisateur auteur = new Utilisateur(); auteur.setId(7);
        Dossier dossier = new Dossier(); dossier.setId(4); dossier.setTypeProduction(production);
        dossier.setPhaseDossier(preparation); dossier.setStatutDossier(ouvert); dossier.setUtilisateur(auteur);
        OperationDossier premiere = operation(10, preparation); premiere.setStatutDossier(ouvert);
        OperationDossier derniere = operation(11, comptabilite); derniere.setStatutDossier(cloture);
        List<MvtDossier> historique = new ArrayList<>();
        when(dossiers.findById(4)).thenReturn(Optional.of(dossier));
        when(statuts.findById(2)).thenReturn(Optional.of(cloture));
        when(statuts.findById(1)).thenReturn(Optional.of(ouvert));
        when(statuts.findAll()).thenReturn(List.of(ouvert, cloture));
        when(operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(3))
                .thenReturn(List.of(premiere, derniere));
        when(mouvements.findActifsByDossierIdOrderByIdAsc(4))
                .thenAnswer(call -> historique.stream().filter(m -> !Boolean.TRUE.equals(m.getEstAnnule())).toList());
        when(mouvements.save(any())).thenAnswer(call -> {
            MvtDossier mvt = call.getArgument(0);
            if (!historique.contains(mvt)) { mvt.setId(20 + historique.size()); historique.add(mvt); }
            return mvt;
        });
        when(utilisateurs.findById(7)).thenReturn(Optional.of(auteur));

        workflow.changerStatut(4, 2);
        assertSame(cloture, dossier.getStatutDossier());
        assertEquals(2, historique.size());
        assertEquals(historique.get(0).getGroupeAjustement(), historique.get(1).getGroupeAjustement());

        workflow.changerStatut(4, 1);
        assertTrue(historique.stream().allMatch(m -> m.getAnnulation() != null));
        assertSame(preparation, dossier.getPhaseDossier());
        assertSame(ouvert, dossier.getStatutDossier());
    }
}

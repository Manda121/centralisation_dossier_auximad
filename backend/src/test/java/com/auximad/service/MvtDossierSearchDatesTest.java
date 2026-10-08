package com.auximad.service;

import com.auximad.repository.*;
import com.auximad.entity.MvtAnnulee;
import com.auximad.entity.MvtDossier;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MvtDossierSearchDatesTest {

    private final MvtDossierRepository mouvements = mock(MvtDossierRepository.class);
    private final MvtDossierService service = new MvtDossierService(
            mouvements,
            mock(DossierRepository.class),
            mock(OperationDossierRepository.class),
            mock(UtilisateurRepository.class),
            mock(DeclarationRepository.class),
            mock(DocumentRepository.class),
            mock(DocumentMvtRepository.class),
            mock(DocumentService.class),
            mock(DossierWorkflowService.class)
    );

    @Test
    void clearedDatesDoNotApplyAnImplicitTodayFilter() {
        when(mouvements.searchMouvements(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.searchMouvements(0, 15, "dateMvt", "desc", null,
                null, null, null, null, null, null, null, null, null, "", "");

        verify(mouvements).searchMouvements(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    void selectedDayIncludesItsEntireDuration() {
        when(mouvements.searchMouvements(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.searchMouvements(0, 15, "dateMvt", "desc", null,
                null, null, null, null, null, null, null, null, null,
                "2026-10-07", "2026-10-07");

        verify(mouvements).searchMouvements(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDateTime.of(2026, 10, 7, 0, 0)),
                eq(LocalDateTime.of(2026, 10, 7, 23, 59, 59, 999999999)), any());
    }

    @Test
    void historyShowsCancellationOnItsOwnDateEvenWhenCreationIsOlder() {
        MvtDossier mouvement = new MvtDossier();
        mouvement.setId(5);
        mouvement.setDateMvt(LocalDateTime.of(2026, 10, 1, 10, 0));
        mouvement.setEstAnnule(true);
        MvtAnnulee annulation = new MvtAnnulee();
        annulation.setId(8);
        annulation.setDateAnnulation(LocalDateTime.of(2026, 10, 8, 9, 0));
        mouvement.setAnnulation(annulation);
        when(mouvements.searchMouvements(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(mouvement)));

        var resultat = service.searchHistorique(0, 15, "desc", null,
                null, null, null, null, null, null, null, null, null,
                "2026-10-08", "2026-10-08");

        assertEquals(1, resultat.getTotalElements());
        assertEquals("ANNULATION", resultat.getContent().get(0).typeEvenement());
        assertEquals("annulation-8", resultat.getContent().get(0).id());
        var complet = service.searchHistorique(0, 15, "desc", null,
                null, null, null, null, null, null, null, null, null, "", "");
        assertEquals(2, complet.getTotalElements());
        assertEquals("ANNULATION", complet.getContent().get(0).typeEvenement());
        assertEquals("CREATION", complet.getContent().get(1).typeEvenement());
        verify(mouvements, times(2)).searchMouvements(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }
}

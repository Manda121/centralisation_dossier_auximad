package com.auximad.service;

import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

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
            mock(DocumentService.class)
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
}

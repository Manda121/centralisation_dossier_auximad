package com.auximad.controller;

import com.auximad.dto.DossierFiltres;
import com.auximad.dto.PageResponse;
import com.auximad.entity.Dossier;
import com.auximad.service.DossierService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DossierControllerFiltersTest {
    private DossierService service;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(DossierService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DossierController(service, mock(com.auximad.service.DossierWorkflowService.class))).build();
        when(service.searchDossiers(anyInt(), anyInt(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(DossierFiltres.class)))
                .thenReturn(new PageResponse<Dossier>());
    }

    private void authenticate(int level) {
        var auth = new UsernamePasswordAuthenticationToken("user", null);
        auth.setDetails(Jwts.claims().add("idAgence", 1).add("userId", 7)
                .add("niveauAutorisation", level).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void bindsCombinedFiltersAndSelectedAgencyForAuthorizedUser() throws Exception {
        authenticate(1010);
        mvc.perform(get("/api/dossiers").param("agenceId", "2").param("clientNom", "Martin")
                .param("cpClient", "001").param("numeroOt", "18").param("estPrepaye", "false")
                .param("provisionMin", "10.50").param("dateLivraisonMax", "2026-10-02")
                .param("documentsIncomplets", "true"))
                .andExpect(status().isOk());
        var filters = ArgumentCaptor.forClass(DossierFiltres.class);
        verify(service).searchDossiers(eq(0), eq(15), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(2), isNull(), filters.capture());
        assertEquals("Martin", filters.getValue().getClientNom());
        assertEquals("001", filters.getValue().getCpClient());
        assertEquals(18, filters.getValue().getNumeroOt());
        assertEquals(false, filters.getValue().getEstPrepaye());
        assertEquals(new java.math.BigDecimal("10.50"), filters.getValue().getProvisionMin());
        assertEquals("2026-10-02", filters.getValue().getDateLivraisonMax());
        assertEquals(true, filters.getValue().getDocumentsIncomplets());
    }

    @Test void agencyParameterCannotExpandOwnAgencyScope() throws Exception {
        authenticate(110);
        mvc.perform(get("/api/dossiers").param("agenceId", "2"))
                .andExpect(status().isOk());
        verify(service).searchDossiers(eq(0), eq(15), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(1), isNull(), any(DossierFiltres.class));
    }
}

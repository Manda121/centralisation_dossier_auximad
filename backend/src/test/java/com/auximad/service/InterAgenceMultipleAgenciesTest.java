package com.auximad.service;

import com.auximad.dto.SousDossierRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterAgenceMultipleAgenciesTest {
    private final DossierInteragenceRepository liens = mock(DossierInteragenceRepository.class);
    private final DossierRepository dossiers = mock(DossierRepository.class);
    private final AgenceRepository agences = mock(AgenceRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final DossierService sauvegarde = mock(DossierService.class);
    private final InterAgenceService service = new InterAgenceService(liens, dossiers, agences, utilisateurs, sauvegarde);

    @AfterEach void nettoyer() { SecurityContextHolder.clearContext(); }

    @Test void oneParentCanHaveChildrenInSeveralAgencies() {
        var auth = new UsernamePasswordAuthenticationToken("user", null);
        auth.setDetails(Jwts.claims().add("userId", 7).add("niveauAutorisation", 10000).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
        Agence origine = new Agence(); origine.setId(1);
        Dossier mere = new Dossier(); mere.setId(10); mere.setAgence(origine); mere.setOrdreTransit(new OrdreTransit());
        when(dossiers.verrouillerInteragence(10)).thenReturn(Optional.of(mere));
        when(utilisateurs.findById(7)).thenReturn(Optional.of(new Utilisateur()));
        when(agences.findById(anyInt())).thenAnswer(call -> {
            Agence agence = new Agence(); agence.setId(call.getArgument(0)); return Optional.of(agence);
        });
        when(sauvegarde.saveDossier(any())).thenAnswer(call -> call.getArgument(0));
        when(liens.save(any())).thenAnswer(call -> call.getArgument(0));
        SousDossierRequest request = new SousDossierRequest(); request.setIdAgence(2);
        DossierInteragence premier = service.creerSousDossier(10, request);
        request.setIdAgence(3);
        DossierInteragence deuxieme = service.creerSousDossier(10, request);
        assertSame(mere, premier.getDossierMere()); assertSame(mere, deuxieme.getDossierMere());
        assertEquals(2, premier.getSousDossier().getAgence().getId());
        assertEquals(3, deuxieme.getSousDossier().getAgence().getId());
        assertSame(mere.getOrdreTransit(), deuxieme.getSousDossier().getOrdreTransit());
        verify(liens, times(2)).save(any());
        when(liens.existsByDossierMereIdAndSousDossierAgenceId(10, 2)).thenReturn(true);
        request.setIdAgence(2);
        assertThrows(IllegalArgumentException.class, () -> service.creerSousDossier(10, request));
        verify(liens, times(2)).save(any());
    }
}

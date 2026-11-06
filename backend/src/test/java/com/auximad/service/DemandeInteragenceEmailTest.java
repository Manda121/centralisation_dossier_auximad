package com.auximad.service;

import com.auximad.dto.DemandeInteragenceRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeInteragenceEmailTest {
    @Mock DemandeInteragenceRepository demandes;
    @Mock DossierRepository dossiers;
    @Mock AgenceRepository agences;
    @Mock UtilisateurRepository utilisateurs;
    @Mock InterAgenceService interagence;
    @Mock NotificationInteragenceService notifications;
    @InjectMocks DemandeInteragenceService service;

    @BeforeEach void authentifier() {
        var auth = new UsernamePasswordAuthenticationToken("user", null);
        auth.setDetails(Jwts.claims().add("userId", 7).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    @AfterEach void nettoyer() { SecurityContextHolder.clearContext(); }

    private DemandeInteragenceRequest preparer() {
        Agence source = new Agence(); source.setId(1);
        Agence destination = new Agence(); destination.setId(2);
        Utilisateur user = new Utilisateur(); user.setAgence(source);
        Dossier dossier = new Dossier(); dossier.setId(10); dossier.setAgence(source);
        when(utilisateurs.findById(7)).thenReturn(Optional.of(user));
        when(dossiers.verrouillerInteragence(10)).thenReturn(Optional.of(dossier));
        when(agences.findById(2)).thenReturn(Optional.of(destination));
        var request = new DemandeInteragenceRequest(); request.setIdDossier(10); request.setIdAgenceDestinataire(2);
        return request;
    }

    @Test void requestIsSavedBeforeQueuingNotifications() {
        var request = preparer();
        when(demandes.save(any())).thenAnswer(call -> {
            DemandeInteragence d = call.getArgument(0); d.setId(42); return d;
        });
        var resultat = service.creerDemande(request);
        var ordre = inOrder(demandes, notifications);
        ordre.verify(demandes).findFirstByDossierIdAndAgenceDestinataireIdAndStatutInOrderByDateDemandeDesc(
                10, 2, List.of("EN_ATTENTE", "ACCEPTEE"));
        ordre.verify(demandes).save(resultat);
        ordre.verify(notifications).preparer(resultat);
        assertEquals(42, resultat.getId());
    }

    @Test void rejectedDuplicateDoesNotQueueAnyEmail() {
        var request = preparer();
        when(demandes.findFirstByDossierIdAndAgenceDestinataireIdAndStatutInOrderByDateDemandeDesc(
                10, 2, List.of("EN_ATTENTE", "ACCEPTEE")))
                .thenReturn(Optional.of(new DemandeInteragence()));
        assertThrows(IllegalArgumentException.class, () -> service.creerDemande(request));
        verifyNoInteractions(notifications);
        verify(demandes, never()).save(any());
    }

    @Test void sameDossierCanRequestSeveralDifferentAgencies() {
        var request = preparer();
        Agence autreAgence = new Agence(); autreAgence.setId(3);
        when(agences.findById(3)).thenReturn(Optional.of(autreAgence));
        var enregistrees = new java.util.ArrayList<DemandeInteragence>();
        when(demandes.save(any())).thenAnswer(call -> {
            DemandeInteragence demande = call.getArgument(0);
            demande.setId(42 + enregistrees.size()); enregistrees.add(demande); return demande;
        });
        when(demandes.findFirstByDossierIdAndAgenceDestinataireIdAndStatutInOrderByDateDemandeDesc(
                eq(10), anyInt(), anyList())).thenAnswer(call -> enregistrees.stream()
                    .filter(d -> d.getAgenceDestinataire().getId().equals(call.getArgument(1))).findFirst());
        service.creerDemande(request);
        request.setIdAgenceDestinataire(3);
        service.creerDemande(request);
        assertEquals(2, enregistrees.size());
        assertEquals(List.of(2, 3), enregistrees.stream().map(d -> d.getAgenceDestinataire().getId()).toList());
        verify(notifications, times(2)).preparer(any());
        request.setIdAgenceDestinataire(2);
        assertThrows(IllegalArgumentException.class, () -> service.creerDemande(request));
        assertEquals(2, enregistrees.size());
    }

    @Test void alreadyAssociatedAgencyCannotReceiveADuplicateRequest() {
        var request = preparer();
        when(interagence.agenceDejaAssociee(10, 2)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.creerDemande(request));
        verifyNoInteractions(notifications);
        verify(demandes, never()).save(any());
    }

    @Test void administratorRequestsOnBehalfOfTheDossierAgency() {
        var request = preparer();
        Utilisateur user = new Utilisateur(); Agence agence = new Agence(); agence.setId(9); user.setAgence(agence);
        when(utilisateurs.findById(7)).thenReturn(Optional.of(user));
        when(demandes.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(1, service.creerDemande(request).getAgenceDemandeuse().getId());
    }
}

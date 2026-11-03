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
        when(dossiers.findById(10)).thenReturn(Optional.of(dossier));
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
        ordre.verify(demandes).findFirstByDossierIdAndStatutOrderByDateDemandeDesc(10, "EN_ATTENTE");
        ordre.verify(demandes).save(resultat);
        ordre.verify(notifications).preparer(resultat);
        assertEquals(42, resultat.getId());
    }

    @Test void rejectedDuplicateDoesNotQueueAnyEmail() {
        var request = preparer();
        when(demandes.findFirstByDossierIdAndStatutOrderByDateDemandeDesc(10, "EN_ATTENTE"))
                .thenReturn(Optional.of(new DemandeInteragence()));
        assertThrows(IllegalArgumentException.class, () -> service.creerDemande(request));
        verifyNoInteractions(notifications);
        verify(demandes, never()).save(any());
    }
}

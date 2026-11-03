package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationInteragenceServiceTest {
    @Mock AdresseEmailAgenceRepository adresses;
    @Mock HistoriqueMailRepository historique;
    @InjectMocks NotificationInteragenceService service;
    private DemandeInteragence demande;

    @BeforeEach void setup() {
        Agence source = new Agence(); source.setId(1); source.setNom("Antananarivo");
        Agence destination = new Agence(); destination.setId(2); destination.setNom("Toamasina");
        Dossier dossier = new Dossier(); dossier.setNumeroDossier("DOS-2026-001");
        demande = new DemandeInteragence(); demande.setId(42); demande.setDossier(dossier);
        demande.setAgenceDemandeuse(source); demande.setAgenceDestinataire(destination);
        demande.setMotif("Dédouanement à réception");
    }

    @Test void queuesOneSeparateMessagePerActiveRecipient() {
        AdresseEmailAgence a = new AdresseEmailAgence(); a.setEmail("service@auximad.mg");
        AdresseEmailAgence b = new AdresseEmailAgence(); b.setEmail("direction@auximad.mg");
        when(adresses.findByAgenceIdAndActifTrueOrderByEmailAsc(2)).thenReturn(List.of(a, b));
        service.preparer(demande);
        var captor = ArgumentCaptor.forClass(HistoriqueMail.class);
        verify(historique, times(2)).save(captor.capture());
        assertNotSame(captor.getAllValues().get(0), captor.getAllValues().get(1));
        assertEquals(List.of(a.getEmail(), b.getEmail()), captor.getAllValues().stream()
                .map(HistoriqueMail::getDestinataire).toList());
        for (HistoriqueMail mail : captor.getAllValues()) {
            assertEquals(HistoriqueMail.Statut.EN_ATTENTE, mail.getStatut());
            assertEquals(42, mail.getIdDemandeInteragence());
            assertEquals(2, mail.getIdAgence());
            assertTrue(mail.getCorps().contains("Dédouanement à réception"));
            assertTrue(mail.getCorps().contains("Antananarivo"));
            assertTrue(mail.getSujet().contains("DOS-2026-001"));
            assertNull(mail.getDateEnvoi());
        }
        verify(historique).annulerEnAttente(42, HistoriqueMail.Statut.EN_ATTENTE, HistoriqueMail.Statut.ANNULE);
    }

    @Test void missingRecipientIsRecordedWithoutBlockingRequest() {
        when(adresses.findByAgenceIdAndActifTrueOrderByEmailAsc(2)).thenReturn(List.of());
        service.preparer(demande);
        var captor = ArgumentCaptor.forClass(HistoriqueMail.class);
        verify(historique).save(captor.capture());
        assertEquals(HistoriqueMail.Statut.NON_CONFIGURE, captor.getValue().getStatut());
        assertNull(captor.getValue().getDestinataire());
        assertNotNull(captor.getValue().getErreur());
    }

    @Test void retryPreservesOriginalAndResolvesNewlyConfiguredRecipients() {
        HistoriqueMail original = new HistoriqueMail(); original.setId(9L); original.setIdAgence(2);
        original.setStatut(HistoriqueMail.Statut.NON_CONFIGURE); original.setCorps("Message original");
        when(historique.verrouiller(9L)).thenReturn(Optional.of(original));
        AdresseEmailAgence a = new AdresseEmailAgence(); a.setEmail("service@auximad.mg");
        AdresseEmailAgence b = new AdresseEmailAgence(); b.setEmail("direction@auximad.mg");
        when(adresses.findByAgenceIdAndActifTrueOrderByEmailAsc(2)).thenReturn(List.of(a, b));
        service.relancer(9L);
        var captor = ArgumentCaptor.forClass(HistoriqueMail.class);
        verify(historique, times(2)).save(captor.capture());
        captor.getAllValues().forEach(mail -> {
            assertEquals(9L, mail.getIdMailOrigine());
            assertEquals("Message original", mail.getCorps());
            assertEquals(HistoriqueMail.Statut.EN_ATTENTE, mail.getStatut());
            assertNull(mail.getId());
        });
        assertEquals(HistoriqueMail.Statut.NON_CONFIGURE, original.getStatut());
    }

    @Test void deliveredMailCannotBeRetried() {
        HistoriqueMail original = new HistoriqueMail(); original.setStatut(HistoriqueMail.Statut.ENVOYE);
        when(historique.verrouiller(9L)).thenReturn(Optional.of(original));
        assertThrows(ResponseStatusException.class, () -> service.relancer(9L));
        verify(historique, never()).save(any());
    }

    @Test void repeatedRetryDoesNotCreateDuplicateMessages() {
        HistoriqueMail original = new HistoriqueMail(); original.setStatut(HistoriqueMail.Statut.ECHEC);
        when(historique.verrouiller(9L)).thenReturn(Optional.of(original));
        when(historique.existsByIdMailOrigine(9L)).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.relancer(9L))
                .getStatusCode().value());
        verify(historique, never()).save(any());
    }
}

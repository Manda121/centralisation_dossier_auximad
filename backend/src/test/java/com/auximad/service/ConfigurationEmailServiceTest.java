package com.auximad.service;

import com.auximad.dto.AdresseEmailAgenceRequest;
import com.auximad.entity.Agence;
import com.auximad.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationEmailServiceTest {
    private final AdresseEmailAgenceRepository adresses = mock(AdresseEmailAgenceRepository.class);
    private final AgenceRepository agences = mock(AgenceRepository.class);
    private final ConfigurationEmailService service = new ConfigurationEmailService(adresses, agences);

    @ParameterizedTest
    @ValueSource(strings = {"", "invalide", "a@exemple.mg,b@exemple.mg", "Nom <a@exemple.mg>", "a@exemple.mg\r\nBcc: b@exemple.mg"})
    void rejectsInvalidOrMultipleAddresses(String email) {
        assertThrows(ResponseStatusException.class, () -> ConfigurationEmailService.validerAdresse(email));
    }

    @Test void normalizesAndPersistsAddressWithAgencyAndActivation() {
        Agence agence = new Agence(); agence.setId(2);
        when(agences.findById(2)).thenReturn(Optional.of(agence));
        when(adresses.save(any())).thenAnswer(call -> call.getArgument(0));
        var adresse = service.enregistrer(null, new AdresseEmailAgenceRequest(2, " Service@Auximad.mg ", false));
        assertEquals("service@auximad.mg", adresse.getEmail());
        assertSame(agence, adresse.getAgence());
        assertFalse(adresse.isActif());
    }

    @Test void rejectsDuplicatesBeforePersisting() {
        Agence agence = new Agence(); agence.setId(2);
        when(agences.findById(2)).thenReturn(Optional.of(agence));
        when(adresses.existsByAgenceIdAndEmailAndIdNot(2, "service@auximad.mg", -1)).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.enregistrer(null,
                new AdresseEmailAgenceRequest(2, "SERVICE@auximad.mg", true))).getStatusCode().value());
        verify(adresses, never()).save(any());
    }
}

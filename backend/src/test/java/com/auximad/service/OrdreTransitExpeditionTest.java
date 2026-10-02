package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdreTransitExpeditionTest {
    @Mock OrdreTransitRepository ordreTransitRepository;
    @Mock ContratTransportRepository contratTransportRepository;
    @Mock InstructionRepository instructionRepository;
    @Mock StatutOtRepository statutOtRepository;
    @Mock CompagnieRepository compagnieRepository;
    @Mock NavireRepository navireRepository;
    @Mock TypeProductionRepository typeProductionRepository;
    @Mock TypeRegimeRepository typeRegimeRepository;
    @Mock BureauRepository bureauRepository;
    @Mock SuppressionService suppressionService;
    @InjectMocks OrdreTransitService service;

    private OrdreTransit create(OrdreTransit ot) {
        when(statutOtRepository.findById(2)).thenReturn(Optional.of(new StatutOt()));
        when(ordreTransitRepository.save(any())).thenAnswer(call -> {
            OrdreTransit saved = call.getArgument(0);
            saved.setId(42);
            if (saved.getExpedition() != null) {
                assertSame(saved, saved.getExpedition().getOrdreTransit());
                assertNull(saved.getExpedition().getDossier());
                assertNull(saved.getExpedition().getId());
            }
            return saved;
        });
        return service.createOrdreTransit(ot, new Utilisateur());
    }

    @Test void createsMaritimeExpeditionWithoutDossierAndPreservesDates() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OrdreTransit ot = mapper.readValue("""
            {"expedition":{"numeroBl":"BL-42","lieuBl":"Toamasina",
              "datePrevue":"2026-10-02T15:30:00+03:00","compagnie":{"id":1},"navire":{"id":2}},
             "contratCreation":{"numero":"BL-42","lieuEmission":"Toamasina","notify":"Client"}}
            """, OrdreTransit.class);
        OrdreTransit saved = create(ot);
        assertEquals("BL-42", saved.getExpedition().getNumeroBl());
        assertEquals(OffsetDateTime.parse("2026-10-02T15:30:00+03:00").toInstant(), saved.getExpedition().getDatePrevue().toInstant());
        ArgumentCaptor<ContratTransport> captor = ArgumentCaptor.forClass(ContratTransport.class);
        verify(contratTransportRepository).save(captor.capture());
        assertSame(saved, captor.getValue().getOrdreTransit());
        assertEquals("Client", captor.getValue().getNotify());
        var json = mapper.readTree(mapper.writeValueAsString(saved));
        assertEquals("BL-42", json.path("expedition").path("numeroBl").asText());
        assertFalse(json.path("expedition").has("ordreTransit"));
        assertFalse(json.has("contratCreation"));
    }

    @Test void createsAirExpeditionAndKeepsPartialInformation() {
        OrdreTransit ot = new OrdreTransit();
        Expedition expedition = new Expedition();
        expedition.setNumeroVol("MD051");
        expedition.setLieuLta("Antananarivo");
        ot.setExpedition(expedition);
        assertSame(expedition, create(ot).getExpedition());
        assertEquals("MD051", expedition.getNumeroVol());
        assertNull(expedition.getNumeroLta());
        verifyNoInteractions(contratTransportRepository);
    }

    @Test void stillAllowsOrderWithoutExpedition() {
        assertNull(create(new OrdreTransit()).getExpedition());
        verifyNoInteractions(contratTransportRepository);
    }

    @Test void transportFailureIsPropagatedToTheTransaction() {
        OrdreTransit ot = new OrdreTransit();
        ot.setContratCreation(new ContratTransport());
        when(contratTransportRepository.save(any())).thenThrow(new IllegalStateException("constraint"));
        assertThrows(IllegalStateException.class, () -> create(ot));
    }
}

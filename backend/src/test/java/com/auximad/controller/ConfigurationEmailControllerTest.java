package com.auximad.controller;

import com.auximad.repository.HistoriqueMailRepository;
import com.auximad.service.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ConfigurationEmailControllerTest {
    private final ConfigurationEmailService configuration = mock(ConfigurationEmailService.class);
    private final HistoriqueMailRepository historique = mock(HistoriqueMailRepository.class);
    private final NotificationInteragenceService notifications = mock(NotificationInteragenceService.class);
    private final EnvoiMailService envoi = mock(EnvoiMailService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ConfigurationEmailController(configuration, historique, notifications, envoi)).build();

    private void authentifier(int niveau) {
        var auth = new UsernamePasswordAuthenticationToken("user", null);
        auth.setDetails(Jwts.claims().add("niveauAutorisation", niveau).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach void nettoyer() { SecurityContextHolder.clearContext(); }

    @Test void allEndpointsRejectNonAdministratorsBeforeAccessingData() throws Exception {
        authentifier(9999);
        mvc.perform(get("/api/admin/emails/adresses")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/emails/historique")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/emails/configuration")).andExpect(status().isForbidden());
        String corps = "{\"idAgence\":2,\"email\":\"service@auximad.mg\",\"actif\":true}";
        mvc.perform(post("/api/admin/emails/adresses").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/emails/adresses/1").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/emails/historique/1/relancer")).andExpect(status().isForbidden());
        verifyNoInteractions(configuration, historique, notifications, envoi);
    }

    @Test void administratorCanCheckConfigurationWithoutReceivingSecrets() throws Exception {
        authentifier(10000); when(envoi.estConfigure()).thenReturn(true);
        mvc.perform(get("/api/admin/emails/configuration")).andExpect(status().isOk())
                .andExpect(content().json("{\"smtpConfigure\":true}", true));
    }

    @Test void administratorCanQueueRetry() throws Exception {
        authentifier(10000);
        mvc.perform(post("/api/admin/emails/historique/1/relancer")).andExpect(status().isAccepted());
        verify(notifications).relancer(1L);
    }

    @Test void invalidPaginationDoesNotReachDatabase() throws Exception {
        authentifier(10000);
        mvc.perform(get("/api/admin/emails/historique").param("size", "1000"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(historique);
    }
}

package com.auximad.config;

import com.auximad.entity.Action;
import com.auximad.service.HistorisationService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

class AuditInterceptorTest {
    private final HistorisationService service = mock(HistorisationService.class);
    private final AuditInterceptor interceptor = new AuditInterceptor(service);

    @BeforeEach
    void authenticate() {
        var authentication = new UsernamePasswordAuthenticationToken("user", null);
        authentication.setDetails(Jwts.claims().add("userId", 7).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/dossiers/sans-documents", "/api/documents/remises/urgentes",
            "/api/historiques/search", "/api/historiques/actions", "/api/historiques/utilisateurs",
            "/api/historiques/filtres", "/api/historiques", "/api/chat/etat"})
    void repeatedAutomaticAndHistoryReadsDoNotCreateAuditEntries(String uri) {
        for (int i = 0; i < 20; i++) {
            var request = new MockHttpServletRequest("GET", uri);
            request.setQueryString("jours=7&agenceId=1");
            interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        }
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/documents/12/fichier", "/api/dossiers/12", "/api/documents"})
    void documentOpeningAndOtherReadsRemainAudited(String uri) {
        interceptor.afterCompletion(new MockHttpServletRequest("GET", uri),
                new MockHttpServletResponse(), new Object(), null);
        verify(service).enregistrerPourUtilisateur(eq(7), eq(Action.VISUALISATION),
                contains("GET " + uri), anyString(), isNull(), eq(200));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void writesRemainAudited(String method) {
        interceptor.afterCompletion(new MockHttpServletRequest(method, "/api/documents/12"),
                new MockHttpServletResponse(), new Object(), null);
        String action = switch (method) {
            case "POST" -> Action.CREATION;
            case "DELETE" -> Action.SUPPRESSION;
            default -> Action.MODIFICATION;
        };
        verify(service).enregistrerPourUtilisateur(eq(7), eq(action),
                contains(method + " /api/documents/12"), eq("Document"), isNull(), eq(200));
    }
}
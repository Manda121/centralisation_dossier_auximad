package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvoiMailServiceTest {
    private final HistoriqueMailRepository historique = mock(HistoriqueMailRepository.class);
    private final DemandeInteragenceRepository demandes = mock(DemandeInteragenceRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    private HistoriqueMail enAttente() {
        HistoriqueMail mail = new HistoriqueMail(); mail.setId(1L); mail.setIdDemandeInteragence(42);
        mail.setIdAgence(2); mail.setDestinataire("service@auximad.mg");
        mail.setSujet("Demande interagences — dédouanement"); mail.setCorps("Bonjour, demande à réception.");
        when(historique.verrouiller(1L)).thenReturn(Optional.of(mail));
        Agence agence = new Agence(); agence.setId(2);
        DemandeInteragence demande = new DemandeInteragence(); demande.setAgenceDestinataire(agence);
        when(demandes.findById(42)).thenReturn(Optional.of(demande));
        return mail;
    }

    private EnvoiMailService service(String from, String host) {
        return new EnvoiMailService(historique, provider, from, host, demandes);
    }

    @Test void missingSmtpIsRecordedAndDoesNotAttemptSending() {
        var mail = enAttente();
        service("", "").traiter(1L);
        assertEquals(HistoriqueMail.Statut.NON_CONFIGURE, mail.getStatut());
        assertNull(mail.getDateEnvoi());
        verify(provider, never()).getObject();
    }

    @Test void authenticationFailureIsRecordedWithoutLeakingCredentials() {
        var mail = enAttente();
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender); when(provider.getObject()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("SECRET-SMTP-PASSWORD")).when(sender).send(any(MimeMessage.class));
        service("notification@auximad.mg", "smtp.test").traiter(1L);
        assertEquals(HistoriqueMail.Statut.ECHEC, mail.getStatut());
        assertNotNull(mail.getDateTentative()); assertNull(mail.getDateEnvoi());
        assertFalse(mail.getErreur().contains("SECRET"));
        verify(historique).save(mail);
    }

    @Test void alreadySentMessageIsNotSentAgain() {
        HistoriqueMail mail = new HistoriqueMail(); mail.setStatut(HistoriqueMail.Statut.ENVOYE);
        when(historique.verrouiller(1L)).thenReturn(Optional.of(mail));
        service("notification@auximad.mg", "smtp.test").traiter(1L);
        verifyNoInteractions(provider, demandes);
        verify(historique, never()).save(any());
    }

    @Test void obsoleteDestinationCancelsQueuedMessage() {
        var mail = enAttente(); mail.setIdAgence(3);
        service("notification@auximad.mg", "smtp.test").traiter(1L);
        assertEquals(HistoriqueMail.Statut.ANNULE, mail.getStatut());
        verifyNoInteractions(provider);
    }

    @Test void jakartaMailDeliversUtf8MessageToLocalSmtpServer() throws Exception {
        var mail = enAttente();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(5000);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> reception = executor.submit(() -> recevoirSmtp(server));
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(server.getInetAddress().getHostAddress()); sender.setPort(server.getLocalPort());
                sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "5000");
                sender.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
                when(provider.getIfAvailable()).thenReturn(sender); when(provider.getObject()).thenReturn(sender);
                service("notification@auximad.mg", sender.getHost()).traiter(1L);
                String donnees = reception.get(6, TimeUnit.SECONDS);
                MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                        new ByteArrayInputStream(donnees.getBytes(StandardCharsets.US_ASCII)));
                assertEquals("Demande interagences — dédouanement", message.getSubject());
                assertEquals("Bonjour, demande à réception.", message.getContent().toString().strip());
                assertEquals("service@auximad.mg", message.getAllRecipients()[0].toString());
                assertEquals("notification@auximad.mg", message.getFrom()[0].toString());
                assertEquals(HistoriqueMail.Statut.ENVOYE, mail.getStatut());
                assertNotNull(mail.getDateEnvoi()); assertNull(mail.getErreur());
            } finally { executor.shutdownNow(); }
        }
    }

    private String recevoirSmtp(ServerSocket server) throws IOException {
        try (Socket socket = server.accept()) {
            socket.setSoTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
            repondre(writer, "220 localhost test");
            StringBuilder contenu = new StringBuilder();
            boolean data = false;
            for (String ligne; (ligne = reader.readLine()) != null;) {
                if (data) {
                    if (ligne.equals(".")) { data = false; repondre(writer, "250 accepted"); }
                    else contenu.append(ligne.startsWith("..") ? ligne.substring(1) : ligne).append("\r\n");
                } else if (ligne.startsWith("DATA")) { repondre(writer, "354 send message"); data = true; }
                else if (ligne.startsWith("QUIT")) { repondre(writer, "221 bye"); break; }
                else repondre(writer, "250 OK");
            }
            return contenu.toString();
        }
    }

    private void repondre(BufferedWriter writer, String texte) throws IOException {
        writer.write(texte + "\r\n"); writer.flush();
    }
}

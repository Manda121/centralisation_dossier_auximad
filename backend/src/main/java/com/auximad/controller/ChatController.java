package com.auximad.controller;

import com.auximad.ai.ChatService;
import com.auximad.dto.ChatConfirmation;
import com.auximad.dto.ChatRequete;
import com.auximad.dto.ChatReponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Points d'entrée du chatbot Auximad.
 *
 * Toutes les routes sont sous {@code /api/chat} et donc authentifiées par la configuration
 * de sécurité existante : le chatbot agit toujours avec l'identité de l'utilisateur connecté.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Pose une question à l'assistant. */
    @PostMapping("/message")
    public ResponseEntity<ChatReponse> message(@RequestBody(required = false) ChatRequete requete) {
        return ResponseEntity.ok(chatService.envoyer(requete));
    }

    /** Valide ou refuse l'opération mise en attente par l'assistant. */
    @PostMapping("/confirmation")
    public ResponseEntity<ChatReponse> confirmation(@RequestBody ChatConfirmation confirmation) {
        if (confirmation == null) {
            return ResponseEntity.badRequest().body(ChatReponse.erreur("Aucune décision reçue."));
        }
        return ResponseEntity.ok(chatService.confirmer(confirmation));
    }

    /** Questions proposées au démarrage. */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> suggestions() {
        return ResponseEntity.ok(chatService.suggestions());
    }

    /** Outils exposés à l'utilisateur, avec le niveau requis et son autorisation. */
    @GetMapping("/outils")
    public ResponseEntity<List<Map<String, Object>>> outils() {
        return ResponseEntity.ok(chatService.outils());
    }

    /** État du service (disponibilité d'Ollama, modèle, nombre d'outils). */
    @GetMapping("/etat")
    public ResponseEntity<Map<String, Object>> etat() {
        return ResponseEntity.ok(chatService.etat());
    }

    /** Repart d'une conversation vierge. */
    @DeleteMapping("/session")
    public ResponseEntity<ChatReponse> reinitialiser() {
        return ResponseEntity.ok(chatService.nouvelleConversation());
    }

    // ─────────── Gestion des erreurs ───────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ChatReponse> securite(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ChatReponse.erreur(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ChatReponse> requeteInvalide(IllegalArgumentException e) {
        log.debug("Requête chatbot invalide : {}", e.getMessage());
        return ResponseEntity.badRequest().body(ChatReponse.erreur(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatReponse> inattendue(Exception e) {
        log.error("Erreur du chatbot", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ChatReponse.erreur("L'assistant a rencontré une erreur inattendue. Réessayez dans un instant."));
    }
}
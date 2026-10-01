package com.auximad.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP du serveur Ollama local (API /api/chat).
 * Utilise le « tool calling » natif : les outils sont déclarés dans la requête et le modèle
 * répond soit par un appel de fonction, soit par du texte.
 */
@Service
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestTemplate rest;
    private final String baseUrl;
    private final String modele;
    private final double temperature;
    private final int numCtx;
    private final int maxIterations;

    public OllamaClient(RestTemplateBuilder builder,
                        @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
                        @Value("${ollama.model:qwen2.5:3b}") String modele,
                        @Value("${ollama.temperature:0.1}") double temperature,
                        @Value("${ollama.num-ctx:8192}") int numCtx,
                        @Value("${ollama.max-iterations:6}") int maxIterations,
                        @Value("${ollama.timeout-seconds:300}") int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.modele = modele;
        this.temperature = temperature;
        this.numCtx = numCtx;
        this.maxIterations = maxIterations;
        this.rest = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public String modele() {
        return modele;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public int maxIterations() {
        return maxIterations;
    }

    /** Erreur applicative remontée à l'utilisateur quand Ollama est injoignable. */
    public static class OllamaIndisponibleException extends RuntimeException {
        public OllamaIndisponibleException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public boolean estDisponible() {
        try {
            rest.getForObject(baseUrl + "/api/tags", String.class);
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    /** Modèles installés sur le serveur Ollama local. */
    @SuppressWarnings("unchecked")
    public List<String> modeles() {
        try {
            Map<String, Object> reponse = rest.getForObject(baseUrl + "/api/tags", Map.class);
            List<String> noms = new ArrayList<>();
            if (reponse != null && reponse.get("models") instanceof List<?> models) {
                for (Object o : models) {
                    if (o instanceof Map<?, ?> m && m.get("name") != null) {
                        noms.add(String.valueOf(m.get("name")));
                    }
                }
            }
            return noms;
        } catch (RestClientException e) {
            return List.of();
        }
    }

    // ─────────────── Appel de chat ───────────────

    public static Map<String, Object> messageSystem(String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "system");
        m.put("content", contenu);
        return m;
    }

    public static Map<String, Object> messageUtilisateur(String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "user");
        m.put("content", contenu);
        return m;
    }

    public static Map<String, Object> messageAssistant(String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "assistant");
        m.put("content", contenu == null ? "" : contenu);
        return m;
    }

    public static Map<String, Object> messageAssistantAvecOutils(String contenu, List<Map<String, Object>> appels) {
        Map<String, Object> m = messageAssistant(contenu);
        m.put("tool_calls", appels);
        return m;
    }

    public static Map<String, Object> messageOutil(String nomOutil, String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", "tool");
        m.put("tool_name", nomOutil);
        m.put("content", contenu == null ? "" : contenu);
        return m;
    }

    /**
     * Convertit un outil métier en déclaration « tool » attendue par Ollama.
     */
    public static Map<String, Object> declarationOutil(OutilChat outil) {
        Map<String, Object> fonction = new LinkedHashMap<>();
        fonction.put("name", outil.nom());
        fonction.put("description", outil.description());
        fonction.put("parameters", outil.schema());

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", fonction);
        return tool;
    }

    /**
     * Envoie un tour de conversation au modèle.
     *
     * @param messages historique complet, y compris le message utilisateur courant
     * @param outils   déclarations des outils autorisés (vide = mode conversation simple)
     */
    public Reponse chatting(List<Map<String, Object>> messages, List<Map<String, Object>> outils) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("model", modele);
        corps.put("stream", false);
        corps.put("messages", messages);
        if (outils != null && !outils.isEmpty()) {
            corps.put("tools", outils);
        }
        corps.put("options", Map.of(
                "temperature", temperature,
                "num_ctx", numCtx,
                "top_p", 0.9,
                "repeat_penalty", 1.05));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JsonNode reponse;
        try {
            reponse = rest.postForObject(baseUrl + "/api/chat", new HttpEntity<>(corps, headers), JsonNode.class);
        } catch (RestClientException e) {
            log.warn("Appel Ollama échoué (modèle={}) : {}", modele, e.getMessage());
            throw new OllamaIndisponibleException(
                    "Le modèle « " + modele + " » est injoignable. Vérifiez qu'Ollama tourne sur " + baseUrl
                            + " et que le modèle est installé (« ollama pull " + modele + " »).", e);
        }

        if (reponse == null) {
            throw new OllamaIndisponibleException("Réponse vide du serveur Ollama.", null);
        }

        JsonNode message = reponse.path("message");
        String contenu = message.path("content").asText("");
        List<AppelOutil> appels = new ArrayList<>();

        for (JsonNode tc : message.path("tool_calls")) {
            JsonNode fn = tc.path("function");
            Map<String, Object> arguments = new LinkedHashMap<>();
            JsonNode argsNode = fn.path("arguments");
            if (argsNode.isObject()) {
                argsNode.fields().forEachRemaining(e -> arguments.put(e.getKey(), valeur(e.getValue())));
            } else if (argsNode.isTextual() && !argsNode.asText().isBlank()) {
                // Certains modèles renvoie les arguments sous forme de JSON texte
                try {
                    JsonNode parse = new com.fasterxml.jackson.databind.ObjectMapper().readTree(argsNode.asText());
                    if (parse.isObject()) {
                        parse.fields().forEachRemaining(e -> arguments.put(e.getKey(), valeur(e.getValue())));
                    }
                } catch (Exception ignored) {
                    // arguments illisibles : on laisse le modèle récupérer l'erreur
                }
            }
            appels.add(new AppelOutil(
                    tc.path("id").asText(null),
                    fn.path("name").asText(""),
                    arguments));
        }

        return new Reponse(contenu, appels);
    }

    /** Convertit un nœud JSON en valeur Java simple (les tableaux deviennent des List). */
    private Object valeur(JsonNode node) {
        if (node.isNumber()) {
            return node.intValue();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            List<Object> liste = new ArrayList<>();
            node.forEach(n -> liste.add(valeur(n)));
            return liste;
        }
        if (node.isNull()) {
            return null;
        }
        return node.asText();
    }

    /** Un appel de fonction demandé par le modèle. */
    public record AppelOutil(String id, String nom, Map<String, Object> arguments) {
    }

    /**
     * Re-sérialise des appels d'outils dans la forme attendue par Ollama, afin de pouvoir
     * les réinjecter dans l'historique quand la boucle est suspendue pour une confirmation
     * puis reprise.
     */
    public static List<Map<String, Object>> serialiserAppels(List<AppelOutil> appels) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (appels == null) {
            return out;
        }
        for (AppelOutil a : appels) {
            Map<String, Object> fn = new LinkedHashMap<>();
            fn.put("name", a.nom());
            fn.put("arguments", a.arguments() == null ? Map.of() : a.arguments());

            Map<String, Object> tc = new LinkedHashMap<>();
            if (a.id() != null) {
                tc.put("id", a.id());
            }
            tc.put("function", fn);
            out.add(tc);
        }
        return out;
    }

    /** Réponse d'un tour de chat. */
    public record Reponse(String contenu, List<AppelOutil> appelsOutils) {
        public boolean aAppelsOutils() {
            return appelsOutils != null && !appelsOutils.isEmpty();
        }
    }
}
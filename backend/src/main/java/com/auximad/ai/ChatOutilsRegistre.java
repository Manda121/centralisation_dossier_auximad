package com.auximad.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registre des outils conversationnels.
 * Collecte automatiquement tous les beans {@link OutilChat} et n'expose au modèle que
 * ceux que l'utilisateur a le droit d'utiliser (filtre par niveau d'autorisation).
 */
@Component
public class ChatOutilsRegistre {

    private static final Logger log = LoggerFactory.getLogger(ChatOutilsRegistre.class);

    private final Map<String, OutilChat> outils = new LinkedHashMap<>();

    public ChatOutilsRegistre(List<OutilChat> tous) {
        for (OutilChat outil : tous) {
            if (outils.putIfAbsent(outil.nom(), outil) != null) {
                throw new IllegalStateException("Deux outils portent le même nom : " + outil.nom());
            }
        }
        log.info("Assistant conversationnel : {} outil(s) enregistré(s) : {}", outils.size(), outils.keySet());
    }

    public OutilChat parNom(String nom) {
        return outils.get(nom);
    }

    /** Outils que l'utilisateur courant a le droit d'invoquer. */
    public List<OutilChat> disponibles(ContexteChat contexte) {
        List<OutilChat> liste = new ArrayList<>();
        for (OutilChat outil : outils.values()) {
            if (contexte.niveauAutorisation() >= outil.niveauRequis()) {
                liste.add(outil);
            }
        }
        liste.sort(Comparator.comparing(OutilChat::nom));
        return liste;
    }

    /** Déclarations « tools » au format attendu par Ollama. */
    public List<Map<String, Object>> declarations(ContexteChat contexte) {
        return disponibles(contexte).stream().map(OllamaClient::declarationOutil).toList();
    }

    /** Vue simplifiée pour l'écran « outils disponibles » du chatbot. */
    public List<Map<String, Object>> apercu(ContexteChat contexte) {
        List<Map<String, Object>> apercu = new ArrayList<>();
        for (OutilChat outil : outils.values()) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("nom", outil.nom());
            ligne.put("description", outil.description());
            ligne.put("danger", outil.danger().name());
            ligne.put("niveauRequis", outil.niveauRequis());
            ligne.put("autorise", contexte.niveauAutorisation() >= outil.niveauRequis());
            apercu.add(ligne);
        }
        apercu.sort(Comparator.comparing(l -> String.valueOf(l.get("nom"))));
        return apercu;
    }
}
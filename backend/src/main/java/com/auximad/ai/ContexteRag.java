package com.auximad.ai;

import java.util.List;

/**
 * Résultat de la récupération RAG pour une question : le texte à injecter dans le prompt
 * et les sources citées à afficher sous la réponse.
 */
public record ContexteRag(String texte, List<Source> sources) {

    /** Provenance d'un fragment de contexte. */
    public record Source(String type, String libelle, String detail) {
    }

    public static ContexteRag vide() {
        return new ContexteRag("", List.of());
    }

    public boolean estVide() {
        return texte == null || texte.isBlank();
    }
}
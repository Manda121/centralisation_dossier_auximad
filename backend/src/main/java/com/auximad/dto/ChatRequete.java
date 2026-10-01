package com.auximad.dto;

/**
 * Question posée par l'utilisateur au chatbot.
 *
 * @param message texte libre saisi dans le widget
 * @param effacer true pour repartir d'une conversation vierge avant de traiter la question
 */
public record ChatRequete(String message, Boolean effacer) {

    public String texte() {
        return message == null ? null : message.trim();
    }

    public boolean doitEffacer() {
        return Boolean.TRUE.equals(effacer);
    }
}
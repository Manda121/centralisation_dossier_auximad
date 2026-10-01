package com.auximad.dto;

/**
 * Décision de l'utilisateur sur une opération en attente de confirmation.
 *
 * @param accepte     true si l'utilisateur valide, false s'il annule
 * @param id          identifiant de la demande de confirmation affichée dans l'interface
 * @param identifiant identifiant de l'utilisateur connecté saisi pour confirmation
 */
public record ChatConfirmation(Boolean accepte, String id, String identifiant) {

    public boolean estAccepte() {
        return Boolean.TRUE.equals(accepte);
    }
}
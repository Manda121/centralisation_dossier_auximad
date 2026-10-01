package com.auximad.dto;

import java.util.List;

/**
 * Réponse renvoyée au widget de chat.
 *
 * @param reponse      texte affiché à l'utilisateur
 * @param sources      provenance des données utilisées (« Base — Dossiers », « Documentation », …)
 * @param confirmation présente uniquement lorsqu'une opération dangereuse attend une décision
 * @param outilsUtilises noms des outils effectivement exécutés (pour la transparence)
 * @param avertissement message d'erreur applicatif, sinon null
 */
public record ChatReponse(String reponse,
                          List<String> sources,
                          Confirmation confirmation,
                          List<String> outilsUtilises,
                          String avertissement) {

    public static ChatReponse simple(String reponse) {
        return new ChatReponse(reponse, List.of(), null, List.of(), null);
    }

    public static ChatReponse erreur(String message) {
        return new ChatReponse(message, List.of(), null, List.of(), message);
    }

    /**
     * Opération en attente de décision explicite de l'utilisateur.
     *
     * @param id      identifiant technique de la demande de confirmation
     * @param outil   nom de la fonction qui sera exécutée si l'utilisateur accepte
     * @param resume  libellé lisible de l'opération (« Supprimer le dossier 30-70-01-6-0001 »)
     * @param danger  niveau de danger (MODIFICATION ou CRITIQUE)
     * @param details paramètres qui seront réellement appliqués
     */
    public record Confirmation(String id,
                               String outil,
                               String resume,
                               String danger,
                               List<String> details) {
    }
}
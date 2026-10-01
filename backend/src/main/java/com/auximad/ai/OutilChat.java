package com.auximad.ai;

import java.util.Map;

/**
 * Un outil (fonction) exposé au modèle via le « function calling » d'Ollama.
 * Chaque implémentation est un bean Spring : le registre les collecte automatiquement.
 */
public interface OutilChat {

    /** Nom de la fonction, en snake_case (ex: rechercher_dossiers). */
    String nom();

    /** Description destinée au modèle : doit dire QUAND et POURQUOI l'utiliser. */
    String description();

    /** Schéma JSON des paramètres : objet avec "properties" et "required". */
    Map<String, Object> schema();

    /** Niveau de danger : tout ce qui n'est pas LECTURE passe par une confirmation humaine. */
    NiveauDanger danger();

    /** Niveau d'autorisation minimal requis pour utiliser l'outil (formule portée + action). */
    int niveauRequis();

    /** Libellé lisible de l'opération, utilisé pour la carte de confirmation côté utilisateur. */
    String resume(Map<String, Object> arguments, ContexteChat contexte);

    /**
     * Exécute l'outil.
     * @return le résultat, sous forme de texte court destiné à être renvoyé au modèle
     * @throws IllegalArgumentException si les arguments sont invalides ou l'opération impossible
     * @throws SecurityException si l'utilisateur n'a pas les droits requis
     */
    String executer(Map<String, Object> arguments, ContexteChat contexte);
}
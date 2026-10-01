package com.auximad.ai;

/**
 * Niveau de danger d'un outil exposé au modèle.
 * Détermine si l'exécution est immédiate ou si une confirmation humaine est exigée.
 */
public enum NiveauDanger {

    /** Simple lecture : exécution immédiate, aucune confirmation. */
    LECTURE,

    /** Écriture métier réversible (changement de phase, remise de documents…) : confirmation exigée. */
    MODIFICATION,

    /** Action grave et difficilement réversible (suppression, refus/acceptation de demande inter-agences) : confirmation exigée. */
    CRITIQUE
}
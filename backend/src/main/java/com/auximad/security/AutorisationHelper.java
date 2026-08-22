package com.auximad.security;

/**
 * Helper de sécurité pour la vérification des permissions.
 * Vérifie la formule : niveauAutorisation >= (portée + codeAction)
 */
public final class AutorisationHelper {

    // ========== Portées Opérationnelles (Propre agence) ==========
    public static final int PORTEE_PROPRE_LECTURE = 100;
    public static final int PORTEE_PROPRE_ECRITURE = 200;
    public static final int PORTEE_PROPRE_SUPPRESSION = 300;
    public static final int PORTEE_PROPRE_VALIDATION = 400;

    // ========== Portées Opérationnelles (Toutes agences) ==========
    public static final int PORTEE_TOUTES_LECTURE = 1000;
    public static final int PORTEE_TOUTES_ECRITURE = 2000;
    public static final int PORTEE_TOUTES_SUPPRESSION = 3000;
    public static final int PORTEE_TOUTES_VALIDATION = 4000;

    // ========== Codes d'Actions ==========
    public static final int ACTION_OT = 10;                // Ordre de transit, Contrat de transport, Marchandise, Dossier
    public static final int ACTION_SOUS_DOSSIER = 20;      // Sous-dossier, Inter-agence
    public static final int ACTION_TRAITEMENT = 30;         // Traitement, Embarquement / Débarquement
    public static final int ACTION_COMPTABILITE = 40;       // Comptabilité
    public static final int ACTION_GESTION_ROLES = 50;      // Gestion des rôles et autorisations

    private AutorisationHelper() {
        // Classe utilitaire — pas d'instanciation
    }

    /**
     * Vérifie si le niveau d'autorisation de l'utilisateur est suffisant
     * pour exécuter l'opération demandée.
     *
     * @param niveauUtilisateur Le niveau d'autorisation de l'utilisateur
     * @param portee            La portée opérationnelle requise (ex: PORTEE_PROPRE_ECRITURE)
     * @param codeAction        Le code d'action requis (ex: ACTION_OT)
     * @return true si l'utilisateur a la permission
     */
    public static boolean verifierPermission(int niveauUtilisateur, int portee, int codeAction) {
        return niveauUtilisateur >= (portee + codeAction);
    }

    /**
     * Vérifie la permission et lève une exception si elle est insuffisante.
     *
     * @throws SecurityException si l'utilisateur n'a pas la permission
     */
    public static void exigerPermission(int niveauUtilisateur, int portee, int codeAction) {
        if (!verifierPermission(niveauUtilisateur, portee, codeAction)) {
            throw new SecurityException(
                    "Permission insuffisante. Niveau requis: " + (portee + codeAction)
                            + ", niveau actuel: " + niveauUtilisateur
            );
        }
    }
}

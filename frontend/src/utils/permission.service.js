/**
 * Service de gestion des permissions côté frontend.
 * Décode le JWT et vérifie les droits d'accès selon la formule (Portée + Code Action).
 */

// ========== Portées Opérationnelles (Propre agence) ==========
export const PORTEE_PROPRE_LECTURE = 100
export const PORTEE_PROPRE_ECRITURE = 200
export const PORTEE_PROPRE_SUPPRESSION = 300
export const PORTEE_PROPRE_VALIDATION = 400

// ========== Portées Opérationnelles (Toutes agences) ==========
export const PORTEE_TOUTES_LECTURE = 1000
export const PORTEE_TOUTES_ECRITURE = 2000
export const PORTEE_TOUTES_SUPPRESSION = 3000
export const PORTEE_TOUTES_VALIDATION = 4000

// ========== Codes d'Actions ==========
export const ACTION_OT = 10               // Ordre de transit, Contrat de transport, Marchandise, Dossier
export const ACTION_SOUS_DOSSIER = 20     // Sous-dossier, Inter-agence
export const ACTION_TRAITEMENT = 30       // Traitement, Embarquement / Débarquement
export const ACTION_COMPTABILITE = 40     // Comptabilité
export const ACTION_GESTION_ROLES = 50    // Gestion des rôles et autorisations

class PermissionService {

    /**
     * Récupère le niveau d'autorisation depuis le localStorage
     * @returns {number}
     */
    getNiveauAutorisation() {
        const userInfo = localStorage.getItem('auximad_user_info')
        if (!userInfo) return 0
        try {
            const info = JSON.parse(userInfo)
            return info.niveauAutorisation || 0
        } catch {
            return 0
        }
    }

    /**
     * Récupère l'ID de l'agence de l'utilisateur connecté
     * @returns {number|null}
     */
    getIdAgence() {
        const userInfo = localStorage.getItem('auximad_user_info')
        if (!userInfo) return null
        try {
            const info = JSON.parse(userInfo)
            return info.idAgence || null
        } catch {
            return null
        }
    }

    /**
     * Vérifie si l'utilisateur possède la permission requise.
     * Formule : niveauAutorisation >= (portée + codeAction)
     *
     * @param {number} portee - La portée opérationnelle (ex: PORTEE_PROPRE_ECRITURE)
     * @param {number} codeAction - Le code d'action (ex: ACTION_OT)
     * @returns {boolean}
     */
    verifierPermission(portee, codeAction) {
        const niveau = this.getNiveauAutorisation()
        return niveau >= (portee + codeAction)
    }

    // ========== Raccourcis pour les opérations courantes sur les OT ==========

    peutLireOT() {
        return this.verifierPermission(PORTEE_PROPRE_LECTURE, ACTION_OT)
    }

    peutEcrireOT() {
        return this.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
    }

    peutSupprimerOT() {
        return this.verifierPermission(PORTEE_PROPRE_SUPPRESSION, ACTION_OT)
    }

    peutValiderOT() {
        return this.verifierPermission(PORTEE_PROPRE_VALIDATION, ACTION_OT)
    }

    // ========== Raccourcis multi-agences ==========

    peutLireToutesAgencesOT() {
        return this.verifierPermission(PORTEE_TOUTES_LECTURE, ACTION_OT)
    }

    peutSupprimerToutesAgencesOT() {
        return this.verifierPermission(PORTEE_TOUTES_SUPPRESSION, ACTION_OT)
    }
}

export default new PermissionService()

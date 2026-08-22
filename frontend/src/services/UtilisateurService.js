/**
 * Service Métier Utilisateur
 * Gère toute la logique métier liée aux utilisateurs
 * Fait le lien entre les modèles et le service API
 */
import Utilisateur from '../models/Utilisateur'
import authService from '../utils/auth.service'

class UtilisateurService {
    /**
     * Authentifie un utilisateur avec validation complète
     * @param {string} nomUtilisateur
     * @param {number} codeAgence
     * @param {string} motDePasse
     * @returns {Promise<Utilisateur>}
     */
    async authentifier(nomUtilisateur, codeAgence, motDePasse) {
        const utilisateur = new Utilisateur()

        // Valider les credentials
        const validation = utilisateur.validerConnexion(
            nomUtilisateur,
            codeAgence,
            motDePasse
        )

        if (!validation.valide) {
            throw new Error(validation.erreurs[0])
        }

        try {
            // Appeler le service d'authentification
            const response = await authService.login(
                nomUtilisateur,
                codeAgence,
                motDePasse
            )

            // Charger les données de réponse dans le modèle
            utilisateur.chargerDonnees(response)

            return utilisateur
        } catch (error) {
            throw this._traiterErreurAPI(error)
        }
    }

    /**
     * Récupère les informations de l'utilisateur connecté
     * @returns {Utilisateur|null}
     */
    obtenirUtilisateurConnecte() {
        const userInfo = authService.getUserInfo()

        if (!userInfo) {
            return null
        }

        const utilisateur = new Utilisateur()
        utilisateur.chargerDonnees(userInfo)

        return utilisateur
    }

    /**
     * Déconnecte l'utilisateur actuel
     */
    deconnecter() {
        authService.logout()
    }

    /**
     * Vérifie si un utilisateur est connecté
     * @returns {boolean}
     */
    estConnecte() {
        return authService.isLoggedIn()
    }

    /**
     * Traite les erreurs API et les convertit en messages compréhensibles
     * @private
     * @param {Error} error
     * @returns {Error}
     */
    _traiterErreurAPI(error) {
        let message = "Une erreur s'est produite lors de la connexion."

        if (error) {
            if (error.erreur) {
                message = error.erreur
            } else if (error.message) {
                message = error.message
            } else if (error.error) {
                message = error.error
            } else if (typeof error === 'string') {
                message = error
            }
        }

        return new Error(message)
    }

    /**
     * Formate les informations utilisateur pour l'affichage
     * @param {Utilisateur} utilisateur
     * @returns {object}
     */
    formaterPourAffichage(utilisateur) {
        return {
            nomComplet: utilisateur.getNomComplet(),
            nomUtilisateur: utilisateur.nomUtilisateur,
            email: utilisateur.email,
            role: utilisateur.role,
            agence: utilisateur.agence
        }
    }
}

export default new UtilisateurService()

/**
 * Modèle Utilisateur
 * Encapsule les données et la logique métier liées à l'utilisateur
 */
export class Utilisateur {
    constructor(data = {}) {
        this.id = data.id || null
        this.nomUtilisateur = data.nomUtilisateur || ''
        this.nom = data.nom || ''
        this.prenom = data.prenom || ''
        this.email = data.email || ''
        this.motDePasse = data.motDePasse || ''
        this.role = data.role || null
        this.agence = data.agence || null 
        this.agenceobj = data.agenceobj || null
        this.dateCreation = data.dateCreation || null
        this.dateModification = data.dateModification || null
    }

    /**
     * Récupère le nom complet de l'utilisateur (prenom + nom)
     * @returns {string} Nom complet formaté
     */
    getNomComplet() {
        return `${this.prenom} ${this.nom}`.trim()
    }

    /**
     * Valide les données essentielles pour la connexion
     * @param {string} nomUtilisateur - Nom d'utilisateur
     * @param {number} codeAgence - Code de l'agence
     * @param {string} motDePasse - Mot de passe
     * @returns {object} { valide: boolean, erreurs: string[] }
     */
    validerConnexion(nomUtilisateur, codeAgence, motDePasse) {
        const erreurs = []

        if (!nomUtilisateur || nomUtilisateur.trim().length === 0) {
            erreurs.push('Le nom d\'utilisateur est requis')
        } else if (nomUtilisateur.length < 3) {
            erreurs.push('Le nom d\'utilisateur doit contenir au moins 3 caractères')
        } else if (nomUtilisateur.length > 50) {
            erreurs.push('Le nom d\'utilisateur ne doit pas dépasser 50 caractères')
        }

        if (!codeAgence || codeAgence <= 0) {
            erreurs.push('Le code agence est requis et doit être valide')
        }

        if (!motDePasse || motDePasse.length === 0) {
            erreurs.push('Le mot de passe est requis')
        } else if (motDePasse.length < 6) {
            erreurs.push('Le mot de passe doit contenir au moins 6 caractères')
        }

        return {
            valide: erreurs.length === 0,
            erreurs
        }
    }

    /**
     * Valide les données de l'utilisateur
     * @returns {object} { valide: boolean, erreurs: string[] }
     */
    validerProfil() {
        const erreurs = []

        if (!this.nomUtilisateur || this.nomUtilisateur.trim().length === 0) {
            erreurs.push('Le nom d\'utilisateur est requis')
        }

        if (!this.nom || this.nom.trim().length === 0) {
            erreurs.push('Le nom est requis')
        }

        if (!this.prenom || this.prenom.trim().length === 0) {
            erreurs.push('Le prenom est requis')
        }

        if (!this.email || this.email.trim().length === 0) {
            erreurs.push('L\'email est requis')
        } else if (!this.estEmailValide()) {
            erreurs.push('L\'email n\'est pas valide')
        }

        if (!this.role) {
            erreurs.push('Le rôle est requis')
        }

        if (!this.agence) {
            erreurs.push('L\'agence est requise')
        }

        return {
            valide: erreurs.length === 0,
            erreurs
        }
    }

    /**
     * Valide le format d'un email
     * @returns {boolean}
     */
    estEmailValide() {
        const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        return regexEmail.test(this.email)
    }

    /**
     * Charge les données de l'utilisateur depuis un objet
     * @param {object} data - Données à charger
     */
    chargerDonnees(data) {
        console.log('Utilisateur:', data)
        console.log(data)
        if (data.id) this.id = data.id
        if (data.nomUtilisateur) this.nomUtilisateur = data.nomUtilisateur
        if (data.nom) this.nom = data.nom
        if (data.prenom) this.prenom = data.prenom
        if (data.email) this.email = data.email
        if (data.motDePasse) this.motDePasse = data.motDePasse
        if (data.role) this.role = data.role
        if (data.agence) this.agence = data.agence
        if (data.idAgence) this.idAgence = data.idAgence
        if (data.dateCreation) this.dateCreation = data.dateCreation
        if (data.dateModification) this.dateModification = data.dateModification
    }

    /**
     * Convertit l'utilisateur en objet JavaScript simple
     * @returns {object}
     */
    toJSON() {
        return {
            id: this.id,
            nomUtilisateur: this.nomUtilisateur,
            nom: this.nom,
            prenom: this.prenom,
            email: this.email,
            motDePasse: this.motDePasse,
            role: this.role,
            agence: this.agence,
            idAgence: this.idAgence,
            dateCreation: this.dateCreation,
            dateModification: this.dateModification
        }
    }

    /**
     * Réinitialise les données de l'utilisateur
     */
    reinitialiser() {
        this.id = null
        this.nomUtilisateur = ''
        this.nom = ''
        this.prenom = ''
        this.email = ''
        this.motDePasse = ''
        this.role = null
        this.agence = null
        this.dateCreation = null
        this.dateModification = null
    }
}

export default Utilisateur

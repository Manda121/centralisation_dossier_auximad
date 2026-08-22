/**
 * Modèle Client
 * Encapsule les données et la logique métier de validation d'un client
 */
export class Client {
    constructor(data = {}) {
        this.id = data.id || null
        this.nom = data.nom || ''
        this.prenom = data.prenom || ''
        this.email = data.email || ''
        this.telephone = data.telephone || ''
        this.adresse = data.adresse || ''
        this.ville = data.ville || ''
        this.codePostal = data.codePostal || ''
        this.pays = data.pays || null
        this.typeClient = data.typeClient || null
        this.nif = data.nif || ''
        this.stat = data.stat || ''
        this.fax = data.fax || ''
        this.dateCreation = data.dateCreation || null
    }

    /**
     * Récupère le nom complet du client
     * @returns {string}
     */
    getNomComplet() {
        if (!this.nom) return '—'
        return `${this.nom} ${this.prenom || ''}`.trim()
    }

    /**
     * Valide le modèle client avant envoi au serveur
     * @returns {{valide: boolean, erreurs: string[]}}
     */
    valider() {
        const erreurs = []

        if (!this.nom || this.nom.trim().length === 0) {
            erreurs.push('Le nom du client est requis.')
        }

        if (this.email && this.email.trim().length > 0 && !this.estEmailValide()) {
            erreurs.push("L'adresse email n'est pas valide.")
        }

        if (this.nif && this.nif.trim().length > 0 && this.nif.length !== 10 && this.nif.length !== 13) {
            erreurs.push('Le NIF doit comporter 10 ou 13 caractères.')
        }

        if (this.stat && this.stat.trim().length > 0 && this.stat.length !== 17) {
            erreurs.push('Le STAT doit comporter 17 caractères.')
        }

        return {
            valide: erreurs.length === 0,
            erreurs
        }
    }

    /**
     * Valide l'adresse email
     * @returns {boolean}
     */
    estEmailValide() {
        const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        return regexEmail.test(this.email)
    }

    /**
     * Charge les attributs d'un objet simple
     * @param {object} data 
     */
    chargerDonnees(data) {
        if (data.id) this.id = data.id
        if (data.nom) this.nom = data.nom
        if (data.prenom) this.prenom = data.prenom
        if (data.email) this.email = data.email
        if (data.telephone) this.telephone = data.telephone
        if (data.adresse) this.adresse = data.adresse
        if (data.ville) this.ville = data.ville
        if (data.codePostal) this.codePostal = data.codePostal
        if (data.pays) this.pays = data.pays
        if (data.typeClient) this.typeClient = data.typeClient
        if (data.nif) this.nif = data.nif
        if (data.stat) this.stat = data.stat
        if (data.fax) this.fax = data.fax
        if (data.dateCreation) this.dateCreation = data.dateCreation
    }

    /**
     * Sérialisation au format JSON attendu par le serveur
     * @returns {object}
     */
    toJSON() {
        return {
            id: this.id,
            nom: this.nom,
            prenom: this.prenom,
            email: this.email,
            telephone: this.telephone,
            adresse: this.adresse,
            ville: this.ville,
            codePostal: this.codePostal,
            pays: this.pays,
            typeClient: this.typeClient,
            nif: this.nif,
            stat: this.stat,
            fax: this.fax
        }
    }
}

export default Client

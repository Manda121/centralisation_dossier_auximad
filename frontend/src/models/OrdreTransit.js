/**
 * Modèle Ordre de Transit (OT)
 * Encapsule les données et validations d'un ordre de transit
 */
export class OrdreTransit {
    constructor(data = {}) {
        this.expedition = data.expedition || null
        this.id = data.id || null
        this.numeroOrdreTransit = data.numeroOrdreTransit || null
        this.agence = data.agence || null
        this.client = data.client || null
        this.typeProduction = data.typeProduction || null
        this.natureMarchandise = data.natureMarchandise || null
        this.numeroCommande = data.numeroCommande || ''
        this.utilisateur = data.utilisateur || null
        this.typeRegime = data.typeRegime || null
        this.destinationFinale = data.destinationFinale || ''
        this.expediteur = data.expediteur || null
        this.destinataire = data.destinataire || null
        this.bureauDedouanement = data.bureauDedouanement || null
        this.estAssure = data.estAssure !== undefined ? data.estAssure : false
        this.destinationFacture = data.destinationFacture || null
        this.statutOt = data.statutOt || null
        this.dateCreation = data.dateCreation || null
    }

    /**
     * Valide l'OT avant création
     * @returns {{valide: boolean, erreurs: string[]}}
     */
    valider() {
        const erreurs = []

        if (!this.client) {
            erreurs.push("Le client donneur d'ordre est requis.")
        }
        if (!this.typeProduction) {
            erreurs.push("Le type de production (import/export) est requis.")
        }
        if (!this.natureMarchandise) {
            erreurs.push("La nature de la marchandise est requise.")
        }
        if (!this.typeRegime) {
            erreurs.push("Le régime douanier est requis.")
        }
        if (!this.expediteur) {
            erreurs.push("L'expéditeur est requis.")
        }
        if (!this.destinataire) {
            erreurs.push("Le destinataire est requis.")
        }
        if (!this.bureauDedouanement) {
            erreurs.push("Le bureau de dédouanement est requis.")
        }

        return {
            valide: erreurs.length === 0,
            erreurs
        }
    }

    /**
     * Formatage pour le payload JSON du backend
     */
    toJSON() {
        return {
            expedition: this.expedition,
            id: this.id,
            numeroOrdreTransit: this.numeroOrdreTransit,
            client: this.client ? { id: this.client.id } : null,
            typeProduction: this.typeProduction ? { id: this.typeProduction.id } : null,
            natureMarchandise: this.natureMarchandise ? { id: this.natureMarchandise.id } : null,
            numeroCommande: this.numeroCommande,
            typeRegime: this.typeRegime ? { id: this.typeRegime.id } : null,
            destinationFinale: this.destinationFinale,
            expediteur: this.expediteur ? { id: this.expediteur.id } : null,
            destinataire: this.destinataire ? { id: this.destinataire.id } : null,
            bureauDedouanement: this.bureauDedouanement ? { id: this.bureauDedouanement.id } : null,
            estAssure: this.estAssure,
            destinationFacture: this.destinationFacture ? { id: this.destinationFacture.id } : null,
            statutOt: this.statutOt
        }
    }
}

export default OrdreTransit

/**
 * Modèle Marchandise
 * Encapsule les données et la logique de validation d'une marchandise liée à un OT
 */
export class Marchandise {
    constructor(data = {}) {
        this.id = data.id || null
        this.ordreTransit = data.ordreTransit || null
        this.uniteMesure = data.uniteMesure || null
        this.marque = data.marque || ''
        this.modele = data.modele || ''
        this.description = data.description || ''
        this.poids = data.poids || 0
        this.volume = data.volume || 0
        this.quantite = data.quantite || 1
    }

    /**
     * Valide le conteneur/marchandise
     * @returns {{valide: boolean, erreurs: string[]}}
     */
    valider() {
        const erreurs = []

        if (!this.description || this.description.trim().length === 0) {
            erreurs.push('La description de la marchandise est requise.')
        }
        if (this.poids < 0) {
            erreurs.push('Le poids ne peut pas être négatif.')
        }
        if (this.volume < 0) {
            erreurs.push('Le volume ne peut pas être négatif.')
        }
        if (this.quantite <= 0) {
            erreurs.push('La quantité doit être supérieure à zéro.')
        }
        if (!this.uniteMesure) {
            erreurs.push('L\'unité de mesure est requise.')
        }

        return {
            valide: erreurs.length === 0,
            erreurs
        }
    }

    /**
     * Sérialisation au format attendu par le serveur
     */
    toJSON() {
        return {
            id: this.id,
            ordreTransit: this.ordreTransit ? { id: this.ordreTransit.id } : null,
            uniteMesure: this.uniteMesure ? { id: this.uniteMesure.id } : null,
            marque: this.marque,
            modele: this.modele,
            description: this.description,
            poids: this.poids,
            volume: this.volume,
            quantite: this.quantite
        }
    }
}

export default Marchandise

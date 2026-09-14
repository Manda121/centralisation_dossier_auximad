/**
 * Modèle Dossier
 */
export class Dossier {
    constructor(data = {}) {
        this.id = data.id || null
        this.numeroDossier = data.numeroDossier || null
        this.agence = data.agence || null
        this.utilisateur = data.utilisateur || null
        this.client = data.client || null
        this.typeProduction = data.typeProduction || null
        this.ordreTransit = data.ordreTransit || null
        this.estGroupage = data.estGroupage !== undefined ? data.estGroupage : false
        this.phaseDossier = data.phaseDossier || null
        this.statutDossier = data.statutDossier || null
        this.echeance = data.echeance || null
        this.pourCompte = data.pourCompte || ''
        this.paysOrigine = data.paysOrigine || null
        this.paysDestination = data.paysDestination || null
        this.estPrepaye = data.estPrepaye !== undefined ? data.estPrepaye : false
        this.estProvisionne = data.estProvisionne !== undefined ? data.estProvisionne : false
        this.provisionDemandee = data.provisionDemandee || 0
        this.lieuPaiementFret = data.lieuPaiementFret || ''
        this.depart = data.depart || ''
        this.dateLivraison = data.dateLivraison || null
        this.adresseLivraison = data.adresseLivraison || ''
        this.inconterme = data.inconterme || null
        this.dateCreation = data.dateCreation || null
    }

    valider() {
        const erreurs = []
        // if (!this.agence) erreurs.push('L\'agence est requise.')
        if (!this.client) erreurs.push('Le client est requis.')
        if (!this.typeProduction) erreurs.push('Le type de production est requis.')
        if (!this.phaseDossier) erreurs.push('La phase du dossier est requise.')
        return { valide: erreurs.length === 0, erreurs }
    }

    toJSON() {
        return {
            id: this.id,
            client: this.client ? { id: this.client.id } : null,
            agence: this.agence ? { id: this.agence.id } : null,           // ✅ Ajouté
            utilisateur: this.utilisateur ? { id: this.utilisateur.id } : null,
            typeProduction: this.typeProduction ? { id: this.typeProduction.id } : null,
            ordreTransit: this.ordreTransit ? { id: this.ordreTransit.id } : null,
            estGroupage: this.estGroupage,
            phaseDossier: this.phaseDossier ? { id: this.phaseDossier.id } : null,
            statutDossier: this.statutDossier ? { id: this.statutDossier.id } : null,
            echeance: this.echeance,
            pourCompte: this.pourCompte,
            paysOrigine: this.paysOrigine ? { id: this.paysOrigine.id } : null,
            paysDestination: this.paysDestination ? { id: this.paysDestination.id } : null,
            estPrepaye: this.estPrepaye,
            estProvisionne: this.estProvisionne,
            provisionDemandee: this.provisionDemandee,
            lieuPaiementFret: this.lieuPaiementFret,
            depart: this.depart,
            dateLivraison: this.dateLivraison,
            adresseLivraison: this.adresseLivraison,
            inconterme: this.inconterme ? { id: this.inconterme.id } : null
        }
    }
}

export default Dossier

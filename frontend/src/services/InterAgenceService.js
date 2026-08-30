import axios from 'axios'

const API_URL = 'http://localhost:8080/api/inter-agences'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class InterAgenceService {

    /**
     * Crée un sous-dossier lié à un dossier mère.
     * @param {number} idDossierMere
     * @param {{ idAgence: number, motif: string, idTypeProduction?: number }} data
     */
    async creerSousDossier(idDossierMere, data) {
        const response = await axios.post(
            `${API_URL}/sous-dossiers/${idDossierMere}`,
            data,
            { headers: authHeader() }
        )
        return response.data
    }

    /**
     * Liste tous les sous-dossiers d'un dossier mère.
     * @param {number} idDossierMere
     */
    async getSousDossiers(idDossierMere) {
        const response = await axios.get(
            `${API_URL}/sous-dossiers/${idDossierMere}`,
            { headers: authHeader() }
        )
        return response.data
    }

    /**
     * Retourne le lien interagence d'un sous-dossier (et donc son dossier mère).
     * @param {number} idSousDossier
     */
    async getDossierMere(idSousDossier) {
        const response = await axios.get(
            `${API_URL}/dossier-mere/${idSousDossier}`,
            { headers: authHeader() }
        )
        return response.data
    }
}

export default new InterAgenceService()

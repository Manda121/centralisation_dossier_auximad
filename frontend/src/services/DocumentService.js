import axios from 'axios'

const API_URL = 'http://localhost:8080/api/documents'
const DOSSIER_API = 'http://localhost:8080/api/dossiers'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class DocumentService {
    /** Récupérer les types de documents (filtrables par type de production) */
    async getTypeDocuments(typeProductionId = null) {
        const params = typeProductionId ? `?typeProductionId=${typeProductionId}` : ''
        const res = await axios.get(`${API_URL}/types${params}`, { headers: authHeader() })
        return res.data
    }

    /** Récupérer les documents actifs d'un dossier */
    async getDocumentsByDossier(dossierId) {
        const res = await axios.get(`${API_URL}?dossierId=${dossierId}`, { headers: authHeader() })
        return res.data
    }

    /** Vérifier si tous les docs obligatoires sont remis */
    async verifierDocuments(dossierId) {
        const res = await axios.get(`${API_URL}/verification/${dossierId}`, { headers: authHeader() })
        return res.data
    }

    /**
     * Traitement batch (cocher/décocher + uploads)
     * @param {number} dossierId
     * @param {number[]} typeIdsAjoutes
     * @param {number[]} typeIdsRetires
     * @param {Object} fichiersParTypeId - { [typeId]: File }
     */
    async processerBatch(dossierId, typeIdsAjoutes, typeIdsRetires, fichiersParTypeId = {}) {
        const formData = new FormData()
        formData.append('dossierId', dossierId)

        if (typeIdsAjoutes && typeIdsAjoutes.length > 0) {
            typeIdsAjoutes.forEach(id => formData.append('typeIdsAjoutes', id))
        }
        if (typeIdsRetires && typeIdsRetires.length > 0) {
            typeIdsRetires.forEach(id => formData.append('typeIdsRetires', id))
        }

        // Fichiers optionnels
        const typeIdsFichiers = Object.keys(fichiersParTypeId).map(Number)
        typeIdsFichiers.forEach(typeId => {
            formData.append('fichiers', fichiersParTypeId[typeId])
            formData.append('typeIdsFichiers', typeId)
        })

        const res = await axios.post(`${API_URL}/batch`, formData, {
            headers: {
                ...authHeader(),
                'Content-Type': 'multipart/form-data'
            }
        })
        return res.data
    }

    /** URL pour visualiser un fichier de document */
    getFichierUrl(documentId) {
        const token = localStorage.getItem('auximad_token')
        return `${API_URL}/${documentId}/fichier?token=${token}`
    }

    /** Récupérer les dossiers sans documents complets */
    async getDossiersSansDocuments() {
        const res = await axios.get(`${DOSSIER_API}/sans-documents`, { headers: authHeader() })
        return res.data
    }

    /** Récupérer les dossiers avec remise urgente */
    async getDossiersRemiseUrgente(jours = 7) {
        const res = await axios.get(`${DOSSIER_API}/remise-urgente?jours=${jours}`, { headers: authHeader() })
        return res.data
    }
}

export default new DocumentService()

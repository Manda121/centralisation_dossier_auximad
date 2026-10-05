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

    /** Lister les types de documents obligatoires (table document_obligatoire_mvt) non encore remis */
    async getDocsObligatoiresManquants(dossierId) {
        const res = await axios.get(`${API_URL}/obligatoires-manquants/${dossierId}`, { headers: authHeader() })
        return res.data
    }

    /**
     * Traitement batch (cocher/décocher + uploads)
     * @param {number} dossierId
     * @param {number[]} typeIdsAjoutes
     * @param {number[]} typeIdsRetires
     * @param {Object} fichiersParTypeId - { [typeId]: File }
     * @param {string|null} dateRemise - date de remise commune (ISO) stockée dans la table remise
     */
    async processerBatch(dossierId, typeIdsAjoutes, typeIdsRetires, fichiersParTypeId = {}, dateRemise = null) {
        const formData = new FormData()
        formData.append('dossierId', dossierId)

        if (typeIdsAjoutes && typeIdsAjoutes.length > 0) {
            typeIdsAjoutes.forEach(id => formData.append('typeIdsAjoutes', id))
        }
        if (typeIdsRetires && typeIdsRetires.length > 0) {
            typeIdsRetires.forEach(id => formData.append('typeIdsRetires', id))
        }
        if (dateRemise) {
            formData.append('dateRemise', dateRemise)
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

    /** Assigner un document à un dossier avec sa date de remise (table remise) */
    async assignerDocument(dossierId, typeDocumentId, dateRemise = null) {
        const params = new URLSearchParams({ dossierId, typeDocumentId })
        if (dateRemise) params.append('dateRemise', dateRemise)
        const res = await axios.post(`${API_URL}/assigner?${params.toString()}`, {}, { headers: authHeader() })
        return res.data
    }

    /** Assigner plusieurs documents avec une date de remise commune (table remise) */
    async assignerBatch(dossierId, typeDocumentIds, dateRemise = null) {
        const params = new URLSearchParams({ dossierId })
        typeDocumentIds.forEach(id => params.append('typeDocumentIds', id))
        if (dateRemise) params.append('dateRemise', dateRemise)
        const res = await axios.post(`${API_URL}/assigner-batch?${params.toString()}`, {}, { headers: authHeader() })
        return res.data
    }

    /** Mettre à jour la date de remise d'un document (table remise) */
    async updateDateRemise(dossierId, typeDocumentId, dateRemise) {
        const params = new URLSearchParams({ dossierId, typeDocumentId })
        if (dateRemise) params.append('dateRemise', dateRemise)
        const res = await axios.put(`${API_URL}/remise?${params.toString()}`, {}, { headers: authHeader() })
        return res.data
    }

    /** Récupérer les remises d'un dossier (table remise) */
    async getRemises(dossierId) {
        const res = await axios.get(`${API_URL}/remises?dossierId=${dossierId}`, { headers: authHeader() })
        return res.data
    }

    /** Ajouter/mettre à jour un document avec son fichier (crée la remise si absente) */
    async saveDocument(dossierId, typeDocumentId, fichier) {
        const formData = new FormData()
        formData.append('dossierId', dossierId)
        formData.append('typeDocumentId', typeDocumentId)
        if (fichier) formData.append('fichier', fichier)
        const res = await axios.post(API_URL, formData, {
            headers: {
                ...authHeader(),
                'Content-Type': 'multipart/form-data'
            }
        })
        return res.data
    }

    /** Retirer (désactiver) un document d'un dossier et supprimer sa remise */
    async retirerDocument(dossierId, typeDocumentId) {
        const res = await axios.delete(`${API_URL}/retirer?dossierId=${dossierId}&typeDocumentId=${typeDocumentId}`, { headers: authHeader() })
        return res.data
    }

    /** Récupérer les remises urgentes (dates de remise dans les N prochains jours) */
    async getRemisesUrgentes(jours = 7, agenceId = null) {
        let url = `${API_URL}/remises/urgentes?jours=${jours}`
        if (agenceId) url += `&agenceId=${agenceId}`
        const res = await axios.get(url, { headers: authHeader() })
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
}

export default new DocumentService()

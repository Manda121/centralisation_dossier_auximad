import axios from 'axios'

const API_URL = 'http://localhost:8080/api/conteneurs'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class ConteneurService {
    async getTypesContainers() {
        const response = await axios.get(`${API_URL}/types`, { headers: authHeader() })
        return response.data
    }

    async getConteneursParDossier(dossierId) {
        const response = await axios.get(`${API_URL}/dossier/${dossierId}`, { headers: authHeader() })
        return response.data
    }

    async getTousConteneurs() {
        const response = await axios.get(API_URL, { headers: authHeader() })
        return response.data
    }

    async assignerConteneur(conteneurData) {
        const response = await axios.post(API_URL, conteneurData, { headers: authHeader() })
        return response.data
    }

    async modifierConteneur(id, conteneurData) {
        const response = await axios.put(`${API_URL}/${id}`, conteneurData, { headers: authHeader() })
        return response.data
    }

    async supprimerConteneur(id, dossierId = null) {
        const url = dossierId ? `${API_URL}/${id}?dossierId=${dossierId}` : `${API_URL}/${id}`
        const response = await axios.delete(url, { headers: authHeader() })
        return response.data
    }

    /**
     * Vérifie si un dossier est de transport maritime ou éligible aux conteneurs
     */
    estEligibleConteneur(dossier) {
        if (!dossier) return false

        // Mode transport direct du dossier (via typeProduction)
        const mtNom = dossier.typeProduction?.modeTransport?.nom
        if (mtNom) {
            const lower = mtNom.toLowerCase()
            if (lower.includes('maritime') || lower === 'combine') return true
        }

        const tpNom = dossier.typeProduction?.nom
        if (tpNom && tpNom.toLowerCase().includes('maritime')) return true

        // Mode transport via Ordre de Transit
        const otMtNom = dossier.ordreTransit?.typeProduction?.modeTransport?.nom
        if (otMtNom) {
            const lower = otMtNom.toLowerCase()
            if (lower.includes('maritime') || lower === 'combine') return true
        }

        return false
    }
}

export default new ConteneurService()

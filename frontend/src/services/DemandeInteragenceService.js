import axios from 'axios'

const API_URL = 'http://localhost:8080/api/demandes-interagence'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class DemandeInteragenceService {

    async creerDemande(payload) {
        const response = await axios.post(API_URL, payload, { headers: authHeader() })
        return response.data
    }

    async searchDemandes(page, size, sortBy, sortDir, filters = {}) {
        const params = new URLSearchParams()
        params.append('page', page)
        params.append('size', size)
        if (sortBy) params.append('sortBy', sortBy)
        if (sortDir) params.append('sortDir', sortDir)

        Object.keys(filters).forEach(key => {
            const v = filters[key]
            if (v !== '' && v !== null && v !== undefined) {
                params.append(key, v)
            }
        })

        const response = await axios.get(`${API_URL}?${params.toString()}`, { headers: authHeader() })
        return response.data
    }

    async getDemandeById(id) {
        const response = await axios.get(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }

    async repondreDemande(id, payload) {
        const response = await axios.post(`${API_URL}/${id}/repondre`, payload, { headers: authHeader() })
        return response.data
    }

    async getNonVuesCount() {
        try {
            const response = await axios.get(`${API_URL}/non-vues-count`, { headers: authHeader() })
            return response.data?.count || 0
        } catch {
            return 0
        }
    }

    async getDemandesParDossier(dossierId) {
        const response = await axios.get(`${API_URL}/dossier/${dossierId}`, { headers: authHeader() })
        return response.data
    }
}

export default new DemandeInteragenceService()

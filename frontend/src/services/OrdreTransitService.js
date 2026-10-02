import { requeteSuppression } from './SuppressionService'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api/ordres-transit'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class OrdreTransitService {
    async searchOrdreTransits(page, size, sortBy, sortDir, filters = {}) {
        const params = new URLSearchParams()
        params.append('page', page)
        params.append('size', size)
        if (sortBy) params.append('sortBy', sortBy)
        if (sortDir) params.append('sortDir', sortDir)
        Object.keys(filters).forEach(key => {
            if (filters[key] !== '' && filters[key] !== null && filters[key] !== undefined) {
                if ((key === 'dateMin' || key === 'dateMax') && filters[key]) {
                    params.append(key, new Date(filters[key]).toISOString())
                } else {
                    params.append(key, filters[key])
                }
            }
        })
        const response = await axios.get(`${API_URL}?${params.toString()}`, { headers: authHeader() })
        return response.data
    }

    async getOrdreTransitById(id) {
        const response = await axios.get(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }

    /** Suggestions d'OT pour l'autocomplétion (numéro d'OT, n° de commande ou client) */
    async suggestions(q, limite = 8) {
        const response = await axios.get(`${API_URL}/suggestions`, {
            params: { q, limite },
            headers: authHeader()
        })
        return response.data
    }

    async createOrdreTransit(otData) {
        const response = await axios.post(API_URL, otData, { headers: authHeader() })
        return response.data
    }

    async validateOrdreTransit(id) {
        const response = await axios.put(`${API_URL}/${id}/valider`, {}, { headers: authHeader() })
        return response.data
    }

    async deleteOrdreTransit(id) {
        const response = await requeteSuppression(axios, { method: 'delete', url: `${API_URL}/${id}`, headers: authHeader() })
        return response.data
    }

    async getContratByOtId(otId) {
        const response = await axios.get(`${API_URL}/${otId}/contrat`, { headers: authHeader() })
        return response.data
    }

    async saveContrat(otId, contratData) {
        const response = await axios.post(`${API_URL}/${otId}/contrat`, contratData, { headers: authHeader() })
        return response.data
    }

    async getInstructionsByOtId(otId) {
        const response = await axios.get(`${API_URL}/${otId}/instructions`, { headers: authHeader() })
        return response.data
    }

    async addInstruction(otId, instructionText) {
        const response = await axios.post(`${API_URL}/${otId}/instructions`, { instruction: instructionText }, { headers: authHeader() })
        return response.data
    }

    async getCompagnies(modeId) {
        const url = modeId ? `${API_URL}/compagnies/${modeId}` : `${API_URL}/compagnies`
        const response = await axios.get(url, { headers: authHeader() })
        return response.data
    }

    async getNaviresByCompagnie(compagnieId) {
        const response = await axios.get(`${API_URL}/navires/${compagnieId}`, { headers: authHeader() })
        return response.data
    }

    async getAllTypeProductions() {
        const response = await axios.get(`${API_URL}/types-production`, { headers: authHeader() })
        return response.data
    }

    async getAllTypeRegimes() {
        const response = await axios.get(`${API_URL}/types-regime`, { headers: authHeader() })
        return response.data
    }

    async getAllBureaux() {
        const response = await axios.get(`${API_URL}/bureaux`, { headers: authHeader() })
        return response.data
    }
}

export default new OrdreTransitService()

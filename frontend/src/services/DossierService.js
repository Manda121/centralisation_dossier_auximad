import { requeteSuppression } from './SuppressionService'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api/dossiers'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class DossierService {
    async searchDossiers(page, size, sortBy, sortDir, filters = {}) {
        const params = new URLSearchParams()
        params.append('page', page)
        params.append('size', size)
        if (sortBy) params.append('sortBy', sortBy)
        if (sortDir) params.append('sortDir', sortDir)
        Object.keys(filters).forEach(key => {
            const v = filters[key]
            if (v !== '' && v !== null && v !== undefined) {
                if ((key === 'dateMin' || key === 'dateMax') && v) {
                    params.append(key, `${v}T${key === 'dateMax' ? '23:59:59.999' : '00:00:00'}`)
                } else {
                    params.append(key, v)
                }
            }
        })
        const response = await axios.get(`${API_URL}?${params.toString()}`, { headers: authHeader() })
        return response.data
    }

    async getDossierById(id) {
        const response = await axios.get(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }

    async createDossier(data) {
        const response = await axios.post(API_URL, data, { headers: authHeader() })
        return response.data
    }

    async updateDossier(id, data) {
        const response = await axios.put(`${API_URL}/${id}`, data, { headers: authHeader() })
        return response.data
    }

    async deleteDossier(id) {
        const response = await requeteSuppression(axios, { method: 'delete', url: `${API_URL}/${id}`, headers: authHeader() })
        return response.data
    }

    async getAllPhases(modeTransportId) {
        const url = modeTransportId
            ? `${API_URL}/phases?modeTransportId=${modeTransportId}`
            : `${API_URL}/phases`
        const response = await axios.get(url, { headers: authHeader() })
        return response.data
    }

    async getAllStatuts() {
        const response = await axios.get(`${API_URL}/statuts`, { headers: authHeader() })
        return response.data
    }

    async getAllIncontermes() {
        const response = await axios.get(`${API_URL}/incontermes`, { headers: authHeader() })
        return response.data
    }
}

export default new DossierService()

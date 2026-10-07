import axios from 'axios'

const API_URL = 'http://localhost:8080/api/mouvements-dossier'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class MvtDossierService {
    async searchMouvements(page = 0, size = 15, sortBy = 'dateMvt', sortDir = 'desc', filters = {}) {
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

    async getOperationsStatusForDossier(dossierId) {
        const response = await axios.get(`${API_URL}/dossier/${dossierId}/operations-status`, { headers: authHeader() })
        return response.data
    }

    async createMouvement(formData) {
        const response = await axios.post(API_URL, formData, {
            headers: {
                ...authHeader(),
                'Content-Type': 'multipart/form-data'
            }
        })
        return response.data
    }

    async annulerMouvement(id, motif = '') {
        const response = await axios.post(`${API_URL}/${id}/annuler`, { motif }, { headers: authHeader() })
        return response.data
    }
}

export default new MvtDossierService()

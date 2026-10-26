import axios from 'axios'

const API_URL = 'http://localhost:8080/api/dashboard'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class DashboardService {
    /** Toutes les stats en un seul appel */
    async getDashboardComplet() {
        const response = await axios.get(API_URL, { headers: authHeader() })
        return response.data
    }

    /** Stats globales uniquement */
    async getStatsGlobales() {
        const response = await axios.get(`${API_URL}/global`, { headers: authHeader() })
        return response.data
    }

    /** Stats d'un module spécifique */
    async getStatsModule(module) {
        const response = await axios.get(`${API_URL}/module/${module}`, { headers: authHeader() })
        return response.data
    }

    /** Activité récente */
    async getActiviteRecente() {
        const response = await axios.get(`${API_URL}/activite`, { headers: authHeader() })
        return response.data
    }
}

export default new DashboardService()

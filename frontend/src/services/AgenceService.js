import axios from 'axios'

const API_URL = 'http://localhost:8080/api/agences'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class AgenceService {
    async getAllAgences() {
        const response = await axios.get(API_URL, { headers: authHeader() })
        return response.data
    }

    async getAgenceById(id) {
        const response = await axios.get(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }
}

export default new AgenceService()

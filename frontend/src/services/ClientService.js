import axios from 'axios'

const API_URL = 'http://localhost:8080/api/clients'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class ClientService {
    async searchClients(page, size, sortBy, sortDir, filters = {}) {
        const params = new URLSearchParams()
        params.append('page', page)
        params.append('size', size)
        if (sortBy) params.append('sortBy', sortBy)
        if (sortDir) params.append('sortDir', sortDir)
        Object.keys(filters).forEach(key => {
            if (filters[key] !== '' && filters[key] !== null && filters[key] !== undefined) {
                params.append(key, filters[key])
            }
        })
        const response = await axios.get(`${API_URL}?${params.toString()}`, { headers: authHeader() })
        return response.data
    }

    async getClientById(id) {
        const response = await axios.get(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }

    async createClient(clientData) {
        const response = await axios.post(API_URL, clientData, { headers: authHeader() })
        return response.data
    }

    async updateClient(id, clientData) {
        const response = await axios.put(`${API_URL}/${id}`, clientData, { headers: authHeader() })
        return response.data
    }

    async deleteClient(id) {
        const response = await axios.delete(`${API_URL}/${id}`, { headers: authHeader() })
        return response.data
    }

    async getAllTypeClients() {
        const response = await axios.get(`${API_URL}/types`, { headers: authHeader() })
        return response.data
    }

    async getAllPays() {
        const response = await axios.get(`${API_URL}/pays`, { headers: authHeader() })
        return response.data
    }
}

export default new ClientService()

import axios from 'axios'

const API_URL = 'http://localhost:8080/api/declarations'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class DeclarationService {
    async getTypeDeclarations() {
        const response = await axios.get(`${API_URL}/types`, { headers: authHeader() })
        return response.data
    }

    async getByDossierId(dossierId) {
        try {
            const response = await axios.get(`${API_URL}/dossier/${dossierId}`, { headers: authHeader() })
            return response.data
        } catch (error) {
            if (error.response && error.response.status === 404) {
                return null
            }
            throw error
        }
    }

    async saveDeclaration(formData) {
        const response = await axios.post(API_URL, formData, {
            headers: {
                ...authHeader(),
                'Content-Type': 'multipart/form-data'
            }
        })
        return response.data
    }
}

export default new DeclarationService()

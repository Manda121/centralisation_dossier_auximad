import { requeteSuppression } from './SuppressionService'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api/marchandises'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class MarchandiseService {
    async getMarchandisesByOtId(otId) {
        const response = await axios.get(`${API_URL}?otId=${otId}`, { headers: authHeader() })
        return response.data
    }

    async saveMarchandise(marchandiseData) {
        const response = await axios.post(API_URL, marchandiseData, { headers: authHeader() })
        return response.data
    }

    async deleteMarchandise(id) {
        const response = await requeteSuppression(axios, { method: 'delete', url: `${API_URL}/${id}`, headers: authHeader() })
        return response.data
    }

    async getUniteMesures() {
        const response = await axios.get(`${API_URL}/unites`, { headers: authHeader() })
        return response.data
    }

    async getNatureMarchandises() {
        const response = await axios.get(`${API_URL}/natures`, { headers: authHeader() })
        return response.data
    }
}

export default new MarchandiseService()

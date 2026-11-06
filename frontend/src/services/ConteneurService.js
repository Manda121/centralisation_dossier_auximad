import { requeteSuppression } from './SuppressionService'
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
        const response = await requeteSuppression(axios, { method: 'delete', url: url, headers: authHeader() })
        return response.data
    }

    /**
     * L'assignation d'un conteneur est réservée au transport maritime.
     */
    estEligibleConteneur(dossier) {
        if (!dossier) return false

        // Mode transport direct du dossier (via typeProduction)
        const mtNom = dossier.typeProduction?.modeTransport?.nom
        if (mtNom?.trim()) {
            return mtNom.trim().toLowerCase() === 'maritime'
        }

        // Mode transport via Ordre de Transit
        const otMtNom = dossier.ordreTransit?.typeProduction?.modeTransport?.nom
        return otMtNom?.trim().toLowerCase() === 'maritime'
    }
}

export default new ConteneurService()

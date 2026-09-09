import axios from 'axios'

const API_URL = 'http://localhost:8080/api/historiques'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

class HistoriqueService {
    /**
     * Recherche multi-critères paginée de l'historique.
     * filters : { query, utilisateurId, actionId, adresseIp, hostname, logiciel,
     *             methodeHttp, statutHttp, dateMin, dateMax }
     */
    async searchHistoriques(page, size, sortBy, sortDir, filters = {}) {
        const params = new URLSearchParams()
        params.append('page', page)
        params.append('size', size)
        if (sortBy) params.append('sortBy', sortBy)
        if (sortDir) params.append('sortDir', sortDir)
        Object.keys(filters).forEach(key => {
            const v = filters[key]
            if (v !== '' && v !== null && v !== undefined) {
                if ((key === 'dateMin' || key === 'dateMax') && v) {
                    // Le backend attend du LocalDateTime ISO (sans Z)
                    const d = new Date(v)
                    if (key === 'dateMax') {
                        // Inclure toute la journée de fin (23:59:59)
                        d.setHours(23, 59, 59, 999)
                    }
                    params.append(key, d.toISOString().replace('Z', ''))
                } else {
                    params.append(key, v)
                }
            }
        })
        const response = await axios.get(`${API_URL}?${params.toString()}`, { headers: authHeader() })
        return response.data
    }

    /** Liste des types d'actions (pour le filtre). */
    async getActions() {
        const response = await axios.get(`${API_URL}/actions`, { headers: authHeader() })
        return response.data
    }

    /** Valeurs distinctes (IP, hostname, logiciel) pour les filtres. */
    async getFiltres() {
        const response = await axios.get(`${API_URL}/filtres`, { headers: authHeader() })
        return response.data
    }

    /** Liste légère des utilisateurs (pour le filtre). */
    async getUtilisateurs() {
        const response = await axios.get(`${API_URL}/utilisateurs`, { headers: authHeader() })
        return response.data
    }

    /** Notifie le serveur de la déconnexion (audit côté serveur). */
    async notifierDeconnexion() {
        try {
            await axios.post(`${API_URL.replace('/historiques', '')}/auth/logout`, {}, { headers: authHeader() })
        } catch (e) {
            // Silencieux : la déconnexion locale doit toujours aboutir
        }
    }
}

export default new HistoriqueService()

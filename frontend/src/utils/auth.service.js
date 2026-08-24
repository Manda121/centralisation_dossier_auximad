import axios from 'axios'

const API_URL = 'http://localhost:8080/api'

// Instance Axios personnalisée
const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json'
    }
})

// Intercepteur pour ajouter le token à toutes les requêtes (sauf connexion/inscription)
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('auximad_token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

/**
 * Service d'authentification responsable de la logique de connexion et du stockage du token.
 */
class AuthService {
    async login(nomUtilisateur, codeAgence, motDePasse) {
        try {
            const response = await api.post('/auth/login', {
                nomUtilisateur,
                codeAgence: parseInt(codeAgence, 10),
                motDePasse
            })

            // Stocker les informations de connexion
            if (response.data.token) {
                localStorage.setItem('auximad_token', response.data.token)
                localStorage.setItem('auximad_user_info', JSON.stringify({
                    id: response.data.id,  // ✅ Ajouté pour stocker l'ID de l'utilisateur
                    nomUtilisateur: response.data.nomUtilisateur,
                    nomComplet: response.data.nomComplet,
                    nom: response.data.nom,
                    prenom: response.data.prenom,
                    role: response.data.role,
                    agence: response.data.agence,
                    codeAgence: response.data.codeAgence,
                    idAgence: response.data.idAgence,
                    niveauAutorisation: response.data.niveauAutorisation
                }))
            }
            return response.data
        } catch (error) {
            // Rejeter avec les détails de l'erreur pour la gestion côté composant
            if (error.response && error.response.data) {
                throw error.response.data
            }
            throw { erreur: "Erreur réseau de communication avec le serveur." }
        }
    }

    logout() {
        localStorage.removeItem('auximad_token')
        localStorage.removeItem('auximad_user_info')
    }

    getToken() {
        return localStorage.getItem('auximad_token')
    }

    isLoggedIn() {
        return !!this.getToken()
    }

    getUserInfo() {
        const info = localStorage.getItem('auximad_user_info')
        if (info) {
            try {
                return JSON.parse(info)
            } catch (e) {
                return null
            }
        }
        return null
    }
}

export default new AuthService()

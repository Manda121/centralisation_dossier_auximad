import axios from 'axios'

const API_URL = 'http://localhost:8080/api/chat'

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

/**
 * Service du chatbot Auximad.
 *
 * L'assistant est stateful côté serveur : la conversation est conservée en mémoire par
 * utilisateur. Les opérations qui écrivent dans la base ne sont jamais exécutées directement :
 * le backend suspend la boucle d'outils et renvoie une carte de confirmation, que l'interface
 * doit soumettre explicitement via `confirmer()`.
 */
class ChatService {
    /** Pose une question. */
    async envoyer(message, effacer = false) {
        const res = await axios.post(`${API_URL}/message`, { message, effacer }, { headers: authHeader() })
        return res.data
    }

    /** Valide (accepte = true) ou refuse une opération mise en attente. */
    async confirmer(id, accepte) {
        const res = await axios.post(`${API_URL}/confirmation`, { id, accepte }, { headers: authHeader() })
        return res.data
    }

    /** Questions proposées au démarrage de la conversation. */
    async getSuggestions() {
        const res = await axios.get(`${API_URL}/suggestions`, { headers: authHeader() })
        return res.data
    }

    /** Outils exposés à l'utilisateur, avec leur niveau requis et leur autorisation. */
    async getOutils() {
        const res = await axios.get(`${API_URL}/outils`, { headers: authHeader() })
        return res.data
    }

    /** État du service : disponibilité d'Ollama, modèle utilisé, nombre d'outils. */
    async getEtat() {
        const res = await axios.get(`${API_URL}/etat`, { headers: authHeader() })
        return res.data
    }

    /** Repart d'une conversation vierge. */
    async nouvelleConversation() {
        const res = await axios.delete(`${API_URL}/session`, { headers: authHeader() })
        return res.data
    }

    /**
     * Extrait un message d'erreur lisible à partir des réponses d'axios.
     */
    messageErreur(e) {
        const data = e?.response?.data
        if (typeof data === 'string' && data.trim()) return data
        if (data?.message) return data.message
        if (data?.avertissement) return data.avertissement
        if (e?.message) return e.message
        return "L'assistant est momentanément indisponible."
    }
}

export default new ChatService()
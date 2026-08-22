<template>
    <div class="card border-0 shadow-lg rounded-4 overflow-hidden glass-card" style="max-width: 440px; width: 100%;">
        <div class="card-body p-4 p-sm-5">

            <!-- Brand Header -->
            <div class="text-center mb-4">
                <h1 class="h2 fw-bold text-primary mb-1 tracking-tight">Auximad</h1>
                <p class="text-muted fs-6 mb-0">Centralisation de dossiers</p>
            </div>

            <!-- Error / Suspension Alert -->
            <div v-if="error" class="alert alert-danger d-flex align-items-start gap-2 mb-4 animate-fade-in shadow-sm"
                role="alert">
                <svg xmlns="http://www.w3.org/2001/svg" width="20" height="20" fill="currentColor"
                    class="bi bi-exclamation-triangle-fill flex-shrink-0 mt-1" viewBox="0 0 16 16">
                    <path
                        d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z" />
                </svg>
                <div class="small fw-medium">{{ error }}</div>
            </div>

            <!-- Login Form -->
            <form @submit.prevent="handleLogin">

                <!-- Nom d'utilisateur -->
                <div class="mb-3">
                    <label for="nomUtilisateur" class="form-label fw-medium text-secondary small">Nom
                        d'utilisateur</label>
                    <div class="input-group">
                        <span class="input-group-text bg-light text-muted border-end-0">
                            <svg xmlns="http://www.w3.org/2001/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-person-fill" viewBox="0 0 16 16">
                                <path d="M3 14s-1 0-1-1 1-4 6-4 6 3 6 4-1 1-1 1H3zm5-6a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
                            </svg>
                        </span>
                        <input id="nomUtilisateur" v-model="credentials.nomUtilisateur" type="text"
                            class="form-control bg-light border-start-0 ps-0" placeholder="Ex: jean.dupont" required
                            :disabled="loading" />
                    </div>
                </div>

                <!-- Code Agence -->
                <div class="mb-3">
                    <label for="codeAgence" class="form-label fw-medium text-secondary small">Code Agence</label>
                    <div class="input-group">
                        <span class="input-group-text bg-light text-muted border-end-0">
                            <svg xmlns="http://www.w3.org/2001/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-building-fill" viewBox="0 0 16 16">
                                <path
                                    d="M3 2a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v12h1a1 1 0 0 1 0 2H2a1 1 0 0 1 0-2h1V2zm2 2v2h2V4H5zm4 0v2h2V4H9zm-4 4v2h2V8H5zm4 0v2h2V8H9zm-4 4v2h2v-2H5zm4 0v2h2v-2H9z" />
                            </svg>
                        </span>
                        <input id="codeAgence" v-model="credentials.codeAgence" type="number"
                            class="form-control bg-light border-start-0 ps-0" placeholder="Code de votre agence"
                            required :disabled="loading" />
                    </div>
                </div>

                <!-- Mot de passe -->
                <div class="mb-4">
                    <label for="motDePasse" class="form-label fw-medium text-secondary small">Mot de passe</label>
                    <div class="input-group">
                        <span class="input-group-text bg-light text-muted border-end-0">
                            <svg xmlns="http://www.w3.org/2001/svg" width="16" height="16" fill="currentColor"
                                class="bi bi-lock-fill" viewBox="0 0 16 16">
                                <path
                                    d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z" />
                            </svg>
                        </span>
                        <input id="motDePasse" v-model="credentials.motDePasse" type="password"
                            class="form-control bg-light border-start-0 ps-0" placeholder="Votre mot de passe" required
                            :disabled="loading" />
                    </div>
                </div>

                <!-- Submit Button -->
                <button type="submit"
                    class="btn btn-primary w-100 py-2.5 fw-bold shadow-sm d-flex align-items-center justify-content-center gap-2"
                    :disabled="loading">
                    <span v-if="loading" class="spinner-border spinner-border-sm" role="status"
                        aria-hidden="true"></span>
                    <span>{{ loading ? 'Connexion en cours...' : 'Se connecter' }}</span>
                </button>

            </form>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Utilisateur from '../models/Utilisateur'
import authService from '../utils/auth.service'

const router = useRouter()

// Initialiser un utilisateur pour la connexion
const utilisateur = new Utilisateur()

const credentials = reactive({
    nomUtilisateur: '',
    codeAgence: '',
    motDePasse: ''
})

const loading = ref(false)
const error = ref('')

/**
 * Valide les credentials avant la connexion
 * @returns {boolean}
 */
const validerFormulaire = () => {
    error.value = ''
    const validation = utilisateur.validerConnexion(
        credentials.nomUtilisateur,
        parseInt(credentials.codeAgence, 10),
        credentials.motDePasse
    )

    if (!validation.valide) {
        error.value = validation.erreurs[0]
        return false
    }
    return true
}

/**
 * Traite la soumission du formulaire de connexion
 */
const handleLogin = async () => {
    if (loading.value) return

    // Valider le formulaire avant d'envoyer
    if (!validerFormulaire()) {
        return
    }

    loading.value = true

    try {
        // Charger les données dans l'utilisateur
        utilisateur.nomUtilisateur = credentials.nomUtilisateur
        utilisateur.motDePasse = credentials.motDePasse

        // Appeler le service d'authentification
        const response = await authService.login(
            credentials.nomUtilisateur,
            credentials.codeAgence,
            credentials.motDePasse
        )

        // Charger les données de réponse dans l'utilisateur
        utilisateur.chargerDonnees(response)

        // Redirection en cas de succès
        router.push({ name: 'home' })
    } catch (err) {
        console.error('Login error detail from backend:', err)

        // Extraire et afficher le message d'erreur approprié
        if (err) {
            if (err.erreur) {
                error.value = err.erreur
            } else if (err.message) {
                error.value = err.message
            } else if (err.error) {
                error.value = err.error
            } else if (typeof err === 'string') {
                error.value = err
            } else {
                error.value = "Une erreur s'est produite lors de la connexion."
            }
        } else {
            error.value = "Une erreur s'est produite lors de la connexion."
        }
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.glass-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(16px);
}

.input-group-text {
    border-color: #dee2e6;
}

.form-control:focus {
    box-shadow: none;
    border-color: var(--bs-primary);
}

.input-group:focus-within .input-group-text,
.input-group:focus-within .form-control {
    border-color: var(--bs-primary);
}
</style>
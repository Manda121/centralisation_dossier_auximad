<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Utilisateur from '../models/Utilisateur'
import authService from '../utils/auth.service'

const router = useRouter()

const utilisateur = new Utilisateur()

const credentials = reactive({
    nomUtilisateur: '',
    codeAgence: '',
    motDePasse: ''
})

const loading = ref(false)
const error = ref('')

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

const handleLogin = async () => {
    if (loading.value) return

    if (!validerFormulaire()) {
        return
    }

    loading.value = true

    try {
        utilisateur.nomUtilisateur = credentials.nomUtilisateur
        utilisateur.motDePasse = credentials.motDePasse

        const response = await authService.login(
            credentials.nomUtilisateur,
            credentials.codeAgence,
            credentials.motDePasse
        )

        utilisateur.chargerDonnees(response)

        router.push({ name: 'home' })
    } catch (err) {
        console.error('Login error detail from backend:', err)

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

<template>
    <div class="card border shadow" style="max-width: 420px; width: 100%;">
        <div class="card-body p-4">
            <div class="text-center mb-4">
                <h1 class="h4 fw-bold text-dark mb-1">Auximad</h1>
                <p class="text-secondary small mb-0">Centralisation de dossiers</p>
            </div>

            <div v-if="error" class="alert alert-danger small py-2 mb-3" role="alert">
                {{ error }}
            </div>

            <form @submit.prevent="handleLogin">
                <div class="mb-3">
                    <label for="nomUtilisateur" class="form-label small fw-semibold text-secondary">Nom d'utilisateur</label>
                    <input id="nomUtilisateur" v-model="credentials.nomUtilisateur" type="text"
                        class="form-control form-control-sm" placeholder="Ex: jean.dupont" required
                        :disabled="loading" />
                </div>

                <div class="mb-3">
                    <label for="codeAgence" class="form-label small fw-semibold text-secondary">Code Agence</label>
                    <input id="codeAgence" v-model="credentials.codeAgence" type="number"
                        class="form-control form-control-sm" placeholder="Code de votre agence"
                        required :disabled="loading" />
                </div>

                <div class="mb-4">
                    <label for="motDePasse" class="form-label small fw-semibold text-secondary">Mot de passe</label>
                    <input id="motDePasse" v-model="credentials.motDePasse" type="password"
                        class="form-control form-control-sm" placeholder="Votre mot de passe" required
                        :disabled="loading" />
                </div>

                <button type="submit" class="btn btn-primary w-100" :disabled="loading">
                    <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                    {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
                </button>
            </form>
        </div>
    </div>
</template>

<style scoped>
</style>
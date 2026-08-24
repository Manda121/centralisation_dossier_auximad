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
  if (!validerFormulaire()) return

  loading.value = true
  try {
    const response = await authService.login(
      credentials.nomUtilisateur,
      credentials.codeAgence,
      credentials.motDePasse
    )
    utilisateur.chargerDonnees(response)
    router.push({ name: 'home' })
  } catch (err) {
    console.error('Login error:', err)
    error.value = err?.erreur || err?.message || err?.error || 'Une erreur est survenue lors de la connexion.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-card">
    <div class="login-card__header">
      <h1 class="login-card__title">Auximad</h1>
      <p class="login-card__subtitle">Centralisation de dossiers</p>
    </div>

    <div v-if="error" class="login-card__error" role="alert">
      {{ error }}
    </div>

    <form @submit.prevent="handleLogin" class="login-card__form">
      <div class="login-card__field">
        <label for="nomUtilisateur" class="login-card__label">Nom d'utilisateur</label>
        <input
          id="nomUtilisateur"
          v-model="credentials.nomUtilisateur"
          type="text"
          class="login-card__input"
          required
          :disabled="loading"
        />
      </div>

      <div class="login-card__field">
        <label for="codeAgence" class="login-card__label">Code Agence</label>
        <input
          id="codeAgence"
          v-model="credentials.codeAgence"
          type="number"
          class="login-card__input"
          required
          :disabled="loading"
        />
      </div>

      <div class="login-card__field">
        <label for="motDePasse" class="login-card__label">Mot de passe</label>
        <input
          id="motDePasse"
          v-model="credentials.motDePasse"
          type="password"
          class="login-card__input"
          placeholder="Votre mot de passe"
          required
          :disabled="loading"
        />
      </div>

      <button type="submit" class="login-card__button" :disabled="loading">
        <span v-if="loading" class="login-card__spinner"></span>
        {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-card {
  --lc-bg: #ffffff;
  --lc-border: #e2e6ec;
  --lc-text: #1e2a3a;
  --lc-text-muted: #5e6f88;
  --lc-input-bg: #f8f9fc;
  --lc-primary: #3b6a9c;
  --lc-primary-hover: #2d5380;
  --lc-error-bg: #fef2f2;
  --lc-error-text: #8a3b3b;
  --lc-radius: 10px;
  --lc-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);

  max-width: 400px;
  width: 100%;
  background: var(--lc-bg);
  border-radius: var(--lc-radius);
  box-shadow: var(--lc-shadow);
  border: 1px solid var(--lc-border);
  padding: 28px 30px 32px;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: var(--lc-text);
}

.login-card__header {
  text-align: center;
  margin-bottom: 24px;
}

.login-card__title {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: var(--lc-text);
  margin: 0 0 2px;
}

.login-card__subtitle {
  font-size: 0.9rem;
  color: var(--lc-text-muted);
  margin: 0;
}

.login-card__error {
  background: var(--lc-error-bg);
  border: 1px solid #f5d0d0;
  color: var(--lc-error-text);
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  margin-bottom: 18px;
}

.login-card__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-card__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-card__label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--lc-text-muted);
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.login-card__input {
  padding: 10px 14px;
  background: var(--lc-input-bg);
  border: 1px solid var(--lc-border);
  border-radius: 6px;
  font-size: 0.95rem;
  transition: border-color 0.15s, box-shadow 0.15s;
  outline: none;
  color: var(--lc-text);
}

.login-card__input:focus {
  border-color: var(--lc-primary);
  box-shadow: 0 0 0 3px rgba(59, 106, 156, 0.12);
}

.login-card__input:disabled {
  opacity: 0.6;
}

/* Supprimer les flèches pour input type number */
.login-card__input[type="number"]::-webkit-outer-spin-button,
.login-card__input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.login-card__input[type="number"] {
  -moz-appearance: textfield;
}

.login-card__button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--lc-primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  font-family: inherit;
}

.login-card__button:hover:not(:disabled) {
  background: var(--lc-primary-hover);
}

.login-card__button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-card__spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
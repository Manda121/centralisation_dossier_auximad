<template>
  <div class="login-container">
    <div class="login-card glass-panel animate-fade-in">
      
      <!-- Brand Logo / Title -->
      <div class="login-header">
        <h1 class="brand-title">Auximad</h1>
        <p class="brand-subtitle">Centralisation de dossiers</p>
      </div>

      <!-- Error / Suspension Alert -->
      <div v-if="error" class="alert-box error animate-fade-in">
        <svg xmlns="http://www.w3.org/2001/svg" class="alert-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="alert-content">
          <p class="alert-message">{{ error }}</p>
        </div>
      </div>

      <!-- Login Form -->
      <form @submit.prevent="handleLogin" class="login-form">
        
        <div class="form-group">
          <label for="nomUtilisateur">Nom d'utilisateur</label>
          <div class="input-wrapper">
            <svg xmlns="http://www.w3.org/2001/svg" class="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <input 
              id="nomUtilisateur"
              v-model="credentials.nomUtilisateur" 
              type="text" 
              placeholder="Ex: jean.dupont" 
              required
              :disabled="loading"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="codeAgence">Code Agence</label>
          <div class="input-wrapper">
            <svg xmlns="http://www.w3.org/2001/svg" class="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <input 
              id="codeAgence"
              v-model="credentials.codeAgence" 
              type="number" 
              placeholder="Code de votre agence" 
              required
              :disabled="loading"
            />
          </div>
        </div>

        <div class="form-group">
          <label for="motDePasse">Mot de passe</label>
          <div class="input-wrapper">
            <svg xmlns="http://www.w3.org/2001/svg" class="input-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            <input 
              id="motDePasse"
              v-model="credentials.motDePasse" 
              type="password" 
              placeholder="Votre mot de passe" 
              required
              :disabled="loading"
            />
          </div>
        </div>

        <button type="submit" class="submit-btn list-btn" :disabled="loading">
          <span v-show="!loading">Se connecter</span>
          <div v-show="loading" class="spinner"></div>
        </button>

      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import authService from '../utils/auth.service'

const router = useRouter()

const credentials = reactive({
  nomUtilisateur: '',
  codeAgence: '',
  motDePasse: ''
})

const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  if (loading.value) return
  
  loading.value = true
  error.value = ''

  try {
    const data = await authService.login(
      credentials.nomUtilisateur,
      credentials.codeAgence,
      credentials.motDePasse
    )
    
    // Redirect on success
    router.push({ name: 'home' })
  } catch (err) {
    // Handling error response from backend
    if (err && err.erreur) {
      error.value = err.erreur
    } else {
      error.value = "Une erreur inattendue s'est produite."
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 1rem;
}

.login-card {
  width: 100%;
  max-width: 440px;
  padding: 2.5rem 2rem;
  border-radius: 1.5rem;
}

.login-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.brand-title {
  font-size: 2rem;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.025em;
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-subtitle {
  color: var(--color-text-muted);
  font-size: 0.95rem;
  font-weight: 500;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 1rem;
  width: 1.25rem;
  height: 1.25rem;
  color: var(--color-text-muted);
}

input {
  padding-left: 2.75rem; /* Space for the icon */
}

/* Alert Styling */
.alert-box {
  display: flex;
  align-items: flex-start;
  padding: 1rem;
  border-radius: 0.75rem;
  margin-bottom: 1.5rem;
  gap: 0.75rem;
}

.alert-box.error {
  background-color: var(--color-error-bg);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.alert-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: var(--color-error);
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
}

.alert-message {
  color: #fecaca;
  font-size: 0.875rem;
  line-height: 1.4;
  font-weight: 500;
}

/* Primary Button */
.submit-btn {
  width: 100%;
  margin-top: 2rem;
  padding: 0.875rem 1rem;
  border: none;
  border-radius: 0.5rem;
  background: var(--color-primary);
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-family);
  box-shadow: 0 4px 14px 0 rgba(59, 130, 246, 0.39);
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.23);
  transform: translateY(-1px);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Button with loading state doesn't shift height */
.submit-btn .spinner {
  width: 1.25rem;
  height: 1.25rem;
  border-width: 2px;
}
</style>

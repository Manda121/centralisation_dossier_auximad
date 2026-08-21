# Exemples d'utilisation - Architecture OOP Login

## 1. Utiliser le Service dans un Composant

### Exemple : Afficher l'utilisateur connecté partout

```vue
<template>
  <div class="profile">
    <h1>Bienvenue {{ utilisateur?.getNomComplet() }}</h1>
    <p>Email: {{ utilisateur?.email }}</p>
    <p>Rôle: {{ utilisateur?.role }}</p>
    <p>Agence: {{ utilisateur?.agence }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UtilisateurService from '@/services/UtilisateurService'

const utilisateur = ref(null)

onMounted(() => {
  utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
})
</script>
```

## 2. Créer un Composant de Profil

```vue
<template>
  <div class="profile-card" v-if="utilisateur">
    <div class="profile-header">
      <h2>{{ utilisateur.getNomComplet() }}</h2>
      <p class="role-badge" :class="getRoleClass()">
        {{ utilisateur.role }}
      </p>
    </div>

    <div class="profile-body">
      <div class="field">
        <label>Nom d'utilisateur:</label>
        <span>{{ utilisateur.nomUtilisateur }}</span>
      </div>
      <div class="field">
        <label>Email:</label>
        <span>{{ utilisateur.email }}</span>
      </div>
      <div class="field">
        <label>Agence:</label>
        <span>{{ utilisateur.agence }}</span>
      </div>
      <div class="field">
        <label>Inscrit depuis:</label>
        <span>{{ formatDate(utilisateur.dateCreation) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import UtilisateurService from '@/services/UtilisateurService'

const utilisateur = ref(null)

onMounted(() => {
  utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
})

const getRoleClass = () => {
  if (!utilisateur.value) return ''
  return `role-${utilisateur.value.role?.toLowerCase()}`
}

const formatDate = (date) => {
  if (!date) return 'N/A'
  return new Date(date).toLocaleDateString('fr-FR')
}
</script>

<style scoped>
.profile-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.role-badge {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 600;
}

.role-badge.role-admin {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

.role-badge.role-user {
  background: rgba(59, 130, 246, 0.2);
  color: #93c5fd;
}

.field {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.field label {
  font-weight: 600;
  color: #cbd5e1;
}

.field span {
  color: #e2e8f0;
}
</style>
```

## 3. Créer un Guard de Route Autentification

```javascript
// router/authGuard.js
import UtilisateurService from '@/services/UtilisateurService'

export function setupAuthGuard(router) {
  router.beforeEach((to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
    const isLoggedIn = UtilisateurService.estConnecte()

    if (requiresAuth && !isLoggedIn) {
      // Rediriger vers login si non authentifié
      next('/login')
    } else if (to.path === '/login' && isLoggedIn) {
      // Rediriger vers home si déjà connecté
      next('/home')
    } else {
      next()
    }
  })
}
```

Utilisation dans `router/index.js`:

```javascript
import { setupAuthGuard } from './authGuard'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { requiresAuth: false }
    }
  ]
})

setupAuthGuard(router)
export default router
```

## 4. Créer une Store Globale (Pinia/Composition API)

```javascript
// stores/authStore.js
import { ref, computed } from 'vue'
import UtilisateurService from '@/services/UtilisateurService'

export function useAuthStore() {
  const utilisateur = ref(null)
  const isLoading = ref(false)

  const isConnecte = computed(() => {
    return UtilisateurService.estConnecte()
  })

  const nomComplet = computed(() => {
    return utilisateur.value?.getNomComplet() || ''
  })

  const chargerUtilisateur = () => {
    utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
  }

  const connecter = async (nomUtilisateur, codeAgence, motDePasse) => {
    isLoading.value = true
    try {
      utilisateur.value = await UtilisateurService.authentifier(
        nomUtilisateur,
        codeAgence,
        motDePasse
      )
    } finally {
      isLoading.value = false
    }
  }

  const deconnecter = () => {
    utilisateur.value = null
    UtilisateurService.deconnecter()
  }

  return {
    utilisateur,
    isLoading,
    isConnecte,
    nomComplet,
    chargerUtilisateur,
    connecter,
    deconnecter
  }
}
```

Utilisation:

```vue
<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

onMounted(() => {
  auth.chargerUtilisateur()
})
</script>

<template>
  <div v-if="auth.isConnecte">
    <p>Connecté en tant que: {{ auth.nomComplet }}</p>
  </div>
</template>
```

## 5. Ajouter une Vérification de Permissions

```javascript
// models/Utilisateur.js - Ajouter à la classe Utilisateur

export class Utilisateur {
  // ... existing code ...

  /**
   * Vérifie si l'utilisateur a un rôle spécifique
   * @param {string} roleRequis
   * @returns {boolean}
   */
  aLesPermissions(roleRequis) {
    if (!this.role) return false
    return this.role === roleRequis || this.role === 'ADMIN'
  }

  /**
   * Vérifie si l'utilisateur est administrateur
   * @returns {boolean}
   */
  estAdmin() {
    return this.role === 'ADMIN'
  }

  /**
   * Vérifie si l'utilisateur appartient à une agence
   * @param {number} idAgence
   * @returns {boolean}
   */
  appartientAgence(idAgence) {
    return this.agence?.id === idAgence
  }
}
```

Utilisation:

```vue
<template>
  <div>
    <!-- Afficher seulement pour les admins -->
    <button v-if="utilisateur?.estAdmin()">
      Gérer les utilisateurs
    </button>

    <!-- Afficher si permissions spécifiques -->
    <div v-if="utilisateur?.aLesPermissions('MANAGER')">
      Section gestionnaire
    </div>
  </div>
</template>
```

## 6. Créer un Composant Réutilisable de Connexion

```vue
<!-- components/SmartLoginForm.vue -->
<template>
  <form @submit.prevent="handleLogin">
    <input
      v-model="credentials.nomUtilisateur"
      type="text"
      placeholder="Nom d'utilisateur"
      @input="clearError"
    />
    <input
      v-model="credentials.codeAgence"
      type="number"
      placeholder="Code agence"
      @input="clearError"
    />
    <input
      v-model="credentials.motDePasse"
      type="password"
      placeholder="Mot de passe"
      @input="clearError"
    />

    <div v-if="errors.length > 0" class="error-list">
      <p v-for="(error, i) in errors" :key="i">{{ error }}</p>
    </div>

    <button type="submit" :disabled="isLoading">
      {{ isLoading ? 'Connexion en cours...' : 'Se connecter' }}
    </button>
  </form>
</template>

<script setup>
import { reactive, ref, emit } from 'vue'
import Utilisateur from '@/models/Utilisateur'

const utilisateur = new Utilisateur()

const credentials = reactive({
  nomUtilisateur: '',
  codeAgence: '',
  motDePasse: ''
})

const isLoading = ref(false)
const errors = ref([])

const clearError = () => {
  errors.value = []
}

const handleLogin = async () => {
  const validation = utilisateur.validerConnexion(
    credentials.nomUtilisateur,
    credentials.codeAgence,
    credentials.motDePasse
  )

  if (!validation.valide) {
    errors.value = validation.erreurs
    return
  }

  isLoading.value = true

  try {
    emit('login-attempt', {
      nomUtilisateur: credentials.nomUtilisateur,
      codeAgence: credentials.codeAgence,
      motDePasse: credentials.motDePasse
    })
  } finally {
    isLoading.value = false
  }
}
</script>
```

## 7. Tester le Modèle Utilisateur

```javascript
// tests/Utilisateur.spec.js
import { describe, it, expect } from 'vitest'
import Utilisateur from '@/models/Utilisateur'

describe('Utilisateur Model', () => {
  it('doit valider une connexion correcte', () => {
    const utilisateur = new Utilisateur()
    const validation = utilisateur.validerConnexion(
      'jean.dupont',
      1,
      'password123'
    )
    expect(validation.valide).toBe(true)
    expect(validation.erreurs).toHaveLength(0)
  })

  it('doit rejeter un nom utilisateur vide', () => {
    const utilisateur = new Utilisateur()
    const validation = utilisateur.validerConnexion('', 1, 'password123')
    expect(validation.valide).toBe(false)
    expect(validation.erreurs).toContain("Le nom d'utilisateur est requis")
  })

  it('doit retourner le nom complet', () => {
    const utilisateur = new Utilisateur({
      prenom: 'Jean',
      nom: 'Dupont'
    })
    expect(utilisateur.getNomComplet()).toBe('Jean Dupont')
  })

  it('doit valider un email', () => {
    const utilisateur = new Utilisateur({ email: 'test@example.com' })
    expect(utilisateur.estEmailValide()).toBe(true)

    utilisateur.email = 'invalid-email'
    expect(utilisateur.estEmailValide()).toBe(false)
  })
})
```

## Résumé des Bonnes Pratiques

✅ Toujours utiliser `UtilisateurService` pour les opérations d'authentification
✅ Valider côté client avec le modèle avant d'envoyer
✅ Charger l'utilisateur connecté au montage du composant
✅ Utiliser les méthodes métier pour la logique complexe
✅ Centraliser la logique dans les services, pas dans les composants
✅ Écrire des tests pour les models et services

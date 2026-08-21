<template>
    <aside class="d-flex flex-column flex-shrink-0 p-3 bg-white border-end vh-100 sidebar-container shadow-sm">
        <!-- Brand / Logo -->
        <router-link to="/" class="d-flex align-items-center mb-3 me-md-auto link-dark text-decoration-none">
            <span class="fs-4 fw-bold text-primary">Auximad</span>
            <span v-if="userInfo && userInfo.agence"
                class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">
                {{ userInfo.agence }}
            </span>
        </router-link>

        <hr class="my-2">

        <!-- User Infos (Si connecté) -->
        <div v-if="isLoggedIn" class="card border-0 bg-light shadow-sm mb-auto p-3 rounded-3">
            <div class="d-flex flex-column">
                <span class="fw-bold text-dark fs-6">{{ userInfo?.nomComplet || userInfo?.nomUtilisateur ||
                    'Utilisateur' }}</span>
                <span v-if="userInfo?.role"
                    class="badge bg-secondary-subtle text-secondary-emphasis w-auto align-self-start mt-1 fs-7">
                    {{ userInfo?.role }}
                </span>
            </div>
        </div>

        <!-- Section Bas / Déconnexion -->
        <div v-if="isLoggedIn" class="mt-auto">
            <hr class="my-2">
            <button @click="handleLogout"
                class="btn btn-outline-danger w-100 d-flex align-items-center justify-content-center gap-2 py-2 fw-semibold">
                <svg xmlns="http://www.w3.org/2001/svg" width="16" height="16" fill="currentColor"
                    class="bi bi-box-arrow-right" viewBox="0 0 16 16">
                    <path fill-rule="evenodd"
                        d="M10 12.5a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v2a.5.5 0 0 0 1 0v-2A1.5 1.5 0 0 0 9.5 2h-8A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-2a.5.5 0 0 0-1 0z" />
                    <path fill-rule="evenodd"
                        d="M15.854 8.354a.5.5 0 0 0 0-.708l-3-3a.5.5 0 0 0-.708.708L14.293 7.5H5.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708z" />
                </svg>
                <span>Déconnexion</span>
            </button>
        </div>
    </aside>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UtilisateurService from '../../services/UtilisateurService'

const router = useRouter()
const utilisateur = ref(null)

const isLoggedIn = computed(() => {
    return UtilisateurService.estConnecte()
})

const userInfo = computed(() => {
    if (!utilisateur.value) {
        return null
    }
    return UtilisateurService.formaterPourAffichage(utilisateur.value)
})

const chargerUtilisateur = () => {
    utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
}

onMounted(() => {
    chargerUtilisateur()
})

const handleLogout = () => {
    UtilisateurService.deconnecter()
    router.push('/login')
}
</script>

<style scoped>
.sidebar-container {
    width: 280px;
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1000;
}

.fs-7 {
    font-size: 0.75rem;
}
</style>
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

        <!-- Navigation Menu -->
        <ul v-if="isLoggedIn" class="nav nav-pills flex-column mb-auto">
            <li class="nav-item">
                <router-link to="/" class="nav-link text-dark py-2 d-flex align-items-center gap-2"
                    active-class="active text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-house" viewBox="0 0 16 16">
                        <path
                            d="M8.707 1.5a1 1 0 0 0-1.414 0L.646 8.146a.5.5 0 0 0 .708.708L2 8.207V13.5A1.5 1.5 0 0 0 3.5 15h9a1.5 1.5 0 0 0 1.5-1.5V8.207l.646.647a.5.5 0 0 0 .708-.708L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM13 7.207V13.5a.5.5 0 0 1-.5.5h-9a.5.5 0 0 1-.5-.5V7.207l5-5z" />
                    </svg>
                    Tableau de bord
                </router-link>
            </li>
            <li class="nav-item mt-1">
                <router-link to="/clients" class="nav-link text-dark py-2 d-flex align-items-center gap-2"
                    active-class="active text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-people" viewBox="0 0 16 16">
                        <path
                            d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1zm-7.978-1L7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002-.014.002zM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4m3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0M6.936 9.28a6 6 0 0 0-1.23-.247A7 7 0 0 0 5 9c-4 0-5 3-5 4q0 1 1 1h4.216A2.24 2.24 0 0 1 5 13c0-1.01.377-2.047 1.09-2.904.243-.294.526-.569.846-.816M4.92 7.01a4 4 0 1 0 0-6 4 4 0 0 0 0 6" />
                    </svg>
                    Clients
                </router-link>
            </li>
            <li class="nav-item mt-1">
                <router-link to="/ordres-transit" class="nav-link text-dark py-2 d-flex align-items-center gap-2"
                    active-class="active text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-file-earmark-text" viewBox="0 0 16 16">
                        <path
                            d="M5.5 7a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1zM5 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0 2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5" />
                        <path
                            d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z" />
                    </svg>
                    Ordres de Transit
                </router-link>
            </li>
            <li class="nav-item mt-1">
                <router-link to="/marchandises" class="nav-link text-dark py-2 d-flex align-items-center gap-2"
                    active-class="active text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-box-seam" viewBox="0 0 16 16">
                        <path
                            d="M8.186 1.113a.5.5 0 0 0-.372 0L1.846 3.5l2.404.961L10.404 2zm3.564 1.426L5.596 5 8 5.961 14.154 3.5zm.325 1.283L6 6.136V13.5l6-2.4zm-7.075 2.3L1 3.824v6.865l5 2zm-1.5-.724v5.44L1.8 9.528zM8 7.062 14 4.662v5.438l-6 2.4z" />
                    </svg>
                    Marchandises
                </router-link>
            </li>
            <li class="nav-item mt-1">
                <router-link to="/dossiers" class="nav-link text-dark py-2 d-flex align-items-center gap-2"
                    active-class="active text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                        class="bi bi-folder" viewBox="0 0 16 16">
                        <path
                            d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31M2.19 4a1 1 0 0 0-.996 1.09l.637 7a1 1 0 0 0 .995.91h10.348a1 1 0 0 0 .995-.91l.637-7A1 1 0 0 0 13.81 4zm4.69-1.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139h5.17l-.828-.827z" />
                    </svg>
                    Dossiers
                </router-link>
            </li>
        </ul>

        <hr v-if="isLoggedIn" class="my-2">

        <!-- User Infos (Si connecté) -->
        <div v-if="isLoggedIn" class="card border-0 bg-light shadow-sm mb-3 p-3 rounded-3">
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
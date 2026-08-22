<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- Page Title / Actions -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h3 fw-bold text-primary mb-1">Gestion des Clients</h1>
                        <p class="text-secondary mb-0">Recherche, gestion et suivi des clients Auximad.</p>
                    </div>
                    <button v-if="peutCreer" class="btn btn-primary d-flex align-items-center gap-2"
                        @click="ouvrirCreation">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                            viewBox="0 0 16 16">
                            <path
                                d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z" />
                        </svg>
                        Nouveau Client
                    </button>
                </div>

                <!-- Global Search & FTS -->
                <div class="card border-0 shadow-sm mb-4">
                    <div class="card-body p-3">
                        <div class="row g-3 align-items-center">
                            <div class="col-md-9">
                                <div class="input-group">
                                    <span class="input-group-text bg-white border-end-0">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                            fill="currentColor" class="text-muted" viewBox="0 0 16 16">
                                            <path
                                                d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001q.044.06.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1 1 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0" />
                                        </svg>
                                    </span>
                                    <input type="text" v-model="allFilters.query"
                                        class="form-control border-start-0 ps-0"
                                        placeholder="Rechercher par nom, prenom, email, NIF, STAT... (FTS)"
                                        @input="declencherRecherche">
                                </div>
                            </div>
                            <div class="col-md-3 text-end">
                                <button class="btn btn-link btn-xs text-decoration-none text-muted"
                                    @click="reinitialiserFiltres">
                                    Effacer tout
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Centralized Table Component -->
                <div class="card border-0 shadow-sm p-4">
                    <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerClients"
                        :filters="allFilters" default-sort="nom" default-sort-dir="asc" :pageSize="15">

                        <template #filters="{ filters }">
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Code Client</label>
                                    <input type="number" v-model="filters.codeClient"
                                        class="form-control form-control-sm" placeholder="ex: 12">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Nom</label>
                                    <input type="text" v-model="filters.nom" class="form-control form-control-sm"
                                        placeholder="Nom">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Prénom</label>
                                    <input type="text" v-model="filters.prenom" class="form-control form-control-sm"
                                        placeholder="Prénom">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Type Client</label>
                                    <select v-model="filters.typeClientId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="t in typeClients" :key="t.id" :value="t.id">{{ t.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">NIF</label>
                                    <input type="text" v-model="filters.nif" class="form-control form-control-sm"
                                        placeholder="NIF client">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">STAT</label>
                                    <input type="text" v-model="filters.stat" class="form-control form-control-sm"
                                        placeholder="STAT client">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Pays</label>
                                    <select v-model="filters.paysId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="p in listePays" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Ville</label>
                                    <input type="text" v-model="filters.ville" class="form-control form-control-sm"
                                        placeholder="Ville">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Téléphone</label>
                                    <input type="text" v-model="filters.telephone" class="form-control form-control-sm"
                                        placeholder="Téléphone">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Email</label>
                                    <input type="text" v-model="filters.email" class="form-control form-control-sm"
                                        placeholder="Email">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Fax</label>
                                    <input type="text" v-model="filters.fax" class="form-control form-control-sm"
                                        placeholder="Fax">
                                </div>
                            </div>
                        </template>

                        <template #cell-code="{ row }">
                            <router-link :to="`/clients/${row.id}`" class="fw-semibold text-primary">
                                CLI-{{ String(row.id).padStart(4, '0') }}
                            </router-link>
                        </template>

                        <template #cell-typeClient.nom="{ row }">
                            <span class="badge rounded-pill bg-light text-secondary border">
                                {{ row.typeClient?.nom || '—' }}
                            </span>
                        </template>

                        <template #actions="{ row }">
                            <div class="d-flex gap-2">
                                <router-link :to="`/clients/${row.id}`" class="btn btn-secondary btn-xs">
                                    Détails &rarr;
                                </router-link>
                            </div>
                        </template>
                    </DataTable>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import ClientService from '../services/ClientService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const router = useRouter()
const datatableRef = ref(null)

// Paramètres de filtres
const allFilters = reactive({
    query: '',
    codeClient: '',
    nom: '',
    prenom: '',
    typeClientId: '',
    nif: '',
    stat: '',
    paysId: '',
    ville: '',
    telephone: '',
    email: '',
    fax: ''
})

// Chargement référentiels
const typeClients = ref([])
const listePays = ref([])

onMounted(async () => {
    try {
        const [types, pays] = await Promise.all([
            ClientService.getAllTypeClients(),
            ClientService.getAllPays()
        ])
        typeClients.value = types
        listePays.value = pays
    } catch (e) {
        console.error('Erreur référentiels clients', e)
    }
})

// Colonnes DataTable
const columns = [
    { key: 'code', label: 'Code Client', sortable: false, width: '130px' },
    { key: 'nom', label: 'Nom', sortable: true },
    { key: 'prenom', label: 'Prénom', sortable: true },
    { key: 'typeClient.nom', label: 'Type', sortable: false },
    { key: 'telephone', label: 'Téléphone', sortable: true },
    { key: 'email', label: 'Email', sortable: true },
    { key: 'ville', label: 'Ville', sortable: true },
    { key: 'pays.nom', label: 'Pays', sortable: false }
]

// Permission de création
const peutCreer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
})

// Fonction fetchData pour le composant DataTable
async function chargerClients(page, size, sortBy, sortDir, filters) {
    return await ClientService.searchClients(page, size, sortBy, sortDir, filters)
}

// Timeout de debounce pour la recherche globale Full Text
let searchTimeout = null
function declencherRecherche() {
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => {
        if (datatableRef.value) {
            datatableRef.value.refresh()
        }
    }, 400)
}

function ouvreCreation() {
    router.push('/clients/nouveau')
}

function ouvrirCreation() {
    router.push('/clients/nouveau')
}

function reinitialiserFiltres() {
    Object.keys(allFilters).forEach(key => {
        allFilters[key] = ''
    })
    if (datatableRef.value) {
        datatableRef.value.refresh()
    }
}
</script>

<style scoped>
.text-xs {
    font-size: 0.75rem;
}

.btn-xs {
    padding: 0.25rem 0.5rem;
    font-size: 0.75rem;
    border-radius: 4px;
}
</style>

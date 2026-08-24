<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import ClientService from '../services/ClientService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const router = useRouter()
const datatableRef = ref(null)

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

const peutCreer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
})

async function chargerClients(page, size, sortBy, sortDir, filters) {
    return await ClientService.searchClients(page, size, sortBy, sortDir, filters)
}

let searchTimeout = null
function declencherRecherche() {
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => {
        if (datatableRef.value) {
            datatableRef.value.refresh()
        }
    }, 400)
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

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Gestion des Clients</h1>
                        <p class="text-secondary mb-0 small">Recherche, gestion et suivi des clients Auximad.</p>
                    </div>
                    <button v-if="peutCreer" class="btn btn-primary btn-sm" @click="ouvrirCreation">
                        Nouveau Client
                    </button>
                </div>

                <div class="card border shadow-sm mb-3">
                    <div class="card-body p-3">
                        <div class="row g-2 align-items-center">
                            <div class="col-md-9">
                                <input type="text" v-model="allFilters.query" class="form-control form-control-sm"
                                    placeholder="Rechercher par nom, prénom, email, NIF, STAT..."
                                    @input="declencherRecherche">
                            </div>
                            <div class="col-md-3 text-end">
                                <button class="btn btn-link btn-sm text-secondary text-decoration-none" @click="reinitialiserFiltres">
                                    Effacer tout
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerClients"
                            :filters="allFilters" default-sort="nom" default-sort-dir="asc" :pageSize="15">

                            <template #filters="{ filters }">
                                <div class="row g-2">
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Code Client</label>
                                        <input type="number" v-model="filters.codeClient" class="form-control form-control-sm" placeholder="ex: 12">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Nom</label>
                                        <input type="text" v-model="filters.nom" class="form-control form-control-sm" placeholder="Nom">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Prénom</label>
                                        <input type="text" v-model="filters.prenom" class="form-control form-control-sm" placeholder="Prénom">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Type Client</label>
                                        <select v-model="filters.typeClientId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="t in typeClients" :key="t.id" :value="t.id">{{ t.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">NIF</label>
                                        <input type="text" v-model="filters.nif" class="form-control form-control-sm" placeholder="NIF client">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">STAT</label>
                                        <input type="text" v-model="filters.stat" class="form-control form-control-sm" placeholder="STAT client">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Pays</label>
                                        <select v-model="filters.paysId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="p in listePays" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Ville</label>
                                        <input type="text" v-model="filters.ville" class="form-control form-control-sm" placeholder="Ville">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Téléphone</label>
                                        <input type="text" v-model="filters.telephone" class="form-control form-control-sm" placeholder="Téléphone">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Email</label>
                                        <input type="text" v-model="filters.email" class="form-control form-control-sm" placeholder="Email">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Fax</label>
                                        <input type="text" v-model="filters.fax" class="form-control form-control-sm" placeholder="Fax">
                                    </div>
                                </div>
                            </template>

                            <template #cell-code="{ row }">
                                <router-link :to="`/clients/${row.id}`" class="fw-semibold text-primary text-decoration-none">
                                    CLI-{{ String(row.id).padStart(4, '0') }}
                                </router-link>
                            </template>

                            <template #cell-typeClient.nom="{ row }">
                                {{ row.typeClient?.nom || '—' }}
                            </template>
                        </DataTable>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<style scoped>
</style>
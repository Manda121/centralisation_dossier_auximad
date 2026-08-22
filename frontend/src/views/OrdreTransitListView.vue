<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- Page Title -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h3 fw-bold text-primary mb-1">Ordres de Transit (OT)</h1>
                        <p class="text-secondary mb-0">Recherche, consultation et suivi des ordres de transit.</p>
                    </div>
                    <button v-if="peutCreer" class="btn btn-primary d-flex align-items-center gap-2"
                        @click="ouvrirWizard">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                            viewBox="0 0 16 16">
                            <path
                                d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z" />
                        </svg>
                        Nouveau Wizard OT
                    </button>
                </div>

                <!-- Global search & clear -->
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
                                        placeholder="Rechercher par Numéro OT, Commande, Client, Expéditeur... (FTS)"
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
                    <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerOts" :filters="allFilters"
                        default-sort="date_creation" default-sort-dir="desc" :pageSize="15">

                        <template #filters="{ filters }">
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Numéro OT</label>
                                    <input type="number" v-model="filters.numero" class="form-control form-control-sm"
                                        placeholder="ex: 10001">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Date de création Min</label>
                                    <input type="date" v-model="filters.dateMin" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Date de création Max</label>
                                    <input type="date" v-model="filters.dateMax" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Client Donneur d'ordre</label>
                                    <select v-model="filters.clientId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom
                                            }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Expéditeur</label>
                                    <select v-model="filters.expediteurId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom
                                            }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Destinataire</label>
                                    <select v-model="filters.destinataireId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom
                                            }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Type Régime</label>
                                    <select v-model="filters.typeRegimeId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="r in typeRegimes" :key="r.id" :value="r.id">{{ r.code }} - {{
                                            r.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Type de Production</label>
                                    <select v-model="filters.typeProductionId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="p in typeProductions" :key="p.id" :value="p.id">{{ p.nom }}
                                        </option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Assuré</label>
                                    <select v-model="filters.estAssure" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option value="true">Oui</option>
                                        <option value="false">Non</option>
                                    </select>
                                </div>
                            </div>
                        </template>

                        <template #cell-numeroOrdreTransit="{ row }">
                            <router-link :to="`/ordres-transit/${row.id}`" class="fw-semibold text-primary">
                                OT-{{ row.numeroOrdreTransit }}
                            </router-link>
                        </template>

                        <template #cell-dateCreation="{ row }">
                            {{ formatarDate(row.dateCreation) }}
                        </template>

                        <template #cell-client.nom="{ row }">
                            {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
                        </template>

                        <template #cell-estAssure="{ row }">
                            <span
                                :class="row.estAssure ? 'badge bg-success-subtle text-success' : 'badge bg-danger-subtle text-danger'">
                                {{ row.estAssure ? 'Oui' : 'Non' }}
                            </span>
                        </template>

                        <template #cell-statutOt.nom="{ row }">
                            <span :class="obtenirBadgeClasse(row.statutOt?.nom)">
                                {{ row.statutOt?.nom || 'Non validé' }}
                            </span>
                        </template>

                        <template #actions="{ row }">
                            <div class="d-flex gap-2">
                                <router-link :to="`/ordres-transit/${row.id}`" class="btn btn-secondary btn-xs">
                                    Consulter &rarr;
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
import OrdreTransitService from '../services/OrdreTransitService'
import ClientService from '../services/ClientService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const router = useRouter()
const datatableRef = ref(null)

const allFilters = reactive({
    query: '',
    numero: '',
    dateMin: '',
    dateMax: '',
    clientId: '',
    expediteurId: '',
    destinataireId: '',
    typeRegimeId: '',
    typeProductionId: '',
    estAssure: ''
})

const clients = ref([])
const typeRegimes = ref([])
const typeProductions = ref([])

onMounted(async () => {
    try {
        const [cls, regimes, productions] = await Promise.all([
            ClientService.searchClients(0, 1000, 'nom', 'asc', {}),
            OrdreTransitService.getAllTypeRegimes(),
            OrdreTransitService.getAllTypeProductions()
        ])
        clients.value = cls.content || []
        typeRegimes.value = regimes
        typeProductions.value = productions
    } catch (e) {
        console.error('Erreur chargement filtres référentiels', e)
    }
})

const columns = [
    { key: 'numeroOrdreTransit', label: 'Numéro OT', sortable: true, width: '130px' },
    { key: 'dateCreation', label: 'Date création', sortable: true },
    { key: 'client.nom', label: 'Client donneur', sortable: false },
    { key: 'typeProduction.nom', label: 'Production', sortable: false },
    { key: 'typeRegime.code', label: 'Régime', sortable: false },
    { key: 'estAssure', label: 'Assuré', sortable: true },
    { key: 'statutOt.nom', label: 'Statut', sortable: false }
]

const peutCreer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
})

async function chargerOts(page, size, sortBy, sortDir, filters) {
    return await OrdreTransitService.searchOrdreTransits(page, size, sortBy, sortDir, filters)
}

let searchTimeout = null
function declencherRecherche() {
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => {
        if (datatableRef.value) {
            datatableRef.value.refresh()
        }
    }, 450)
}

function ouvrirWizard() {
    router.push('/ordres-transit/creer')
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    })
}

function obtenirBadgeClasse(status) {
    if (!status) return 'badge bg-warning-subtle text-warning border border-warning-subtle'
    switch (status.toLowerCase()) {
        case 'validé':
        case 'valide':
            return 'badge bg-success-subtle text-success border border-success-subtle'
        case 'brouillon':
            return 'badge bg-secondary-subtle text-secondary border border-secondary-subtle'
        default:
            return 'badge bg-warning-subtle text-warning border border-warning-subtle'
    }
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

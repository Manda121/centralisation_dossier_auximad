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
    { key: 'estAssure', label: 'Assuré', sortable: true }
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
                        <h1 class="h4 fw-bold text-dark mb-1">Ordres de Transit (OT)</h1>
                        <p class="text-secondary mb-0 small">Recherche, consultation et suivi des ordres de transit.</p>
                    </div>
                    <button v-if="peutCreer" class="btn btn-primary btn-sm" @click="ouvrirWizard">
                        Nouveau Wizard OT
                    </button>
                </div>

                

                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerOts" :filters="allFilters"
                            default-sort="date_creation" default-sort-dir="desc" :pageSize="15">

                            <template #filters="{ filters }">
                                <div class="row g-2">
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Numéro OT</label>
                                        <input type="number" v-model="filters.numero" class="form-control form-control-sm" placeholder="ex: 10001">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Date de création Min</label>
                                        <input type="date" v-model="filters.dateMin" class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Date de création Max</label>
                                        <input type="date" v-model="filters.dateMax" class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Client Donneur d'ordre</label>
                                        <select v-model="filters.clientId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Expéditeur</label>
                                        <select v-model="filters.expediteurId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Destinataire</label>
                                        <select v-model="filters.destinataireId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.nom }} {{ c.prenom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Type Régime</label>
                                        <select v-model="filters.typeRegimeId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="r in typeRegimes" :key="r.id" :value="r.id">{{ r.code }} - {{ r.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Type de Production</label>
                                        <select v-model="filters.typeProductionId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="p in typeProductions" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Assuré</label>
                                        <select v-model="filters.estAssure" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option value="true">Oui</option>
                                            <option value="false">Non</option>
                                        </select>
                                    </div>
                                </div>
                            </template>

                            <template #cell-numeroOrdreTransit="{ row }">
                                <router-link :to="`/ordres-transit/${row.id}`" class="fw-semibold text-primary text-decoration-none">
                                    {{ row.numeroOrdreTransit }}
                                </router-link>
                            </template>

                            <template #cell-dateCreation="{ row }">
                                {{ formatarDate(row.dateCreation) }}
                            </template>

                            <template #cell-client.nom="{ row }">
                                {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
                            </template>

                            <template #cell-estAssure="{ row }">
                                {{ row.estAssure ? 'Oui' : 'Non' }}
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
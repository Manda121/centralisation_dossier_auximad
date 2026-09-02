<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const route = useRoute()
const datatableRef = ref(null)

const filters = reactive({
    query: '', phaseId: '', statutId: '', clientId: '',
    typeProductionId: '', otId: '', estGroupage: '', dateMin: '', dateMax: '',
    typeDossier: route.query.typeDossier || ''
})

const phases = ref([])
const statuts = ref([])

const peutCreer = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT))

watch(() => route.query.typeDossier, (newVal) => {
    filters.typeDossier = newVal || ''
    datatableRef.value?.refresh(true)
})

function onTypeDossierChange(val) {
    filters.typeDossier = val
    datatableRef.value?.refresh(true)
}

onMounted(async () => {
    try {
        const [ph, st] = await Promise.all([
            DossierService.getAllPhases(),
            DossierService.getAllStatuts()
        ])
        phases.value = ph
        statuts.value = st
    } catch (e) {
        console.error('Erreur chargement filtres dossiers', e)
    }
})

const columns = computed(() => {
    const list = [
        { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
        { key: 'dateCreation', label: 'Date', sortable: true },
        { key: 'client.nom', label: 'Client', sortable: false },
        { key: 'phaseDossier.nom', label: 'Phase', sortable: false }
    ]
    if (filters.typeDossier === 'INTERAGENCE' || filters.typeDossier === 'MERE' || filters.typeDossier === 'SOUS_DOSSIER') {
        list.push({ key: 'typeInteragence', label: 'Type Inter-agence', sortable: false })
    } else {
        list.push({ key: 'estGroupage', label: 'Groupage', sortable: true })
    }
    return list
})

async function chargerDossiers(page, size, sortBy, sortDir, filters) {
    return await DossierService.searchDossiers(page, size, sortBy, sortDir, filters)
}

let searchTimeout = null
function declencherRecherche() {
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => datatableRef.value?.refresh(true), 450)
}

function reinitialiserFiltres() {
    Object.keys(filters).forEach(k => { filters[k] = '' })
    datatableRef.value?.refresh(true)
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Dossiers de Transit</h1>
                        <p class="text-secondary mb-0 small">Gestion centralisée des dossiers de dédouanement.</p>
                    </div>
                    <router-link v-if="peutCreer" to="/dossiers/creer" class="btn btn-primary btn-sm">
                        Nouveau Dossier
                    </router-link>
                </div>


                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerDossiers"
                            :filters="filters" default-sort="date_creation" default-sort-dir="desc" :pageSize="15">

                            <template #filters="{ filters: slotFilters }">
                                <div class="row g-2">
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Phase</label>
                                        <select v-model="slotFilters.phaseId" class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Statut</label>
                                        <select v-model="slotFilters.statutId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Type dossier</label>
                                        <select :value="slotFilters.typeDossier"
                                                @change="onTypeDossierChange($event.target.value)"
                                                class="form-select form-select-sm">
                                            <option value="">Tous les dossiers</option>
                                            <option value="INTERAGENCE">Tous les inter-agences</option>
                                            <option value="MERE">Dossiers mère</option>
                                            <option value="SOUS_DOSSIER">Sous-dossiers</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Groupage</label>
                                        <select v-model="slotFilters.estGroupage" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option value="true">Oui</option>
                                            <option value="false">Non</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Date création Min</label>
                                        <input type="date" v-model="slotFilters.dateMin" class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Date création Max</label>
                                        <input type="date" v-model="slotFilters.dateMax" class="form-control form-control-sm">
                                    </div>
                                </div>
                            </template>

                            <template #cell-numeroDossier="{ row }">
                                <router-link :to="`/dossiers/${row.id}`" class="fw-semibold text-primary text-decoration-none">
                                    {{ row.numeroDossier }}
                                </router-link>
                            </template>

                            <template #cell-dateCreation="{ row }">
                                {{ formatarDate(row.dateCreation) }}
                            </template>

                            <template #cell-client.nom="{ row }">
                                {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
                            </template>

                            <template #cell-estGroupage="{ row }">
                                {{ row.estGroupage ? 'Groupage' : 'FCL' }}
                            </template>

                            <template #cell-typeInteragence="{ row }">
                                <span v-if="row.estDossierMere" class="badge bg-primary text-white">
                                    Mère
                                </span>
                                <span v-else-if="row.estSousDossier" class="badge bg-info text-dark" :title="row.motifInteragence">
                                    Sous-dossier
                                    <span v-if="row.numeroDossierMere" class="small d-block text-muted">
                                        (de {{ row.numeroDossierMere }})
                                    </span>
                                </span>
                                <span v-else class="text-secondary">—</span>
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
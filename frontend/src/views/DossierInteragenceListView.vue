<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const datatableRef = ref(null)
const aujourdhui = new Date().toISOString().split('T')[0]

const filters = reactive({
    query: '',
    phaseId: '',
    statutId: '',
    clientId: '',
    typeProductionId: '',
    otId: '',
    estGroupage: '',
    dateMin: aujourdhui,
    dateMax: aujourdhui,
    typeDossier: 'INTERAGENCE'
})

const phases = ref([])
const statuts = ref([])

const peutCreer = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT))

onMounted(async () => {
    try {
        const [ph, st] = await Promise.all([
            DossierService.getAllPhases(),
            DossierService.getAllStatuts()
        ])
        phases.value = ph
        statuts.value = st
    } catch (e) {
        console.error('Erreur chargement filtres dossiers inter-agences', e)
    }
})

const columns = [
    { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
    { key: 'dateCreation', label: 'Date', sortable: true },
    { key: 'client.nom', label: 'Client', sortable: false },
    { key: 'phaseDossier.nom', label: 'Phase', sortable: false },
    { key: 'typeInteragence', label: 'Type Inter-agence', sortable: false,
        get: row => row.estDossierMere ? 'Dossier Mère' : row.estSousDossier ? 'Sous-dossier' : 'Inter-agence' }
]

async function chargerDossiers(page, size, sortBy, sortDir, queryFilters) {
    // S'assurer que le filtre typeDossier reste INTERAGENCE si non spécifié différemment
    const activeFilters = { ...queryFilters }
    if (!activeFilters.typeDossier) {
        activeFilters.typeDossier = 'INTERAGENCE'
    }
    return await DossierService.searchDossiers(page, size, sortBy, sortDir, activeFilters)
}

function reinitialiserFiltres() {
    filters.query = ''
    filters.phaseId = ''
    filters.statutId = ''
    filters.clientId = ''
    filters.typeProductionId = ''
    filters.otId = ''
    filters.estGroupage = ''
    filters.dateMin = aujourdhui
    filters.dateMax = aujourdhui
    filters.typeDossier = 'INTERAGENCE'
    datatableRef.value?.refresh(true)
}

function appliquerAujourdhui() {
    filters.dateMin = aujourdhui
    filters.dateMax = aujourdhui
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
                <!-- En-tête de page -->
                <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Dossiers Inter-Agences</h1>
                        <p class="text-secondary mb-0 small">Liste et suivi des dossiers de transit partagés (Dossiers
                            Mères & Sous-dossiers).</p>
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-primary btn-sm" @click="appliquerAujourdhui">
                            📅 Dossiers du jour
                        </button>
                        <button class="btn btn-outline-secondary btn-sm" @click="reinitialiserFiltres">
                            🔄 Réinitialiser
                        </button>
                    </div>
                </div>

                <!-- Carte principale -->
                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerDossiers" export-title="Dossiers inter-agences"
                            :filters="filters" default-sort="date_creation" default-sort-dir="desc" :pageSize="15">

                            <template #filters="{ filters: slotFilters }">
                                <div class="row g-2">
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Catégorie
                                            Inter-agence</label>
                                        <select v-model="slotFilters.typeDossier" class="form-select form-select-sm">
                                            <option value="INTERAGENCE">Tous les inter-agences</option>
                                            <option value="MERE">Dossiers Mères uniquement</option>
                                            <option value="SOUS_DOSSIER">Sous-dossiers uniquement</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Phase</label>
                                        <select v-model="slotFilters.phaseId" class="form-select form-select-sm">
                                            <option value="">Toutes les phases</option>
                                            <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Statut</label>
                                        <select v-model="slotFilters.statutId" class="form-select form-select-sm">
                                            <option value="">Tous les statuts</option>
                                            <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
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
                                        <label class="form-label small fw-semibold text-secondary">Date création
                                            Min</label>
                                        <input type="date" v-model="slotFilters.dateMin"
                                            class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Date création
                                            Max</label>
                                        <input type="date" v-model="slotFilters.dateMax"
                                            class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Recherche</label>
                                        <input type="text" v-model="slotFilters.query"
                                            class="form-control form-control-sm"
                                            placeholder="N° dossier, nom client, port...">
                                    </div>
                                </div>
                            </template>

                            <template #cell-numeroDossier="{ row }">
                                <router-link :to="`/dossiers/${row.id}`"
                                    class="fw-semibold text-primary text-decoration-none">
                                    {{ row.numeroDossier }}
                                </router-link>
                            </template>

                            <template #cell-dateCreation="{ row }">
                                {{ formatarDate(row.dateCreation) }}
                            </template>

                            <template #cell-client.nom="{ row }">
                                {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
                            </template>

                            <template #cell-typeInteragence="{ row }">
                                <span v-if="row.estDossierMere" class="badge bg-primary text-white">
                                    Dossier Mère
                                </span>
                                <span v-else-if="row.estSousDossier" class="badge bg-info text-dark"
                                    :title="row.motifInteragence">
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

<style scoped></style>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h3 fw-bold text-primary mb-1">Dossiers de Transit</h1>
                        <p class="text-secondary mb-0">Gestion centralisée des dossiers de dédouanement.</p>
                    </div>
                    <router-link to="/dossiers/creer" class="btn btn-primary d-flex align-items-center gap-2">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                            viewBox="0 0 16 16">
                            <path
                                d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z" />
                        </svg>
                        Nouveau Dossier
                    </router-link>
                </div>

                <!-- Barre recherche -->
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
                                    <input type="text" v-model="filters.query" class="form-control border-start-0 ps-0"
                                        placeholder="Rechercher par N° Dossier, Client, Pour compte..."
                                        @input="declencherRecherche">
                                </div>
                            </div>
                            <div class="col-md-3 text-end">
                                <button class="btn btn-link text-muted text-decoration-none btn-sm"
                                    @click="reinitialiserFiltres">Effacer tout</button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card border-0 shadow-sm p-4">
                    <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerDossiers"
                        :filters="filters" default-sort="date_creation" default-sort-dir="desc" :pageSize="15">

                        <template #filters="{ filters }">
                            <div class="row g-3">
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Phase</label>
                                    <select v-model="filters.phaseId" class="form-select form-select-sm">
                                        <option value="">Toutes</option>
                                        <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Statut</label>
                                    <select v-model="filters.statutId" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Date création Min</label>
                                    <input type="date" v-model="filters.dateMin" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Date création Max</label>
                                    <input type="date" v-model="filters.dateMax" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-3">
                                    <label class="form-label text-xs fw-semibold">Groupage</label>
                                    <select v-model="filters.estGroupage" class="form-select form-select-sm">
                                        <option value="">Tous</option>
                                        <option value="true">Oui</option>
                                        <option value="false">Non</option>
                                    </select>
                                </div>
                            </div>
                        </template>

                        <template #cell-numeroDossier="{ row }">
                            <router-link :to="`/dossiers/${row.id}`" class="fw-semibold text-primary">
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
                            <span
                                :class="row.estGroupage ? 'badge bg-info-subtle text-info' : 'badge bg-secondary-subtle text-secondary'">
                                {{ row.estGroupage ? 'Groupage' : 'FCL' }}
                            </span>
                        </template>

                        <template #cell-statutDossier.nom="{ row }">
                            <span :class="obtenirBadgeStatut(row.statutDossier?.nom)">
                                {{ row.statutDossier?.nom || '—' }}
                            </span>
                        </template>

                        <template #actions="{ row }">
                            <router-link :to="`/dossiers/${row.id}`" class="btn btn-secondary btn-xs">
                                Consulter &rarr;
                            </router-link>
                        </template>
                    </DataTable>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'

const datatableRef = ref(null)

const filters = reactive({
    query: '', phaseId: '', statutId: '', clientId: '',
    typeProductionId: '', otId: '', estGroupage: '', dateMin: '', dateMax: ''
})

const phases = ref([])
const statuts = ref([])

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

const columns = [
    { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
    { key: 'dateCreation', label: 'Date', sortable: true },
    { key: 'client.nom', label: 'Client', sortable: false },
    { key: 'typeProduction.nom', label: 'Production', sortable: false },
    { key: 'phaseDossier.nom', label: 'Phase', sortable: false },
    { key: 'estGroupage', label: 'Type', sortable: true },
    { key: 'statutDossier.nom', label: 'Statut', sortable: false }
]

async function chargerDossiers(page, size, sortBy, sortDir, filters) {
    return await DossierService.searchDossiers(page, size, sortBy, sortDir, filters)
}

let searchTimeout = null
function declencherRecherche() {
    clearTimeout(searchTimeout)
    searchTimeout = setTimeout(() => datatableRef.value?.refresh(), 450)
}

function reinitialiserFiltres() {
    Object.keys(filters).forEach(k => { filters[k] = '' })
    datatableRef.value?.refresh()
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function obtenirBadgeStatut(nom) {
    if (!nom) return 'badge bg-secondary-subtle text-secondary'
    switch (nom.toLowerCase()) {
        case 'ouvert': return 'badge bg-success-subtle text-success border border-success-subtle'
        case 'clôturé': case 'cloture': return 'badge bg-secondary-subtle text-secondary'
        case 'en litige': return 'badge bg-danger-subtle text-danger border border-danger-subtle'
        default: return 'badge bg-warning-subtle text-warning border border-warning-subtle'
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

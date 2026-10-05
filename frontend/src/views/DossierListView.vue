<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'
import AgenceService from '../services/AgenceService'
import ClientService from '../services/ClientService'
import OrdreTransitService from '../services/OrdreTransitService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const route = useRoute()
const datatableRef = ref(null)
const aujourdhui = new Date().toLocaleDateString('en-CA')

const filters = reactive({
    agenceId: '', numeroDossier: '', clientNom: '', cpClient: '', numeroOt: '',
    paysOrigineId: '', paysDestinationId: '', incontermeId: '', pourCompte: '',
    depart: '', adresseLivraison: '', lieuPaiementFret: '',
    estPrepaye: '', estProvisionne: '', provisionMin: '', provisionMax: '',
    echeanceMin: aujourdhui, echeanceMax: aujourdhui, dateLivraisonMin: aujourdhui, dateLivraisonMax: aujourdhui,
    query: '', phaseId: '', statutId: '', clientId: '',
    typeProductionId: '', otId: '', estGroupage: '', dateMin: aujourdhui, dateMax: aujourdhui,
    typeDossier: route.query.typeDossier || '',
})

const phases = ref([])
const statuts = ref([])
const agences = ref([])
const typeProductions = ref([])
const pays = ref([])
const incontermes = ref([])
const erreurFiltres = ref('')
const peutVoirToutesAgences = computed(() => PermissionService.peutLireToutesAgencesOT())
const agenceUtilisateur = JSON.parse(localStorage.getItem('auximad_user_info') || '{}').idAgence
const agencesDisponibles = computed(() => peutVoirToutesAgences.value
    ? agences.value : agences.value.filter(a => a.id === agenceUtilisateur))
const filtresTexte = [
    { key: 'numeroDossier', label: 'N° dossier' },
    { key: 'clientNom', label: 'Client (nom ou prénom)' },
    { key: 'cpClient', label: 'CP client' },
    { key: 'pourCompte', label: 'Pour compte' },
    { key: 'depart', label: 'Lieu de départ' },
    { key: 'adresseLivraison', label: 'Adresse de livraison' },
    { key: 'lieuPaiementFret', label: 'Lieu de paiement du fret' }
]
const filtresDates = [
    { key: 'echeanceMin', label: 'Échéance du' },
    { key: 'echeanceMax', label: 'Échéance au' },
    { key: 'dateLivraisonMin', label: 'Livraison du' },
    { key: 'dateLivraisonMax', label: 'Livraison au' }
]

const peutCreer = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT))

watch(() => route.query.typeDossier, (newVal) => {
    filters.typeDossier = newVal || ''
    datatableRef.value?.refresh(true)
})

onMounted(async () => {
    try {
        const chargements = [
            [DossierService.getAllPhases(), phases],
            [DossierService.getAllStatuts(), statuts],
            [AgenceService.getAllAgences(), agences],
            [OrdreTransitService.getAllTypeProductions(), typeProductions],
            [ClientService.getAllPays(), pays],
            [DossierService.getAllIncontermes(), incontermes]
        ]
        const resultats = await Promise.allSettled(chargements.map(([promesse]) => promesse))
        resultats.forEach((resultat, index) => {
            if (resultat.status === 'fulfilled') chargements[index][1].value = resultat.value
            else erreurFiltres.value = 'Certains choix de filtres sont indisponibles. Rechargez la page pour réessayer.'
        })
    } catch (e) {
        console.error('Erreur chargement filtres dossiers', e)
    }
})

const columns = computed(() => {
    const list = [
        { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
        { key: 'dateCreation', label: 'Date', sortable: true },
        { key: 'client.nom', label: 'Client', sortable: false },
        { key: 'agence.nom', label: 'Agence', sortable: false },
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
                                <p v-if="erreurFiltres" class="alert alert-warning small mb-2">{{ erreurFiltres }}</p>
                                <div class="row g-2">
                                    <div v-for="champ in filtresTexte" :key="champ.key" class="col-md-3">
                                        <label :for="`filtre-${champ.key}`" class="form-label small fw-semibold text-secondary">{{ champ.label }}</label>
                                        <input :id="`filtre-${champ.key}`" v-model="slotFilters[champ.key]" type="text" class="form-control form-control-sm" placeholder="Mots ou débuts de mots…">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary" for="filtre-agence">Agence</label>
                                        <select id="filtre-agence" v-model="slotFilters.agenceId" class="form-select form-select-sm">
                                            <option value="">{{ peutVoirToutesAgences ? 'Toutes les agences' : 'Mon agence' }}</option>
                                            <option v-for="a in agencesDisponibles" :key="a.id" :value="a.id">{{ a.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary" for="filtre-production">Type de production</label>
                                        <select id="filtre-production" v-model="slotFilters.typeProductionId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="p in typeProductions" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary" for="filtre-ot">N° ordre de transit</label>
                                        <input id="filtre-ot" v-model="slotFilters.numeroOt" type="number" min="1" step="1" class="form-control form-control-sm">
                                    </div>
                                    <div v-for="champ in [{ key: 'paysOrigineId', label: 'Pays d’origine' }, { key: 'paysDestinationId', label: 'Pays de destination' }]" :key="champ.key" class="col-md-3">
                                        <label :for="`filtre-${champ.key}`" class="form-label small fw-semibold text-secondary">{{ champ.label }}</label>
                                        <select :id="`filtre-${champ.key}`" v-model="slotFilters[champ.key]" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="p in pays" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary" for="filtre-incoterm">Incoterm</label>
                                        <select id="filtre-incoterm" v-model="slotFilters.incontermeId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="i in incontermes" :key="i.id" :value="i.id">{{ i.abreviation }} — {{ i.nom }}</option>
                                        </select>
                                    </div>
                                    <div v-for="champ in [{ key: 'estPrepaye', label: 'Prépayé' }, { key: 'estProvisionne', label: 'Provisionné' }]" :key="champ.key" class="col-md-3">
                                        <label :for="`filtre-${champ.key}`" class="form-label small fw-semibold text-secondary">{{ champ.label }}</label>
                                        <select :id="`filtre-${champ.key}`" v-model="slotFilters[champ.key]" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option value="true">Oui</option>
                                            <option value="false">Non</option>
                                        </select>
                                    </div>
                                    <div v-for="champ in [{ key: 'provisionMin', label: 'Provision demandée min.' }, { key: 'provisionMax', label: 'Provision demandée max.' }]" :key="champ.key" class="col-md-3">
                                        <label :for="`filtre-${champ.key}`" class="form-label small fw-semibold text-secondary">{{ champ.label }}</label>
                                        <input :id="`filtre-${champ.key}`" v-model="slotFilters[champ.key]" type="number" min="0" step="0.01" class="form-control form-control-sm">
                                    </div>
                                    <div v-for="champ in filtresDates" :key="champ.key" class="col-md-3">
                                        <label :for="`filtre-${champ.key}`" class="form-label small fw-semibold text-secondary">{{ champ.label }}</label>
                                        <input :id="`filtre-${champ.key}`" v-model="slotFilters[champ.key]" type="date" class="form-control form-control-sm">
                                    </div>
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
                                        <select v-model="slotFilters.typeDossier"
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

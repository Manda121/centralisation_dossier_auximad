<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'
import ConteneurService from '../services/ConteneurService'
import ModalConteneur from '../components/conteneur/ModalConteneur.vue'
import { useNotification } from '../composables/useNotification'

const { notifyError } = useNotification()
const datatableRef = ref(null)

// Date du jour par défaut (Format YYYY-MM-DD)
const todayStr = new Date().toISOString().split('T')[0]

const filters = reactive({
  query: '',
  phaseId: '',
  statutId: '',
  typeProductionId: '',
  dateMin: todayStr,
  dateMax: todayStr
})

const phases = ref([])
const statuts = ref([])

// Pour la modale d'assignation
const showModal = ref(false)
const dossierSelectionne = ref(null)
const conteneurAEditer = ref(null)

// Map pour stocker la liste des conteneurs par dossierId
const conteneursParDossier = ref({})
const loadingConteneurs = ref(false)

// Gestion du déroulement flottant (dropdown/popover)
const expandedRows = reactive({})

function toggleExpand(dossierId, e) {
  if (e) e.stopPropagation()
  const currentlyOpen = !!expandedRows[dossierId]
  Object.keys(expandedRows).forEach(k => { expandedRows[k] = false })
  expandedRows[dossierId] = !currentlyOpen
}

function fermerTousDropdowns() {
  Object.keys(expandedRows).forEach(k => { expandedRows[k] = false })
}

async function chargerTousLesConteneurs() {
  loadingConteneurs.value = true
  try {
    const tous = await ConteneurService.getTousConteneurs()
    const map = {}
    tous.forEach(c => {
      const listDossiers = (c.dossiers && c.dossiers.length > 0) ? c.dossiers : (c.dossier ? [c.dossier] : [])
      listDossiers.forEach(d => {
        if (d && d.id) {
          if (!map[d.id]) {
            map[d.id] = []
          }
          if (!map[d.id].some(item => item.id === c.id)) {
            map[d.id].push(c)
          }
        }
      })
    })
    conteneursParDossier.value = map
  } catch (e) {
    console.error("Erreur de chargement des conteneurs", e)
  } finally {
    loadingConteneurs.value = false
  }
}

onMounted(async () => {
  window.addEventListener('click', fermerTousDropdowns)
  try {
    const [ph, st] = await Promise.all([
      DossierService.getAllPhases(),
      DossierService.getAllStatuts()
    ])
    phases.value = ph
    statuts.value = st
  } catch (e) {
    console.error('Erreur chargement des référentiels', e)
  }
  await chargerTousLesConteneurs()
})

onUnmounted(() => {
  window.removeEventListener('click', fermerTousDropdowns)
})

// Colonnes demandées : numéro dossier, client, date embarquement, date arrivée, compagnie, description du type production, conteneurs, actions
const columns = [
  { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
  { key: 'client.nom', label: 'Client', sortable: false },
  { key: 'dateEmbarquement', label: 'Date Embarquement', sortable: false },
  { key: 'dateArrivee', label: 'Date Arrivée', sortable: false },
  { key: 'compagnie', label: 'Compagnie', sortable: false },
  { key: 'typeProduction.description', label: 'Description Type Prod.', sortable: false },
  { key: 'conteneurs', label: 'Conteneurs Assignés', sortable: false },
  { key: 'actions', label: 'Actions', sortable: false }
]

async function chargerDossiers(page, size, sortBy, sortDir, currentFilters) {
  return await DossierService.searchDossiers(page, size, sortBy, sortDir, currentFilters)
}

function estEligible(dossier) {
  return ConteneurService.estEligibleConteneur(dossier)
}

function ouvrirModalAssignation(dossier, conteneur = null) {
  dossierSelectionne.value = dossier
  conteneurAEditer.value = conteneur
  showModal.value = true
}

async function onConteneurEnregistre() {
  await chargerTousLesConteneurs()
  datatableRef.value?.refresh(false)
}

async function supprimerConteneur(cId, dossierId = null) {
    try {
        await ConteneurService.supprimerConteneur(cId, dossierId)
        await chargerTousLesConteneurs()
    } catch (e) {
        if (e?.code === 'ERR_CANCELED') return
        notifyError(e.response?.data?.message || 'Erreur de suppression du conteneur')
    }
}

function formatarDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function reinitialiserFiltreDate() {
  filters.dateMin = ''
  filters.dateMax = ''
  datatableRef.value?.refresh(true)
}

function filtrerAujourdhui() {
  filters.dateMin = todayStr
  filters.dateMax = todayStr
  datatableRef.value?.refresh(true)
}
</script>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />

    <main class="flex-grow-1 p-4" style="margin-left: 280px;">
      <div class="container-fluid">
        <!-- Titre & Indicateur de chargement des conteneurs -->
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h1 class="h4 fw-bold text-dark mb-1">Gestion & Assignation des Conteneurs</h1>
            <p class="text-secondary mb-0 small">
              Filtre automatique sur les dossiers du jour. Assignation simplifiée des conteneurs maritime.
            </p>
          </div>

          <!-- Chargement des conteneurs -->
          <div v-if="loadingConteneurs" class="d-flex align-items-center gap-2 bg-white px-3 py-2 border rounded shadow-sm">
            <span class="spinner-border spinner-border-sm text-primary" role="status"></span>
            <span class="small fw-semibold text-primary">Chargement des conteneurs…</span>
          </div>
        </div>

        <!-- Carte Table -->
        <div class="card border shadow-sm">
          <div class="card-body p-3">
            <DataTable 
              ref="datatableRef" 
              :columns="columns" 
              :fetch-function="chargerDossiers"
              :filters="filters" 
              default-sort="date_creation" 
              default-sort-dir="desc" 
              :pageSize="15"
            >
              <!-- Filtres -->
              <template #filters="{ filters: slotFilters }">
                <div class="row g-2 align-items-end">
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Date dossier</label>
                    <input type="date" v-model="slotFilters.dateMin" class="form-control form-control-sm" />
                  </div>
                  <div class="col-md-3">
                    <div class="d-flex gap-1">
                      <button 
                        type="button" 
                        class="btn btn-sm btn-outline-primary w-100" 
                        :class="{ active: slotFilters.dateMin === todayStr }"
                        @click="filtrerAujourdhui"
                      >
                        Aujourd'hui
                      </button>
                      <button 
                        type="button" 
                        class="btn btn-sm btn-outline-secondary w-100" 
                        @click="reinitialiserFiltreDate"
                      >
                        Tous les jours
                      </button>
                    </div>
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
                </div>
              </template>

              <!-- Colonne 1: N° Dossier -->
              <template #cell-numeroDossier="{ row }">
                <router-link :to="`/dossiers/${row.id}`" class="fw-semibold text-primary text-decoration-none">
                  {{ row.numeroDossier }}
                </router-link>
              </template>

              <!-- Colonne 2: Client -->
              <template #cell-client.nom="{ row }">
                {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
              </template>

              <!-- Colonne 3: Date Embarquement -->
              <template #cell-dateEmbarquement="{ row }">
                {{ formatarDate(row.ordreTransit?.contratTransport?.datePrevue || row.echeance) }}
              </template>

              <!-- Colonne 4: Date Arrivée -->
              <template #cell-dateArrivee="{ row }">
                {{ formatarDate(row.ordreTransit?.contratTransport?.dateArrivee || row.dateLivraison) }}
              </template>

              <!-- Colonne 5: Compagnie -->
              <template #cell-compagnie="{ row }">
                {{ row.ordreTransit?.contratTransport?.compagnie?.nom || row.compagnie || '—' }}
              </template>

              <!-- Colonne 6: Description du type production -->
              <template #cell-typeProduction.description="{ row }">
                <span class="small text-dark">
                  {{ row.typeProduction?.description || row.typeProduction?.nom || row.ordreTransit?.typeProduction?.description || '—' }}
                </span>
              </template>

              <!-- Colonne 7: Conteneurs Assignés (Déroulement flottant sans impacter la hauteur des lignes) -->
              <template #cell-conteneurs="{ row }">
                <div v-if="conteneursParDossier[row.id] && conteneursParDossier[row.id].length > 0" class="position-relative">
                  
                  <!-- En-tête tronqué en une ligne fixe -->
                  <div 
                    class="d-flex align-items-center gap-1 cursor-pointer py-1 px-2 border rounded bg-white hover-bg-light text-nowrap"
                    style="max-width: 220px; font-size: 0.82rem;"
                    @click="toggleExpand(row.id, $event)"
                    title="Cliquer pour dérouler la liste des conteneurs"
                  >
                    <span class="text-dark text-truncate">
                      {{ conteneursParDossier[row.id][0].numeroContainer }}
                      <span class="text-secondary">({{ conteneursParDossier[row.id][0].typeContainer?.nom || 'Conteneur' }})</span>
                    </span>
                    <span v-if="conteneursParDossier[row.id].length > 1" class="text-primary fw-bold ms-auto" style="font-size: 0.75rem;">
                      ... (+{{ conteneursParDossier[row.id].length - 1 }}) ▾
                    </span>
                    <span v-else class="text-secondary ms-auto" style="font-size: 0.7rem;">▾</span>
                  </div>

                  <!-- Menu déroulant flottant au clic -->
                  <div 
                    v-if="expandedRows[row.id]" 
                    class="position-absolute start-0 top-100 mt-1 bg-white border rounded shadow-lg p-2 dropdown-container"
                    style="z-index: 1050; min-width: 280px; max-height: 220px; overflow-y: auto;"
                    @click.stop
                  >
                    <div class="d-flex justify-content-between align-items-center pb-2 mb-2 border-bottom">
                      <span class="fw-semibold text-dark small">Conteneurs assignés ({{ conteneursParDossier[row.id].length }})</span>
                      <button 
                        type="button" 
                        class="btn-close p-0" 
                        style="font-size: 0.65rem;" 
                        @click="expandedRows[row.id] = false"
                      ></button>
                    </div>

                    <div 
                      v-for="c in conteneursParDossier[row.id]" 
                      :key="c.id"
                      class="py-1 px-2 mb-1 border-bottom-subtle d-flex justify-content-between align-items-center gap-2 hover-bg-light rounded small"
                    >
                      <div class="text-truncate">
                        <strong class="text-dark">{{ c.numeroContainer }}</strong>
                        <span class="text-secondary ms-1">({{ c.typeContainer?.nom || 'Conteneur' }})</span>
                        <div v-if="c.description" class="text-muted" style="font-size: 0.7rem;">
                          {{ c.description }}
                        </div>
                      </div>

                      <div class="d-flex align-items-center gap-2 flex-shrink-0">
                        <button 
                          class="btn btn-link btn-sm p-0 text-primary text-decoration-none" 
                          style="font-size: 0.75rem;" 
                          @click="ouvrirModalAssignation(row, c); expandedRows[row.id] = false"
                        >
                          Edit
                        </button>
                        <button 
                          class="btn btn-link btn-sm p-0 text-danger text-decoration-none" 
                          style="font-size: 0.75rem;" 
                          @click="supprimerConteneur(c.id, row.id)"
                        >
                          Suppr.
                        </button>
                      </div>
                    </div>
                  </div>

                </div>
                
                <span v-else class="text-muted small fst-italic">—</span>
              </template>

              <!-- Colonne 8: Actions -->
              <template #cell-actions="{ row }">
                <button 
                  v-if="estEligible(row)"
                  class="btn btn-outline-primary btn-sm d-flex align-items-center gap-1"
                  @click="ouvrirModalAssignation(row)"
                >
                  + Assigner conteneur
                </button>
                <button 
                  v-else
                  class="btn btn-sm btn-light border text-muted opacity-75" 
                  disabled 
                  title="Assignation impossible : le dossier n'est pas maritime"
                >
                  Non maritime
                </button>
              </template>
            </DataTable>
          </div>
        </div>
      </div>
    </main>

    <!-- Modale d'assignation -->
    <ModalConteneur
      :show="showModal"
      :dossier="dossierSelectionne"
      :conteneurAEditer="conteneurAEditer"
      @close="showModal = false"
      @saved="onConteneurEnregistre"
    />
  </div>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
.hover-bg-light:hover {
  background-color: #f8fafc;
}
.border-bottom-subtle {
  border-bottom: 1px dashed #e2e8f0;
}
.dropdown-container {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}
</style>

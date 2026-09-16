<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DossierService from '../services/DossierService'
import ConteneurService from '../services/ConteneurService'
import ModalConteneur from '../components/conteneur/ModalConteneur.vue'

const datatableRef = ref(null)

const filters = reactive({
  query: '',
  phaseId: '',
  statutId: '',
  typeProductionId: '',
  filtreMaritime: '' // 'maritime', 'non_maritime', ou ''
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

async function chargerTousLesConteneurs() {
  loadingConteneurs.value = true
  try {
    const tous = await ConteneurService.getTousConteneurs()
    const map = {}
    tous.forEach(c => {
      if (c.dossier && c.dossier.id) {
        if (!map[c.dossier.id]) {
          map[c.dossier.id] = []
        }
        map[c.dossier.id].push(c)
      }
    })
    conteneursParDossier.value = map
  } catch (e) {
    console.error("Erreur de chargement des conteneurs", e)
  } finally {
    loadingConteneurs.value = false
  }
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
    console.error('Erreur chargement des référentiels', e)
  }
  await chargerTousLesConteneurs()
})

const columns = [
  { key: 'numeroDossier', label: 'N° Dossier', sortable: true },
  { key: 'dateCreation', label: 'Date', sortable: true },
  { key: 'client.nom', label: 'Client', sortable: false },
  { key: 'modeTransport', label: 'Mode Transport', sortable: false },
  { key: 'conteneurs', label: 'Conteneurs Assignés', sortable: false },
  { key: 'actions', label: 'Actions', sortable: false }
]

async function chargerDossiers(page, size, sortBy, sortDir, currentFilters) {
  // Option de filtrage maritime coté frontend ou backend
  const res = await DossierService.searchDossiers(page, size, sortBy, sortDir, currentFilters)
  return res
}

function estEligible(dossier) {
  return ConteneurService.estEligibleConteneur(dossier)
}

function getModeTransportLabel(dossier) {
  if (!dossier) return 'Non défini'
  return dossier.typeProduction?.modeTransport?.nom || 
         dossier.typeProduction?.nom || 
         dossier.ordreTransit?.typeProduction?.modeTransport?.nom || 
         'Autre'
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

async function supprimerConteneur(cId) {
  if (confirm("Supprimer cette assignation de conteneur ?")) {
    try {
      await ConteneurService.supprimerConteneur(cId)
      await chargerTousLesConteneurs()
    } catch (e) {
      alert(e.response?.data?.message || "Erreur de suppression du conteneur")
    }
  }
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
        <!-- Titre -->
        <div class="d-flex justify-content-between align-items-center mb-4">
          <div>
            <h1 class="h4 fw-bold text-dark mb-1">Gestion & Assignation des Conteneurs</h1>
            <p class="text-secondary mb-0 small">
              Assignez les conteneurs (20', 40', High Cube, Reefer...) aux dossiers de transport maritime.
            </p>
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
                <div class="row g-2">
                  <div class="col-md-4">
                    <label class="form-label small fw-semibold text-secondary">Phase</label>
                    <select v-model="slotFilters.phaseId" class="form-select form-select-sm">
                      <option value="">Toutes les phases</option>
                      <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                    </select>
                  </div>
                  <div class="col-md-4">
                    <label class="form-label small fw-semibold text-secondary">Statut</label>
                    <select v-model="slotFilters.statutId" class="form-select form-select-sm">
                      <option value="">Tous les statuts</option>
                      <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
                    </select>
                  </div>
                </div>
              </template>

              <!-- Cellule N° Dossier -->
              <template #cell-numeroDossier="{ row }">
                <router-link :to="`/dossiers/${row.id}`" class="fw-semibold text-primary text-decoration-none">
                  {{ row.numeroDossier }}
                </router-link>
              </template>

              <!-- Cellule Date -->
              <template #cell-dateCreation="{ row }">
                {{ formatarDate(row.dateCreation) }}
              </template>

              <!-- Cellule Client -->
              <template #cell-client.nom="{ row }">
                {{ row.client ? `${row.client.nom} ${row.client.prenom || ''}` : '—' }}
              </template>

              <!-- Cellule Mode Transport -->
              <template #cell-modeTransport="{ row }">
                <div class="d-flex align-items-center gap-1">
                  <span v-if="estEligible(row)" class="badge bg-success-subtle text-success border border-success px-2 py-1">
                    🚢 Maritime
                  </span>
                  <span v-else class="badge bg-secondary-subtle text-secondary border border-secondary px-2 py-1">
                    ✈️ {{ getModeTransportLabel(row) }}
                  </span>
                </div>
              </template>

              <!-- Cellule Conteneurs -->
              <template #cell-conteneurs="{ row }">
                <div v-if="conteneursParDossier[row.id] && conteneursParDossier[row.id].length > 0" class="d-flex flex-wrap gap-1">
                  <span 
                    v-for="c in conteneursParDossier[row.id]" 
                    :key="c.id" 
                    class="badge bg-dark-subtle text-dark border p-1 px-2 d-inline-flex align-items-center gap-1"
                    style="font-size: 0.75rem;"
                  >
                    📦 <strong>{{ c.numeroContainer }}</strong>
                    <span class="text-secondary">({{ c.typeContainer?.nom || 'Conteneur' }})</span>
                    <button 
                      class="btn-close p-0 ms-1" 
                      style="font-size: 0.6rem;" 
                      title="Supprimer" 
                      @click.stop="supprimerConteneur(c.id)"
                    ></button>
                  </span>
                </div>
                <span v-else class="text-muted small fst-italic">Aucun conteneur</span>
              </template>

              <!-- Cellule Actions -->
              <template #cell-actions="{ row }">
                <button 
                  v-if="estEligible(row)"
                  class="btn btn-outline-primary btn-sm d-flex align-items-center gap-1"
                  @click="ouvrirModalAssignation(row)"
                >
                  <svg viewBox="0 0 16 16" fill="currentColor" width="14" height="14">
                    <path d="M14 1a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1zM2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2z"/>
                    <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4"/>
                  </svg>
                  Assigner conteneur
                </button>
                <button 
                  v-else
                  class="btn btn-sm btn-light border text-muted opacity-75" 
                  disabled 
                  title="Assignation impossible : le dossier n'est pas maritime"
                >
                  🚫 Non maritime
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
</style>

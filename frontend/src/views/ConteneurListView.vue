<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
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
  filtreMaritime: ''
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

// Gestion du déroulement / dépliement des conteneurs dans le tableau
const expandedRows = reactive({})

function toggleExpand(dossierId) {
  expandedRows[dossierId] = !expandedRows[dossierId]
}

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
  return await DossierService.searchDossiers(page, size, sortBy, sortDir, currentFilters)
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
              Assignation simplifiée des conteneurs aux dossiers de dédedouanement maritime.
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
                <div class="d-flex align-items-center gap-1 small">
                  <span v-if="estEligible(row)" class="text-success fw-semibold">
                    🚢 Maritime
                  </span>
                  <span v-else class="text-secondary">
                    ✈️ {{ getModeTransportLabel(row) }}
                  </span>
                </div>
              </template>

              <!-- Cellule Conteneurs (Affichage sobre sans couleur, tronqué avec déroulement au clic) -->
              <template #cell-conteneurs="{ row }">
                <div v-if="conteneursParDossier[row.id] && conteneursParDossier[row.id].length > 0" class="conteneurs-cell">
                  
                  <!-- Vue tronquée en une ligne si non déroulé -->
                  <div 
                    v-if="!expandedRows[row.id]" 
                    class="d-flex align-items-center gap-1 cursor-pointer py-1"
                    @click="toggleExpand(row.id)"
                    title="Cliquer pour dérouler la liste des conteneurs"
                  >
                    <span class="text-dark small text-truncate" style="max-width: 220px;">
                      {{ conteneursParDossier[row.id][0].numeroContainer }}
                      <span class="text-secondary">({{ conteneursParDossier[row.id][0].typeContainer?.nom || 'Conteneur' }})</span>
                    </span>
                    
                    <span v-if="conteneursParDossier[row.id].length > 1" class="text-primary fw-bold small ms-1">
                      ... (+{{ conteneursParDossier[row.id].length - 1 }})
                    </span>
                    <span v-else-if="conteneursParDossier[row.id][0].numeroContainer.length > 20" class="text-primary small">...</span>
                  </div>

                  <!-- Vue déroulée complète -->
                  <div v-else class="py-1">
                    <ul class="list-unstyled mb-1 small text-dark">
                      <li 
                        v-for="c in conteneursParDossier[row.id]" 
                        :key="c.id" 
                        class="py-1 border-bottom-subtle d-flex justify-content-between align-items-center gap-2"
                      >
                        <span>
                          <strong>{{ c.numeroContainer }}</strong>
                          <span class="text-secondary ms-1">({{ c.typeContainer?.nom || 'Conteneur' }})</span>
                        </span>

                        <div class="d-flex align-items-center gap-2">
                          <button 
                            class="btn btn-link btn-sm p-0 text-primary text-decoration-none" 
                            style="font-size: 0.75rem;" 
                            @click.stop="ouvrirModalAssignation(row, c)"
                          >
                            Modifier
                          </button>
                          <button 
                            class="btn btn-link btn-sm p-0 text-danger text-decoration-none" 
                            style="font-size: 0.75rem;" 
                            @click.stop="supprimerConteneur(c.id)"
                          >
                            Supprimer
                          </button>
                        </div>
                      </li>
                    </ul>
                    <a 
                      href="javascript:void(0)" 
                      class="text-secondary small text-decoration-none d-inline-block mt-1" 
                      style="font-size: 0.75rem;" 
                      @click="toggleExpand(row.id)"
                    >
                      ▲ Réduire
                    </a>
                  </div>

                </div>
                
                <span v-else class="text-muted small fst-italic">—</span>
              </template>

              <!-- Cellule Actions -->
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
.conteneurs-cell {
  user-select: none;
}
.border-bottom-subtle {
  border-bottom: 1px dashed #e2e8f0;
}
</style>

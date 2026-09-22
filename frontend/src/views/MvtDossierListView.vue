<template>
  <div class="mvt-container">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h2 class="fw-bold text-dark mb-1">Mouvement de Dossier</h2>
        <p class="text-muted small mb-0">Suivi en temps réel des opérations et avancements des dossiers</p>
      </div>
      <router-link to="/mouvements-dossier/creer" class="btn btn-primary d-flex align-items-center gap-2 shadow-sm">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
          <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4"/>
        </svg>
        <span>Créer un mouvement</span>
      </router-link>
    </div>

    <!-- Filters Card -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body p-4">
        <h6 class="fw-bold text-uppercase text-secondary fs-7 mb-3">Filtres de recherche multi-critères</h6>
        <div class="row g-3">
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Code / Nom Client</label>
            <input type="text" v-model="filters.cpClient" class="form-control form-control-sm" placeholder="Ex: CLI-001" @keyup.enter="loadMouvements(0)" />
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Numéro de Dossier</label>
            <input type="text" v-model="filters.numeroDossier" class="form-control form-control-sm" placeholder="Ex: DOS-2026-001" @keyup.enter="loadMouvements(0)" />
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Date Min</label>
            <input type="date" v-model="filters.dateMin" class="form-control form-control-sm" />
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Date Max</label>
            <input type="date" v-model="filters.dateMax" class="form-control form-control-sm" />
          </div>

          <div class="col-md-3">
            <label class="form-label small fw-semibold">Agence</label>
            <select v-model="filters.agenceId" class="form-select form-select-sm">
              <option :value="null">Toutes les agences</option>
              <option v-for="a in agences" :key="a.id" :value="a.id">{{ a.nom }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Type de Production</label>
            <select v-model="filters.typeProductionId" class="form-select form-select-sm">
              <option :value="null">Tous les types</option>
              <option v-for="tp in typesProduction" :key="tp.id" :value="tp.id">{{ tp.nom }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Phase Dossier</label>
            <select v-model="filters.phaseId" class="form-select form-select-sm">
              <option :value="null">Toutes les phases</option>
              <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label small fw-semibold">Statut Dossier</label>
            <select v-model="filters.statutId" class="form-select form-select-sm">
              <option :value="null">Tous les statuts</option>
              <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
            </select>
          </div>
        </div>

        <div class="d-flex justify-content-end gap-2 mt-3">
          <button class="btn btn-outline-secondary btn-sm" @click="resetFilters">
            Réinitialiser
          </button>
          <button class="btn btn-primary btn-sm d-flex align-items-center gap-1" @click="loadMouvements(0)">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
              <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001q.044.06.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1 1 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0"/>
            </svg>
            Rechercher
          </button>
        </div>
      </div>
    </div>

    <!-- Data Table -->
    <div class="card border-0 shadow-sm">
      <div class="card-body p-0">
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Chargement...</span>
          </div>
          <p class="text-muted small mt-2">Chargement des mouvements...</p>
        </div>

        <div v-else-if="mouvements.length === 0" class="text-center py-5">
          <p class="text-muted mb-0">Aucun mouvement de dossier trouvé pour ces critères.</p>
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="bg-light text-muted small text-uppercase">
              <tr>
                <th class="ps-4">Date Mouvement</th>
                <th>Dossier</th>
                <th>Client</th>
                <th>Opération</th>
                <th>Phase</th>
                <th>Statut</th>
                <th>Utilisateur</th>
                <th class="pe-4 text-end">Document</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="mvt in mouvements" :key="mvt.id">
                <td class="ps-4 fw-semibold small">
                  {{ formatDate(mvt.dateMvt) }}
                </td>
                <td>
                  <router-link v-if="mvt.dossier" :to="`/dossiers/${mvt.dossier.id}`" class="text-decoration-none fw-bold text-primary">
                    {{ mvt.dossier.numeroDossier }}
                  </router-link>
                  <span v-else class="text-muted">-</span>
                </td>
                <td class="small">
                  <span v-if="mvt.dossier?.client" class="fw-semibold">
                    {{ mvt.dossier.client.cpClient }} - {{ mvt.dossier.client.nom }}
                  </span>
                  <span v-else class="text-muted">-</span>
                </td>
                <td>
                  <span class="badge bg-info-subtle text-info-emphasis border border-info-subtle px-2 py-1">
                    {{ mvt.operation ? mvt.operation.description : 'Mouvement manuel' }}
                  </span>
                </td>
                <td class="small">
                  <div class="d-flex align-items-center gap-1">
                    <span v-if="mvt.anciennePhase" class="badge bg-light text-dark border">{{ mvt.anciennePhase.nom }}</span>
                    <span v-if="mvt.anciennePhase && mvt.nouvellePhase" class="text-muted">➔</span>
                    <span v-if="mvt.nouvellePhase" class="badge bg-primary-subtle text-primary border border-primary-subtle">{{ mvt.nouvellePhase.nom }}</span>
                  </div>
                </td>
                <td class="small">
                  <div class="d-flex align-items-center gap-1">
                    <span v-if="mvt.ancienStatut" class="badge bg-light text-dark border">{{ mvt.ancienStatut.nom }}</span>
                    <span v-if="mvt.ancienStatut && mvt.nouveauStatut" class="text-muted">➔</span>
                    <span v-if="mvt.nouveauStatut" class="badge bg-success-subtle text-success border border-success-subtle">{{ mvt.nouveauStatut.nom }}</span>
                  </div>
                </td>
                <td class="small text-muted">
                  {{ mvt.utilisateur ? (mvt.utilisateur.nomComplet || mvt.utilisateur.nomUtilisateur) : 'Système' }}
                </td>
                <td class="pe-4 text-end">
                  <a v-if="mvt.document" :href="getFileUrl(mvt.document.cheminFichier)" target="_blank" class="btn btn-outline-primary btn-sm px-2 py-0" title="Voir le document">
                    📎 {{ mvt.document.nomFichier || 'Fichier' }}
                  </a>
                  <span v-else class="text-muted small">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="d-flex justify-content-between align-items-center p-3 border-top">
          <div class="small text-muted">
            Page {{ currentPage + 1 }} sur {{ totalPages }} (Total : {{ totalElements }} mouvements)
          </div>
          <div class="d-flex align-items-center gap-2">
            <button class="btn btn-sm btn-outline-secondary" :disabled="currentPage === 0" @click="loadMouvements(currentPage - 1)">
              Précédent
            </button>
            <button v-for="p in pageNumbers" :key="p" class="btn btn-sm" :class="p === currentPage ? 'btn-primary' : 'btn-outline-secondary'" @click="loadMouvements(p)">
              {{ p + 1 }}
            </button>
            <button class="btn btn-sm btn-outline-secondary" :disabled="currentPage >= totalPages - 1" @click="loadMouvements(currentPage + 1)">
              Suivant
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import MvtDossierService from '../services/MvtDossierService'
import AdminService from '../services/AdminService'
import DossierService from '../services/DossierService'

const mouvements = ref([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(15)
const totalPages = ref(0)
const totalElements = ref(0)

const agences = ref([])
const typesProduction = ref([])
const phases = ref([])
const statuts = ref([])

const getTodayDateStr = () => {
  const d = new Date()
  return d.toISOString().split('T')[0]
}

const filters = ref({
  cpClient: '',
  numeroDossier: '',
  dateMin: getTodayDateStr(),
  dateMax: getTodayDateStr(),
  agenceId: null,
  typeProductionId: null,
  phaseId: null,
  statutId: null
})

const loadMouvements = async (page = 0) => {
  loading.value = true
  currentPage.value = page
  try {
    const res = await MvtDossierService.searchMouvements(
      page,
      pageSize.value,
      'dateMvt',
      'desc',
      filters.value
    )
    mouvements.value = res.content || []
    totalPages.value = res.totalPages || 0
    totalElements.value = res.totalElements || 0
  } catch (err) {
    console.error('Erreur lors du chargement des mouvements:', err)
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.value = {
    cpClient: '',
    numeroDossier: '',
    dateMin: getTodayDateStr(),
    dateMax: getTodayDateStr(),
    agenceId: null,
    typeProductionId: null,
    phaseId: null,
    statutId: null
  }
  loadMouvements(0)
}

const loadReferentiels = async () => {
  try {
    const [agRes, tpRes, phRes, stRes] = await Promise.all([
      AdminService.agences.lister(),
      AdminService.typesProduction.lister(),
      DossierService.getAllPhases(),
      DossierService.getAllStatuts()
    ])
    agences.value = agRes || []
    typesProduction.value = tpRes || []
    phases.value = phRes || []
    statuts.value = stRes || []
  } catch (err) {
    console.error('Erreur chargement réferentiels:', err)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

const getFileUrl = (chemin) => {
  if (!chemin) return '#'
  return `http://localhost:8080/uploads/documents/${chemin}`
}

const pageNumbers = computed(() => {
  const pages = []
  const maxDisplayed = 5
  let start = Math.max(0, currentPage.value - 2)
  let end = Math.min(totalPages.value - 1, start + maxDisplayed - 1)
  if (end - start + 1 < maxDisplayed) {
    start = Math.max(0, end - maxDisplayed + 1)
  }
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

onMounted(() => {
  loadReferentiels()
  loadMouvements(0)
})
</script>

<style scoped>
.mvt-container {
  padding: 1.5rem;
}
.fs-7 {
  font-size: 0.75rem;
}
</style>

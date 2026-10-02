<template>
  <div class="admin-op-container">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold text-dark mb-1">Administration - Opérations Dossier</h3>
        <p class="text-muted small mb-0">Configuration des étapes, phases, et statuts par type de production</p>
      </div>
      <div class="d-flex gap-2">
        <button v-if="selectedOpIds.length > 0" class="btn btn-danger btn-sm d-flex align-items-center gap-1 shadow-sm" @click="deleteBatch">
          🗑️ Supprimer en masse ({{ selectedOpIds.length }})
        </button>
        <button class="btn btn-primary btn-sm d-flex align-items-center gap-1 shadow-sm" @click="openCreateModal">
          ➕ Ajouter une Opération
        </button>
      </div>
    </div>

    <!-- Main Layout with Fixed Right Sidebar -->
    <div class="row g-4">
      <!-- Left / Center Content: Tabs by Production Type & Matrix Table -->
      <div class="col-lg-9 col-md-8">
        <!-- Tabs for Product Types -->
        <ul class="nav nav-tabs nav-fill mb-3 bg-white p-2 rounded shadow-sm">
          <li v-for="tp in typesProduction" :key="tp.id" class="nav-item">
            <button
              class="nav-link text-nowrap fw-semibold"
              :class="{ active: activeTypeProductionId === tp.id }"
              @click="switchTypeProduction(tp.id)"
            >
              📦 {{ tp.nom }}
            </button>
          </li>
        </ul>

        <!-- Operations Matrix Table -->
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
            <h6 class="fw-bold mb-0 text-secondary">
              Sequence des Opérations (Glisser-Déposer pour réordonner ou affecter Phase/Statut)
            </h6>
            <button class="btn btn-outline-success btn-sm py-0" @click="saveReorder" :disabled="!isOrderChanged">
              💾 Enregistrer l'ordre
            </button>
          </div>
          <div class="card-body p-0">
            <div v-if="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status"></div>
            </div>

            <div v-else-if="currentOperations.length === 0" class="text-center py-5">
              <p class="text-muted mb-0">Aucune opération configurée pour ce type de produit.</p>
            </div>

            <div v-else class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="bg-light small text-uppercase">
                  <tr>
                    <th style="width: 40px;" class="ps-3">
                      <input type="checkbox" class="form-check-input" :checked="isAllSelected" @change="toggleSelectAll" />
                    </th>
                    <th style="width: 30px;">#</th>
                    <th>Description Opération</th>
                    <th>Phase Déposée</th>
                    <th>Statut Déposé</th>
                    <th>Obligatoire</th>
                    <th class="pe-3 text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(op, index) in currentOperations"
                    :key="op.id"
                    draggable="true"
                    @dragstart="onDragStartRow($event, index)"
                    @dragover.prevent
                    @drop="onDropRow($event, index)"
                    @dragenter.prevent
                    class="draggable-row"
                  >
                    <td class="ps-3">
                      <input type="checkbox" class="form-check-input" :value="op.id" v-model="selectedOpIds" />
                    </td>
                    <td class="text-muted small cursor-move" title="Glisser pour réordonner">
                      ☰ {{ index + 1 }}
                    </td>
                    <td class="fw-semibold">
                      <span class="d-block">{{ op.description }}</span>
                    </td>

                    <!-- Drop Target for Phase -->
                    <td
                      @dragover.prevent
                      @drop="onDropPhaseOnOp($event, op)"
                      class="drop-zone p-2 border-dashed rounded"
                      :class="{'bg-info-subtle': dragOverOpId === op.id}"
                    >
                      <div v-if="op.phaseDossier" class="d-flex align-items-center justify-content-between badge bg-info text-dark p-2 w-100">
                        <span>🔷 {{ op.phaseDossier.nom }}</span>
                        <button class="btn-close btn-close-white ms-1" style="font-size: 0.6rem;" @click.stop="removePhase(op)"></button>
                      </div>
                      <div v-else class="text-muted small text-center py-1">
                        ➕ Déposer une Phase ici
                      </div>
                    </td>

                    <!-- Drop Target for Statut -->
                    <td
                      @dragover.prevent
                      @drop="onDropStatutOnOp($event, op)"
                      class="drop-zone p-2 border-dashed rounded"
                      :class="{'bg-success-subtle': dragOverOpId === op.id}"
                    >
                      <div v-if="op.statutDossier" class="d-flex align-items-center justify-content-between badge bg-success p-2 w-100">
                        <span>🟢 {{ op.statutDossier.nom }}</span>
                        <button class="btn-close btn-close-white ms-1" style="font-size: 0.6rem;" @click.stop="removeStatut(op)"></button>
                      </div>
                      <div v-else class="text-muted small text-center py-1">
                        ➕ Déposer un Statut ici
                      </div>
                    </td>

                    <td>
                      <div class="form-check form-switch mb-0">
                        <input class="form-check-input" type="checkbox" :checked="op.estObligatoire" @change="toggleObligatoire(op)" />
                        <span class="form-check-label small" :class="op.estObligatoire ? 'text-danger fw-bold' : 'text-muted'">
                          {{ op.estObligatoire ? 'Oui' : 'Non' }}
                        </span>
                      </div>
                    </td>

                    <td class="pe-3 text-end">
                      <button class="btn btn-link text-primary p-0 me-2" @click="editOp(op)" title="Modifier">✏️</button>
                      <button class="btn btn-link text-danger p-0" @click="deleteSingleOp(op.id)" title="Supprimer">🗑️</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Fixed Sidebar Box (Boîte fixe ne bougeant pas au scroll) -->
      <div class="col-lg-3 col-md-4">
        <div class="fixed-sidebar-box p-3 bg-white rounded shadow-sm border">
          <h6 class="fw-bold text-uppercase text-secondary fs-7 mb-3 d-flex align-items-center gap-1">
            <span>🎯 Boîte à Outils (Drag & Drop)</span>
          </h6>
          <p class="text-muted extra-small mb-3">Glissez les éléments ci-dessous vers les cases du tableau pour les assigner aux opérations.</p>

          <!-- Draggable Phases -->
          <div class="mb-4">
            <h6 class="fw-semibold text-info small border-bottom pb-1 mb-2">Phases de Dossier</h6>
            <div class="d-flex flex-column gap-2" style="max-height: 220px; overflow-y: auto;">
              <div
                v-for="phase in phases"
                :key="phase.id"
                draggable="true"
                @dragstart="onDragStartPhase($event, phase)"
                class="badge bg-info text-dark p-2 d-flex align-items-center gap-2 cursor-grab shadow-sm border"
              >
                <span>🖐️</span>
                <span class="fw-bold">{{ phase.nom }}</span>
              </div>
            </div>
          </div>

          <!-- Draggable Statuts -->
          <div>
            <h6 class="fw-semibold text-success small border-bottom pb-1 mb-2">Statuts de Dossier</h6>
            <div class="d-flex flex-column gap-2" style="max-height: 250px; overflow-y: auto;">
              <div
                v-for="statut in statuts"
                :key="statut.id"
                draggable="true"
                @dragstart="onDragStartStatut($event, statut)"
                class="badge bg-success text-white p-2 d-flex align-items-center gap-2 cursor-grab shadow-sm border"
              >
                <span>🖐️</span>
                <span class="fw-bold">{{ statut.nom }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Form for Operation Creation / Edition -->
    <div v-if="showOpModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
          <div class="modal-header bg-dark text-white">
            <h5 class="modal-title fw-bold">{{ opForm.id ? 'Modifier l\'Opération' : 'Nouvelle Opération' }}</h5>
            <button type="button" class="btn-close btn-close-white" @click="showOpModal = false"></button>
          </div>
          <div class="modal-body p-4">
            <div class="mb-3">
              <label class="form-label small fw-bold">Description de l'Opération <span class="text-danger">*</span></label>
              <input type="text" v-model="opForm.description" class="form-control" placeholder="Ex: Déclaration Douane, Visite physique..." />
            </div>
            <div class="mb-3">
              <label class="form-label small fw-bold">Type de Production</label>
              <select v-model="opForm.idTypeProduction" class="form-select">
                <option v-for="tp in typesProduction" :key="tp.id" :value="tp.id">{{ tp.nom }}</option>
              </select>
            </div>
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold">Phase Associeé</label>
                <select v-model="opForm.idPhase" class="form-select">
                  <option :value="null">Aucune phase</option>
                  <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Statut Associé</label>
                <select v-model="opForm.idStatut" class="form-select">
                  <option :value="null">Aucun statut</option>
                  <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
                </select>
              </div>
            </div>
            <div class="form-check form-switch">
              <input class="form-check-input" type="checkbox" v-model="opForm.estObligatoire" id="checkOblig" />
              <label class="form-check-label fw-bold small" for="checkOblig">Opération Obligatoire</label>
            </div>
          </div>
          <div class="modal-footer bg-light">
            <button type="button" class="btn btn-secondary btn-sm" @click="showOpModal = false">Annuler</button>
            <button type="button" class="btn btn-primary btn-sm" @click="saveOp">Enregistrer</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import AdminService from '../services/AdminService'

const typesProduction = ref([])
const phases = ref([])
const statuts = ref([])

const activeTypeProductionId = ref(null)
const currentOperations = ref([])
const loading = ref(false)
const selectedOpIds = ref([])
const isOrderChanged = ref(false)

const showOpModal = ref(false)
const opForm = ref({
  id: null,
  description: '',
  idTypeProduction: null,
  idPhase: null,
  idStatut: null,
  estObligatoire: false
})

const draggedRowIndex = ref(null)
const dragOverOpId = ref(null)

const loadInitialData = async () => {
  try {
    const [tpRes, phRes, stRes] = await Promise.all([
      AdminService.typesProduction.lister(),
      AdminService.phasesDossier.lister(),
      AdminService.statutsDossier.lister()
    ])
    typesProduction.value = tpRes || []
    phases.value = phRes || []
    statuts.value = stRes || []

    if (typesProduction.value.length > 0) {
      activeTypeProductionId.value = typesProduction.value[0].id
      loadOperationsForActiveType()
    }
  } catch (err) {
    console.error('Erreur chargement donnees admin:', err)
  }
}

const loadOperationsForActiveType = async () => {
  if (!activeTypeProductionId.value) return
  loading.value = true
  selectedOpIds.value = []
  isOrderChanged.value = false

  try {
    const res = await AdminService.operationsDossier.lister(activeTypeProductionId.value)
    currentOperations.value = res || []
  } catch (err) {
    console.error('Erreur chargement operations:', err)
  } finally {
    loading.value = false
  }
}

const switchTypeProduction = (tpId) => {
  activeTypeProductionId.value = tpId
  loadOperationsForActiveType()
}

// ── Drag & Drop Handlers ──────────────────────────────────────────────────

const onDragStartPhase = (event, phase) => {
  event.dataTransfer.setData('text/plain', JSON.stringify({ type: 'PHASE', item: phase }))
}

const onDragStartStatut = (event, statut) => {
  event.dataTransfer.setData('text/plain', JSON.stringify({ type: 'STATUT', item: statut }))
}

const onDropPhaseOnOp = async (event, op) => {
  const rawData = event.dataTransfer.getData('text/plain')
  if (!rawData) return
  try {
    const data = JSON.parse(rawData)
    if (data.type === 'PHASE') {
      const phase = data.item
      op.phaseDossier = phase
      await AdminService.operationsDossier.modifier(op.id, { idPhase: phase.id })
    }
  } catch (err) {
    console.error('Erreur drop phase:', err)
  }
}

const onDropStatutOnOp = async (event, op) => {
  const rawData = event.dataTransfer.getData('text/plain')
  if (!rawData) return
  try {
    const data = JSON.parse(rawData)
    if (data.type === 'STATUT') {
      const statut = data.item
      op.statutDossier = statut
      await AdminService.operationsDossier.modifier(op.id, { idStatut: statut.id })
    }
  } catch (err) {
    console.error('Erreur drop statut:', err)
  }
}

const removePhase = async (op) => {
  op.phaseDossier = null
  await AdminService.operationsDossier.modifier(op.id, { idPhase: null })
}

const removeStatut = async (op) => {
  op.statutDossier = null
  await AdminService.operationsDossier.modifier(op.id, { idStatut: null })
}

const toggleObligatoire = async (op) => {
  op.estObligatoire = !op.estObligatoire
  await AdminService.operationsDossier.modifier(op.id, { estObligatoire: op.estObligatoire })
}

// ── Reordering Rows via Drag and Drop ─────────────────────────────────────

const onDragStartRow = (event, index) => {
  draggedRowIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', JSON.stringify({ type: 'ROW', index }))
}

const onDropRow = (event, targetIndex) => {
  const rawData = event.dataTransfer.getData('text/plain')
  if (!rawData) return
  try {
    const data = JSON.parse(rawData)
    if (data.type === 'ROW') {
      const fromIndex = data.index
      if (fromIndex !== targetIndex) {
        const movedItem = currentOperations.value.splice(fromIndex, 1)[0]
        currentOperations.value.splice(targetIndex, 0, movedItem)
        isOrderChanged.value = true
      }
    }
  } catch (e) {}
}

const saveReorder = async () => {
  const ids = currentOperations.value.map(op => op.id)
  try {
    await AdminService.operationsDossier.reordonner(ids)
    isOrderChanged.value = false
    alert('Ordre des opérations sauvegardé avec succès !')
  } catch (err) {
    alert('Erreur réordonnancement: ' + err.message)
  }
}

// ── Batch Delete & Actions ────────────────────────────────────────────────

const isAllSelected = computed(() => {
  return currentOperations.value.length > 0 && selectedOpIds.value.length === currentOperations.value.length
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedOpIds.value = []
  } else {
    selectedOpIds.value = currentOperations.value.map(op => op.id)
  }
}

const deleteBatch = async () => {
  
  try {
    await AdminService.operationsDossier.supprimerBatch(selectedOpIds.value)
    loadOperationsForActiveType()
  } catch (err) {
        if (err?.code === 'ERR_CANCELED') return
    alert('Erreur suppression en masse: ' + err.message)
  }
}

const deleteSingleOp = async (id) => {
  
  try {
    await AdminService.operationsDossier.supprimer(id)
    loadOperationsForActiveType()
  } catch (err) {
        if (err?.code === 'ERR_CANCELED') return
    alert('Erreur suppression: ' + err.message)
  }
}

const openCreateModal = () => {
  opForm.value = {
    id: null,
    description: '',
    idTypeProduction: activeTypeProductionId.value,
    idPhase: null,
    idStatut: null,
    estObligatoire: false
  }
  showOpModal.value = true
}

const editOp = (op) => {
  opForm.value = {
    id: op.id,
    description: op.description,
    idTypeProduction: op.typeProduction ? op.typeProduction.id : activeTypeProductionId.value,
    idPhase: op.phaseDossier ? op.phaseDossier.id : null,
    idStatut: op.statutDossier ? op.statutDossier.id : null,
    estObligatoire: op.estObligatoire || false
  }
  showOpModal.value = true
}

const saveOp = async () => {
  if (!opForm.value.description) {
    alert('La description est requise.')
    return
  }

  try {
    if (opForm.value.id) {
      await AdminService.operationsDossier.modifier(opForm.value.id, opForm.value)
    } else {
      await AdminService.operationsDossier.creer({
        description: opForm.value.description,
        typeProduction: { id: opForm.value.idTypeProduction },
        phaseDossier: opForm.value.idPhase ? { id: opForm.value.idPhase } : null,
        statutDossier: opForm.value.idStatut ? { id: opForm.value.idStatut } : null,
        estObligatoire: opForm.value.estObligatoire
      })
    }
    showOpModal.value = false
    loadOperationsForActiveType()
  } catch (err) {
    alert('Erreur sauvegarde opération: ' + err.message)
  }
}

onMounted(() => {
  loadInitialData()
})
</script>

<style scoped>
.admin-op-container {
  padding: 1.5rem;
}
.fixed-sidebar-box {
  position: sticky;
  top: 80px;
  z-index: 100;
}
.cursor-grab {
  cursor: grab;
}
.cursor-move {
  cursor: move;
}
.border-dashed {
  border: 2px dashed #dee2e6;
}
.drop-zone {
  min-height: 48px;
}
.extra-small {
  font-size: 0.7rem;
}
</style>

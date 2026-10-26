<template>
  <div class="admin-op-container">
    <div class="operation-page-header">
      <div>
        <span class="operation-page-header__eyebrow">RÉFÉRENTIEL MÉTIER</span>
        <h2>Opérations de dossier</h2>
        <p>Organisez les étapes, les phases et les statuts par type de production.</p>
      </div>
      <div class="operation-page-header__actions">
        <ExportActions :title="`Opérations dossier — ${typesProduction.find(tp => tp.id === activeTypeProductionId)?.nom || ''}`"
          :columns="colonnesExport" :load-rows="lignesExportOperations" :disabled="loading || !currentOperations.length" />
        <button v-if="selectedOpIds.length > 0" type="button" class="btn btn-outline-danger btn-sm" @click="deleteBatch">
          Supprimer la sélection ({{ selectedOpIds.length }})
        </button>
        <button type="button" class="btn btn-primary btn-sm" @click="openCreateModal">
          + Ajouter une opération
        </button>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-xl-9">
        <div class="production-tabs" role="tablist" aria-label="Type de production">
          <button v-for="tp in typesProduction" :key="tp.id" type="button" role="tab"
            class="production-tabs__item" :class="{ 'production-tabs__item--active': activeTypeProductionId === tp.id }"
            :aria-selected="activeTypeProductionId === tp.id"
            @click="switchTypeProduction(tp.id)">
            {{ tp.nom }}<span v-if="tp.modeTransport?.nom" class="production-tabs__mode"> · {{ tp.modeTransport.nom }}</span>
          </button>
        </div>

        <div class="card operation-table-card">
          <div class="card-header bg-white d-flex justify-content-between align-items-center gap-3 flex-wrap">
            <div>
              <h3 class="operation-table-card__title">Séquence des opérations</h3>
              <p class="operation-table-card__hint">{{ currentOperations.length }} opération(s) · Glissez les lignes pour changer leur ordre.</p>
            </div>
            <button type="button" class="btn btn-outline-primary btn-sm" @click="saveReorder" :disabled="!isOrderChanged">
              Enregistrer l’ordre
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
                    <th>Opération</th>
                    <th>Phase</th>
                    <th>Statut</th>
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
                      <span class="operation-drag-handle" aria-hidden="true">⋮⋮</span> {{ index + 1 }}
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
                      <div v-if="op.phaseDossier" class="operation-assignment operation-assignment--phase">
                        <span>{{ op.phaseDossier.nom }}</span>
                        <button type="button" class="operation-assignment__remove" :aria-label="'Retirer la phase ' + op.phaseDossier.nom" @click.stop="removePhase(op)">×</button>
                      </div>
                      <div v-else class="text-muted small text-center py-1">
                        Déposer une phase
                      </div>
                    </td>

                    <!-- Drop Target for Statut -->
                    <td
                      @dragover.prevent
                      @drop="onDropStatutOnOp($event, op)"
                      class="drop-zone p-2 border-dashed rounded"
                      :class="{'bg-success-subtle': dragOverOpId === op.id}"
                    >
                      <div v-if="op.statutDossier" class="operation-assignment operation-assignment--status">
                        <span>{{ op.statutDossier.nom }}</span>
                        <button type="button" class="operation-assignment__remove" :aria-label="'Retirer le statut ' + op.statutDossier.nom" @click.stop="removeStatut(op)">×</button>
                      </div>
                      <div v-else class="text-muted small text-center py-1">
                        Déposer un statut
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
                      <button type="button" class="btn btn-link btn-sm text-primary p-0 me-2" @click="editOp(op)">Modifier</button>
                      <button type="button" class="btn btn-link btn-sm text-danger p-0" @click="deleteSingleOp(op.id)">Supprimer</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <div class="col-xl-3">
        <div class="fixed-sidebar-box">
          <h3 class="operation-toolbox__title">Affectations</h3>
          <p class="operation-toolbox__hint">Glissez une phase ou un statut vers une opération.</p>

          <!-- Draggable Phases -->
          <div class="mb-4">
            <h4 class="operation-toolbox__section-title">Phases du dossier</h4>
            <div class="d-flex flex-column gap-2 operation-toolbox__list">
              <div
                v-for="phase in phases"
                :key="phase.id"
                draggable="true"
                @dragstart="onDragStartPhase($event, phase)"
                class="operation-toolbox__item cursor-grab"
              >
                <span class="operation-toolbox__grip" aria-hidden="true">⋮⋮</span>
                <span>{{ phase.nom }}</span>
              </div>
            </div>
          </div>

          <!-- Draggable Statuts -->
          <div>
            <h4 class="operation-toolbox__section-title">Statuts du dossier</h4>
            <div class="d-flex flex-column gap-2 operation-toolbox__list">
              <div
                v-for="statut in statuts"
                :key="statut.id"
                draggable="true"
                @dragstart="onDragStartStatut($event, statut)"
                class="operation-toolbox__item cursor-grab"
              >
                <span class="operation-toolbox__grip" aria-hidden="true">⋮⋮</span>
                <span>{{ statut.nom }}</span>
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
          <div class="modal-header bg-white">
            <h5 class="modal-title fw-bold">{{ opForm.id ? 'Modifier l\'Opération' : 'Nouvelle Opération' }}</h5>
            <button type="button" class="btn-close" aria-label="Fermer" @click="showOpModal = false"></button>
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
                <label class="form-label small fw-bold">Phase associée</label>
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
import ExportActions from '../components/inc/ExportActions.vue'
import { useNotification } from '../composables/useNotification'

const { notifySuccess, notifyError, notifyWarning } = useNotification()
const typesProduction = ref([])
const phases = ref([])
const statuts = ref([])

const activeTypeProductionId = ref(null)
const currentOperations = ref([])
const colonnesExport = [
  { key: 'niveauOperation', label: 'Ordre' },
  { key: 'description', label: 'Opération' },
  { key: 'phaseDossier.nom', label: 'Phase' },
  { key: 'statutDossier.nom', label: 'Statut' },
  { key: 'estObligatoire', label: 'Obligatoire' }
]
async function lignesExportOperations() { return currentOperations.value }
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
    notifySuccess('Ordre des opérations sauvegardé avec succès !')
  } catch (err) {
    notifyError('Erreur réordonnancement: ' + err.message)
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
    notifyError('Erreur suppression en masse: ' + err.message)
  }
}

const deleteSingleOp = async (id) => {
  
  try {
    await AdminService.operationsDossier.supprimer(id)
    loadOperationsForActiveType()
  } catch (err) {
        if (err?.code === 'ERR_CANCELED') return
    notifyError('Erreur suppression: ' + err.message)
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
    notifyWarning('La description est requise.')
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
    notifyError('Erreur sauvegarde opération: ' + err.message)
  }
}

onMounted(() => {
  loadInitialData()
})
</script>

<style scoped>
.admin-op-container {
  min-width: 0;
}
.operation-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 20px;
}
.operation-page-header__eyebrow {
  display: block;
  margin-bottom: 5px;
  color: #3b6a9c;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.11em;
}
.operation-page-header h2 {
  margin: 0 0 5px;
  color: #1e2a3a;
  font-size: 1.45rem;
  font-weight: 700;
}
.operation-page-header p {
  margin: 0;
  color: #5e6f88;
  font-size: 0.86rem;
}
.operation-page-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}
.production-tabs {
  display: flex;
  gap: 7px;
  overflow-x: auto;
  padding: 6px;
  margin-bottom: 15px;
  background: #fff;
  border: 1px solid #e2e6ec;
  border-radius: 10px;
}
.production-tabs__item {
  flex: none;
  padding: 8px 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #5e6f88;
  font-size: 0.79rem;
  font-weight: 600;
  white-space: nowrap;
}
.production-tabs__item:hover,
.production-tabs__item:focus-visible {
  background: #f2f4f8;
  color: #1e2a3a;
}
.production-tabs__item--active {
  background: #e6edf6;
  color: #244d73;
  border-color: #d4e1ef;
}
.production-tabs__mode {
  font-weight: 400;
}
.operation-table-card,
.fixed-sidebar-box {
  border: 1px solid #e2e6ec;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(30, 42, 58, 0.035);
}
.operation-table-card .card-header {
  padding: 17px 18px;
  border-bottom: 1px solid #e2e6ec;
}
.operation-table-card__title,
.operation-toolbox__title {
  margin: 0 0 4px;
  color: #1e2a3a;
  font-size: 0.96rem;
  font-weight: 700;
}
.operation-table-card__hint,
.operation-toolbox__hint {
  margin: 0;
  color: #5e6f88;
  font-size: 0.77rem;
}
.operation-table-card table {
  min-width: 800px;
  font-size: 0.8rem;
}
.operation-table-card th {
  color: #5e6f88;
  font-size: 0.7rem;
  white-space: nowrap;
}
.operation-drag-handle,
.operation-toolbox__grip {
  margin-right: 6px;
  color: #8a9aa8;
  letter-spacing: -2px;
}
.operation-assignment {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 0.74rem;
  font-weight: 600;
}
.operation-assignment--phase {
  color: #2c5f8a;
  background: #e6edf6;
}
.operation-assignment--status {
  color: #2b6d50;
  background: #e8f4ed;
}
.operation-assignment__remove {
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 1rem;
  line-height: 1;
}
.fixed-sidebar-box {
  position: sticky;
  top: 24px;
  z-index: 1;
  padding: 18px;
}
.operation-toolbox__hint {
  margin-bottom: 19px;
  line-height: 1.5;
}
.operation-toolbox__section-title {
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e6ec;
  color: #5e6f88;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
}
.operation-toolbox__list {
  max-height: 225px;
  overflow-y: auto;
}
.operation-toolbox__item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #e2e6ec;
  border-radius: 7px;
  background: #f9fafc;
  color: #1e2a3a;
  font-size: 0.76rem;
  font-weight: 600;
  white-space: normal;
}
.operation-toolbox__item:hover {
  border-color: #b7c8da;
  background: #f2f6fb;
}
.cursor-grab {
  cursor: grab;
}
.cursor-move {
  cursor: move;
}
.border-dashed {
  border: 1px dashed #c7d1de;
}
.drop-zone {
  min-height: 46px;
}
@media (max-width: 1199px) {
  .fixed-sidebar-box {
    position: static;
  }
}
</style>

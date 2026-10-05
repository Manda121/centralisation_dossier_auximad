<template>
  <div class="mvt-create-container">
    <!-- Header -->
    <div class="d-flex align-items-center gap-3 mb-4">
      <router-link to="/mouvements-dossier" class="btn btn-outline-secondary btn-sm">
        ← Retour aux mouvements
      </router-link>
      <div>
        <h3 class="fw-bold mb-0">Créer un Mouvement de Dossier</h3>
        <p class="text-muted small mb-0">Sélectionnez un dossier et enregistrez l'avancement de ses opérations</p>
      </div>
    </div>

    <!-- Section 1 : Autocomplete Dossier & Info Box -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body p-4">
        <label class="form-label fw-bold">Numéro de Dossier <span class="text-danger">*</span></label>
        <div class="position-relative col-md-6 mb-3">
          <input
            type="text"
            v-model="dossierSearchQuery"
            @input="onSearchDossier"
            class="form-control"
            placeholder="Tapez le numéro de dossier (ex: DOS-2026)..."
          />
          <!-- Autocomplete Dropdown -->
          <ul v-if="suggestions.length > 0" class="dropdown-menu show w-100 shadow-sm" style="max-height: 200px; overflow-y: auto;">
            <li v-for="d in suggestions" :key="d.id">
              <button class="dropdown-item d-flex justify-content-between align-items-center" @click="selectDossier(d)">
                <span class="fw-bold">{{ d.numeroDossier }}</span>
                <span class="small text-muted">{{ d.client ? d.client.nom : '' }} | {{ d.typeProduction ? d.typeProduction.nom : '' }}</span>
              </button>
            </li>
          </ul>
        </div>

        <!-- Small Info Box (Petit boite) -->
        <div v-if="selectedDossier" class="dossier-info-box p-3 rounded-3 border bg-light">
          <div class="row g-3 align-items-center">
            <div class="col-md-3">
              <span class="text-muted small d-block">Numéro Dossier</span>
              <strong class="text-primary fs-5">{{ selectedDossier.numeroDossier }}</strong>
            </div>
            <div class="col-md-3">
              <span class="text-muted small d-block">Client</span>
              <span class="fw-semibold">{{ selectedDossier.client ? `${selectedDossier.client.cpClient} - ${selectedDossier.client.nom}` : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Agence</span>
              <span class="fw-semibold">{{ selectedDossier.agence ? selectedDossier.agence.nom : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Type de Production</span>
              <span class="badge bg-secondary">{{ selectedDossier.typeProduction ? selectedDossier.typeProduction.nom : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Phase / Statut Actuel</span>
              <div class="d-flex flex-wrap gap-1 mt-1">
                <span v-if="selectedDossier.phaseDossier" class="badge bg-info text-dark">{{ selectedDossier.phaseDossier.nom }}</span>
                <span v-if="selectedDossier.statutDossier" class="badge bg-success">{{ selectedDossier.statutDossier.nom }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Section 2 : Table des Opérations à effectuer -->
    <div v-if="selectedDossier" class="card border-0 shadow-sm">
      <div class="card-header bg-white py-3">
        <h5 class="fw-bold mb-0 text-dark">Opérations du type de produit ({{ selectedDossier.typeProduction?.nom }})</h5>
      </div>
      <div class="card-body p-0">
        <div v-if="loadingOperations" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="text-muted small mt-2">Chargement des opérations...</p>
        </div>

        <div v-else class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="bg-light small text-uppercase">
              <tr>
                <th class="ps-4">Opération</th>
                <th>Phase & Statut Résultant</th>
                <th>Obligatoire</th>
                <th>Date Mouvement</th>
                <th>Document / Action</th>
                <th class="pe-4 text-end">Statut & Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="opDto in operationsStatus" :key="opDto.operation.id" :class="{'table-light opacity-75': opDto.estBloque}">
                <td class="ps-4">
                  <div class="fw-semibold">{{ opDto.operation.description }}</div>
                  <small v-if="opDto.estBloque" class="text-danger fw-semibold">
                    🔒 Requis : réaliser d'abord "{{ opDto.operationPrecedenteObligatoire?.description }}"
                  </small>
                </td>

                <td class="small">
                  <span v-if="opDto.operation.phaseDossier" class="badge bg-info-subtle text-info border border-info-subtle me-1">
                    Phase: {{ opDto.operation.phaseDossier.nom }}
                  </span>
                  <span v-if="opDto.operation.statutDossier" class="badge bg-success-subtle text-success border border-success-subtle">
                    Statut: {{ opDto.operation.statutDossier.nom }}
                  </span>
                </td>

                <td>
                  <span v-if="opDto.operation.estObligatoire" class="badge bg-danger-subtle text-danger border border-danger-subtle">Obligatoire</span>
                  <span v-else class="badge bg-light text-muted border">Optionnel</span>
                </td>

                <td style="min-width: 180px;">
                  <span v-if="opDto.estFait" class="text-success fw-bold small">
                    ✓ {{ formatDate(opDto.dateMvt) }}
                  </span>
                  <input
                    v-else
                    type="datetime-local"
                    v-model="movementDates[opDto.operation.id]"
                    :disabled="opDto.estBloque"
                    class="form-control form-control-sm"
                  />
                </td>

                <td>
                  <div v-if="isDeclarationOp(opDto.operation)" class="d-flex align-items-center gap-2">
                    <button
                      class="btn btn-outline-warning btn-sm d-flex align-items-center gap-1"
                      :disabled="opDto.estBloque"
                      @click="openDeclarationModal(opDto)"
                    >
                      📜 {{ opDto.declaration ? 'Voir/Modifier Déclaration' : 'Remplir Déclaration' }}
                    </button>
                    <span v-if="opDto.declaration" class="badge bg-success small">
                      N° {{ opDto.declaration.numeroDeclaration }}
                    </span>
                  </div>

                  <div v-else>
                    <input
                      v-if="!opDto.estFait"
                      type="file"
                      @change="onFileSelected($event, opDto.operation.id)"
                      :disabled="opDto.estBloque"
                      class="form-control form-control-sm"
                    />
                    <a v-else-if="opDto.document" :href="getFileUrl(opDto.document.cheminFichier)" target="_blank" class="btn btn-link btn-sm p-0">
                      📎 {{ opDto.document.nomFichier || 'Voir Fichier' }}
                    </a>
                  </div>
                </td>

                <td class="pe-4 text-end">
                  <span v-if="opDto.estFait" class="badge bg-success px-3 py-2">
                    Complété
                  </span>
                  <button
                    v-else
                    class="btn btn-primary btn-sm px-3"
                    :disabled="opDto.estBloque || savingOpId === opDto.operation.id"
                    @click="executeMouvement(opDto)"
                  >
                    <span v-if="savingOpId === opDto.operation.id" class="spinner-border spinner-border-sm me-1"></span>
                    Valider Opération
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Declaration Modal -->
    <div v-if="showDeclarationModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content border-0 shadow">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title fw-bold">Déclaration Douanière - Dossier {{ selectedDossier?.numeroDossier }}</h5>
            <button type="button" class="btn-close btn-close-white" @click="showDeclarationModal = false"></button>
          </div>
          <div class="modal-body p-4">
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small fw-bold">Numéro de Déclaration <span class="text-danger">*</span></label>
                <input type="text" v-model="declarationForm.numeroDeclaration" class="form-control" placeholder="Ex: DEC-2026-889" />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Date Déclaration</label>
                <input type="datetime-local" v-model="declarationForm.dateDeclaration" class="form-control" />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Bureau de Douane</label>
                <select v-model="declarationForm.bureauId" class="form-select">
                  <option :value="null">Sélectionnez un bureau...</option>
                  <option v-for="b in bureaux" :key="b.id" :value="b.id">{{ b.nom }} ({{ b.code }})</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-bold">Type de Déclaration</label>
                <select v-model="declarationForm.typeDeclarationId" class="form-select">
                  <option :value="null">Sélectionnez un type...</option>
                  <option v-for="td in typeDeclarations" :key="td.id" :value="td.id">{{ td.nom }}</option>
                </select>
              </div>
              <div class="col-12">
                <label class="form-label small fw-bold">Description / Observations</label>
                <textarea v-model="declarationForm.description" class="form-control" rows="2" placeholder="Remarques éventuelles..."></textarea>
              </div>
              <div class="col-12">
                <label class="form-label small fw-bold">Joindre Document / Scan de Déclaration</label>
                <input type="file" @change="onDeclarationFileSelected" class="form-control" />
              </div>
            </div>
          </div>
          <div class="modal-footer bg-light">
            <button type="button" class="btn btn-secondary btn-sm" @click="showDeclarationModal = false">Annuler</button>
            <button type="button" class="btn btn-primary btn-sm" :disabled="savingDeclaration" @click="saveDeclaration">
              <span v-if="savingDeclaration" class="spinner-border spinner-border-sm me-1"></span>
              Enregistrer la Déclaration
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal : document obligatoire manquant (blocage du mouvement) -->
    <div v-if="showDocModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
          <div class="modal-header bg-danger text-white">
            <h5 class="modal-title fw-bold">Document obligatoire manquant</h5>
            <button type="button" class="btn-close btn-close-white" @click="closeDocModal"></button>
          </div>
          <div class="modal-body p-4">
            <div class="alert alert-warning small">
              Le mouvement ne peut pas être enregistré : des documents obligatoires ne sont pas encore remis pour le
              dossier <strong>{{ selectedDossier?.numeroDossier }}</strong>.
              Assignez le document manquant pour pouvoir valider le mouvement.
            </div>
            <div v-if="loadingDocsManquants" class="text-center py-4">
              <div class="spinner-border text-primary" role="status"></div>
              <p class="text-muted small mt-2">Chargement des documents manquants...</p>
            </div>
            <div v-else class="row g-3">
              <div class="col-12">
                <label class="form-label small fw-bold">Document manquant <span class="text-danger">*</span></label>
                <select v-model="docForm.typeDocumentId" class="form-select">
                  <option :value="null">Sélectionnez un document...</option>
                  <option v-for="t in docsManquants" :key="t.id" :value="t.id">
                    {{ t.nom }}{{ t.typeProduction ? ` (${t.typeProduction.nom})` : '' }}
                  </option>
                </select>
                <div v-if="docsManquants.length === 0" class="text-muted small mt-1">
                  Aucun document manquant trouvé. Réessayez le mouvement.
                </div>
              </div>
              <div class="col-12">
                <label class="form-label small fw-bold">Fichier du document <span class="text-danger">*</span></label>
                <input type="file" @change="onDocFileSelected" class="form-control" />
              </div>
            </div>
          </div>
          <div class="modal-footer bg-light">
            <button type="button" class="btn btn-secondary btn-sm" @click="closeDocModal">Plus tard</button>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="savingDoc || !docForm.typeDocumentId || !docForm.fichier"
              @click="assignerEtValiderMouvement"
            >
              <span v-if="savingDoc" class="spinner-border spinner-border-sm me-1"></span>
              Assigner et valider le mouvement
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DossierService from '../services/DossierService'
import MvtDossierService from '../services/MvtDossierService'
import DeclarationService from '../services/DeclarationService'
import AdminService from '../services/AdminService'
import DocumentService from '../services/DocumentService'
import { useNotification } from '../composables/useNotification'

const router = useRouter()
const { notifyError, notifySuccess, notifyWarning } = useNotification()

const dossierSearchQuery = ref('')
const suggestions = ref([])
const selectedDossier = ref(null)

const operationsStatus = ref([])
const loadingOperations = ref(false)
const movementDates = ref({})
const selectedFiles = ref({})
const savingOpId = ref(null)

// Modal Déclaration
const showDeclarationModal = ref(false)
const savingDeclaration = ref(false)
const bureaux = ref([])
const typeDeclarations = ref([])
const declarationFile = ref(null)
const currentDeclarationOpDto = ref(null)

// Modal : document obligatoire manquant (blocage du mouvement)
const showDocModal = ref(false)
const loadingDocsManquants = ref(false)
const savingDoc = ref(false)
const docsManquants = ref([])
const pendingMouvementOp = ref(null)
const docForm = ref({ typeDocumentId: null, fichier: null })

const declarationForm = ref({
  numeroDeclaration: '',
  dateDeclaration: new Date().toISOString().slice(0, 16),
  bureauId: null,
  typeDeclarationId: null,
  description: ''
})

const onSearchDossier = async () => {
  if (!dossierSearchQuery.value || dossierSearchQuery.value.length < 2) {
    suggestions.value = []
    return
  }
  try {
    const res = await DossierService.searchDossiers(0, 10, 'numeroDossier', 'asc', { numeroDossier: dossierSearchQuery.value })
    suggestions.value = res.content || []
  } catch (err) {
    console.error('Erreur autocomplete dossier:', err)
  }
}

const selectDossier = (dossier) => {
  selectedDossier.value = dossier
  dossierSearchQuery.value = dossier.numeroDossier
  suggestions.value = []
  loadOperationsStatus()
}

const loadOperationsStatus = async () => {
  if (!selectedDossier.value) return
  loadingOperations.value = true
  try {
    const res = await MvtDossierService.getOperationsStatusForDossier(selectedDossier.value.id)
    operationsStatus.value = res || []

    // Pre-fill movement dates for uncompleted operations
    const dates = {}
    const nowISO = new Date().toISOString().slice(0, 16)
    operationsStatus.value.forEach(op => {
      if (!op.estFait) {
        dates[op.operation.id] = nowISO
      }
    })
    movementDates.value = dates
  } catch (err) {
    console.error('Erreur chargement statut opérations:', err)
  } finally {
    loadingOperations.value = false
  }
}

const onFileSelected = (event, opId) => {
  const file = event.target.files[0]
  if (file) {
    selectedFiles.value[opId] = file
  }
}

const executeMouvement = async (opDto) => {
  if (opDto.estBloque) return
  savingOpId.value = opDto.operation.id

  try {
    const formData = new FormData()
    formData.append('dossierId', selectedDossier.value.id)
    formData.append('operationId', opDto.operation.id)

    const dateVal = movementDates.value[opDto.operation.id]
    if (dateVal) {
      formData.append('dateMvt', dateVal)
    }

    if (selectedFiles.value[opDto.operation.id]) {
      formData.append('fichier', selectedFiles.value[opDto.operation.id])
    }

    await MvtDossierService.createMouvement(formData)

    // Re-fetch updated status
    await loadOperationsStatus()
    // Refresh dossier info
    const updatedDos = await DossierService.getDossierById(selectedDossier.value.id)
    selectedDossier.value = updatedDos
  } catch (err) {
    const msg = err.response?.data?.erreur || err.message
    if (estErreurDocumentsObligatoires(msg)) {
      // Un document obligatoire manque : ouvrir la fenetre d'assignation
      await openDocModal(opDto)
    } else {
      notifyError(msg)
    }
  } finally {
    savingOpId.value = null
  }
}

// ─── Fenetre d'assignation d'un document obligatoire manquant ───

const estErreurDocumentsObligatoires = (msg) =>
  (msg || '').toLowerCase().includes('documents obligatoires')

const openDocModal = async (opDto) => {
  pendingMouvementOp.value = opDto
  docForm.value = { typeDocumentId: null, fichier: null }
  showDocModal.value = true
  loadingDocsManquants.value = true
  try {
    docsManquants.value = await DocumentService.getDocsObligatoiresManquants(selectedDossier.value.id)
    if (docsManquants.value.length > 0) {
      docForm.value.typeDocumentId = docsManquants.value[0].id
    }
  } catch (err) {
    console.error('Erreur chargement documents obligatoires manquants:', err)
    docsManquants.value = []
  } finally {
    loadingDocsManquants.value = false
  }
}

const closeDocModal = () => {
  showDocModal.value = false
  pendingMouvementOp.value = null
}

const onDocFileSelected = (e) => {
  docForm.value.fichier = e.target.files[0] || null
}

const assignerEtValiderMouvement = async () => {
  if (!docForm.value.typeDocumentId || !docForm.value.fichier) {
    notifyWarning('Sélectionnez le document manquant et joignez le fichier correspondant.')
    return
  }
  savingDoc.value = true
  try {
    await DocumentService.saveDocument(selectedDossier.value.id, docForm.value.typeDocumentId, docForm.value.fichier)
    notifySuccess('Document assigné au dossier.')
    const opDto = pendingMouvementOp.value
    showDocModal.value = false
    pendingMouvementOp.value = null
    if (opDto) {
      await executeMouvement(opDto)
    }
  } catch (err) {
    const data = err.response?.data
    const msg = (typeof data === 'string' && data) ? data : (data?.erreur || err.message)
    notifyError(typeof msg === 'string' ? msg : "Erreur lors de l'assignation du document.")
  } finally {
    savingDoc.value = false
  }
}

const isDeclarationOp = (op) => {
  if (!op || !op.description) return false
  const desc = op.description.toLowerCase()
  return desc.includes('declaration') || desc.includes('déclaration')
}

const openDeclarationModal = async (opDto) => {
  currentDeclarationOpDto.value = opDto
  declarationFile.value = null

  // Pre-fill if declaration already exists
  if (opDto.declaration) {
    declarationForm.value = {
      numeroDeclaration: opDto.declaration.numeroDeclaration || '',
      dateDeclaration: opDto.declaration.dateDeclaration ? opDto.declaration.dateDeclaration.slice(0, 16) : new Date().toISOString().slice(0, 16),
      bureauId: opDto.declaration.bureau ? opDto.declaration.bureau.id : null,
      typeDeclarationId: opDto.declaration.typeDeclaration ? opDto.declaration.typeDeclaration.id : null,
      description: opDto.declaration.description || ''
    }
  } else {
    declarationForm.value = {
      numeroDeclaration: '',
      dateDeclaration: new Date().toISOString().slice(0, 16),
      bureauId: null,
      typeDeclarationId: null,
      description: ''
    }
  }
  showDeclarationModal.value = true
}

const onDeclarationFileSelected = (e) => {
  if (e.target.files[0]) {
    declarationFile.value = e.target.files[0]
  }
}

const saveDeclaration = async () => {
  if (!declarationForm.value.numeroDeclaration) {
    notifyWarning('Le numéro de déclaration est obligatoire.')
    return
  }

  savingDeclaration.value = true
  try {
    const formData = new FormData()
    formData.append('dossierId', selectedDossier.value.id)
    formData.append('numeroDeclaration', declarationForm.value.numeroDeclaration)
    if (declarationForm.value.dateDeclaration) formData.append('dateDeclaration', declarationForm.value.dateDeclaration)
    if (declarationForm.value.bureauId) formData.append('bureauId', declarationForm.value.bureauId)
    if (declarationForm.value.typeDeclarationId) formData.append('typeDeclarationId', declarationForm.value.typeDeclarationId)
    if (declarationForm.value.description) formData.append('description', declarationForm.value.description)
    if (declarationFile.value) formData.append('fichier', declarationFile.value)

    await DeclarationService.saveDeclaration(formData)

    // Execute movement if not done yet
    if (currentDeclarationOpDto.value && !currentDeclarationOpDto.value.estFait) {
      await executeMouvement(currentDeclarationOpDto.value)
    }

    showDeclarationModal.value = false
    await loadOperationsStatus()
  } catch (err) {
    const msg = err.response?.data?.erreur || err.message
    notifyError('Erreur déclaration : ' + msg)
  } finally {
    savingDeclaration.value = false
  }
}

const loadModalReferentiels = async () => {
  try {
    const [bRes, tdRes] = await Promise.all([
      AdminService.bureaux.lister(),
      DeclarationService.getTypeDeclarations()
    ])
    bureaux.value = bRes || []
    typeDeclarations.value = tdRes || []
  } catch (err) {
    console.error('Erreur referentiels modal:', err)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

const getFileUrl = (chemin) => {
  if (!chemin) return '#'
  return `http://localhost:8080/uploads/documents/${chemin}`
}

onMounted(() => {
  loadModalReferentiels()
})
</script>

<style scoped>
.mvt-create-container {
  padding: 1.5rem;
}
.dossier-info-box {
  background-color: #f8f9fa;
}
</style>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />
    <main class="mvt-main flex-grow-1">
      <div class="mvt-create-container">
    <header class="mvt-page-header">
      <div>
        <span class="mvt-eyebrow">SUIVI DES DOSSIERS</span>
        <h1>Créer un mouvement de dossier</h1>
        <p>Sélectionnez un dossier pour consulter ses étapes et enregistrer son avancement.</p>
      </div>
      <router-link to="/mouvements-dossier" class="btn btn-outline-secondary btn-sm">
        ← Retour aux mouvements
      </router-link>
    </header>

    <section class="card mvt-search-card mb-4" aria-labelledby="mvt-search-title">
      <div class="card-body p-4">
        <div class="mvt-section-heading mb-3">
          <span class="mvt-section-heading__number">01</span>
          <div>
            <h2 id="mvt-search-title">Choisir un dossier</h2>
            <p>Recherchez par numéro, puis sélectionnez le dossier correspondant.</p>
          </div>
        </div>
        <label class="form-label fw-semibold">Numéro de dossier <span class="text-danger">*</span></label>
        <div class="mvt-search-field mb-2">
          <AutocompleteRecherche v-model="dossierSearchQuery" :fetch-results="rechercherDossiers"
            :label-fn="dossier => dossier.numeroDossier"
            :sublabel-fn="dossier => [dossier.client?.nom, dossier.agence?.nom].filter(Boolean).join(' · ')"
            :value-fn="dossier => dossier.numeroDossier" :min-chars="2" :petit="false"
            placeholder="Ex. 1101060001" message-vide="Aucun dossier correspondant"
            @update:model-value="onDossierQueryChange" @efface="effacerDossier" @selection="selectDossier">
            <template #action>
              <button type="button" class="btn btn-primary" :disabled="chargementDossier || !dossierSearchQuery.trim()"
                @click="chargerDossierSaisi">
                {{ chargementDossier ? 'Chargement…' : 'Charger' }}
              </button>
            </template>
          </AutocompleteRecherche>
        </div>
        <p class="mvt-search-help">Quelques chiffres affichent des suggestions. Le numéro complet peut aussi être chargé directement.</p>

        <div v-if="selectedDossier" class="dossier-info-box">
          <div class="row g-3 align-items-center">
            <div class="col-md-3">
              <span class="text-muted small d-block">Numéro de dossier</span>
              <strong class="text-primary fs-5">{{ selectedDossier.numeroDossier }}</strong>
            </div>
            <div class="col-md-3">
              <span class="text-muted small d-block">Client</span>
              <span class="fw-semibold">{{ selectedDossier.client ? ([selectedDossier.client.cpClient, selectedDossier.client.nom].filter(Boolean).join(' - ') || '-') : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Agence</span>
              <span class="fw-semibold">{{ selectedDossier.agence ? selectedDossier.agence.nom : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Type de production</span>
              <span class="badge bg-secondary">{{ selectedDossier.typeProduction ? selectedDossier.typeProduction.nom : '-' }}</span>
            </div>
            <div class="col-md-2">
              <span class="text-muted small d-block">Phase / statut actuel</span>
              <div class="d-flex flex-wrap gap-1 mt-1">
                <span v-if="selectedDossier.phaseDossier" class="badge bg-info text-dark">{{ selectedDossier.phaseDossier.nom }}</span>
                <span v-if="selectedDossier.statutDossier" class="badge bg-success">{{ selectedDossier.statutDossier.nom }}</span>
                <span v-if="!selectedDossier.phaseDossier && !selectedDossier.statutDossier">—</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="!selectedDossier" class="mvt-empty-state card" aria-label="Étapes de création d'un mouvement">
      <div class="card-body p-4 p-md-5">
        <div class="mvt-empty-icon">03 ÉTAPES</div>
        <h2>Le suivi du dossier commence ici</h2>
        <p>Entrez le numéro d’un dossier pour afficher son avancement et les mouvements disponibles.</p>
        <div class="mvt-empty-steps">
          <div><span>01</span><strong>Rechercher</strong><small>Saisir ou sélectionner le numéro de dossier.</small></div>
          <div><span>02</span><strong>Consulter</strong><small>Vérifier les opérations effectuées et celles à venir.</small></div>
          <div><span>03</span><strong>Enregistrer</strong><small>Renseigner la date et valider le mouvement.</small></div>
        </div>
      </div>
    </section>

    <section v-if="selectedDossier && !loadingOperations && !operationsError" class="mvt-overview" aria-label="Avancement du dossier">
      <div class="mvt-progress-card card">
        <div class="card-body p-4">
          <div class="d-flex justify-content-between align-items-start gap-3">
            <div><span class="mvt-overline">AVANCEMENT</span><h2>Progression des opérations</h2></div>
            <strong class="mvt-progress-value">{{ progression }} %</strong>
          </div>
          <div class="progress mvt-progress-bar" role="progressbar" :aria-valuenow="progression" aria-valuemin="0" aria-valuemax="100">
            <div class="progress-bar" :style="{ width: progression + '%' }"></div>
          </div>
          <p class="mb-0 text-muted small">{{ operationsTerminees }} opération(s) terminée(s) sur {{ operationsStatus.length }}.</p>
        </div>
      </div>
      <div class="mvt-stat-card card"><div class="card-body p-4"><span class="mvt-overline">À TRAITER</span><strong>{{ operationsRestantes }}</strong><small>opération(s) restante(s)</small></div></div>
      <div class="mvt-stat-card card"><div class="card-body p-4"><span class="mvt-overline">EN ATTENTE</span><strong>{{ operationsBloquees }}</strong><small>opération(s) bloquée(s)</small></div></div>
      <div class="mvt-next-card card"><div class="card-body p-4"><span class="mvt-overline">PROCHAINE ÉTAPE</span><strong>{{ prochaineOperation?.operation?.description || (operationsRestantes ? 'Opérations en attente de prérequis' : operationsStatus.length ? 'Toutes les opérations sont terminées' : 'Aucune opération configurée') }}</strong><small v-if="prochaineOperation">Prête à être enregistrée</small></div></div>
    </section>

    <div v-if="selectedDossier" class="card mvt-operations-card">
      <div class="card-header bg-white py-3 px-4">
        <div class="mvt-section-heading">
          <span class="mvt-section-heading__number">02</span>
          <div><h2>Opérations du dossier</h2><p>{{ selectedDossier.typeProduction?.nom || 'Type de production non renseigné' }} · {{ operationsStatus.length }} opération(s)</p></div>
        </div>
      </div>
      <div class="card-body p-0">
        <div v-if="loadingOperations" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="text-muted small mt-2">Chargement des opérations...</p>
        </div>

        <div v-else-if="operationsError" class="mvt-no-operations text-danger">{{ operationsError }}</div>
        <div v-else-if="operationsStatus.length === 0" class="mvt-no-operations">
          Aucune opération n’est configurée pour le type de production de ce dossier.
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
                    Prérequis : réaliser d’abord « {{ opDto.operationPrecedenteObligatoire?.description }} »
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
                    {{ formatDate(opDto.dateMvt) }}
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
                      {{ opDto.declaration ? 'Voir / modifier la déclaration' : 'Remplir la déclaration' }}
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
                      {{ opDto.document.nomFichier || 'Voir le fichier' }}
                    </a>
                  </div>
                </td>

                <td class="pe-4 text-end">
                  <template v-if="opDto.estFait">
                    <span class="badge bg-success px-3 py-2 me-1">Complété</span>
                    <button v-if="peutAnnuler" type="button" class="btn btn-outline-danger btn-sm"
                      :disabled="savingOpId === opDto.operation.id"
                      @click="annulerOperation(opDto)">Annuler</button>
                  </template>
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
              L'opération <strong>{{ pendingMouvementOp?.operation.description }}</strong> ne peut pas être enregistrée :
              des documents obligatoires ne sont pas encore remis pour le dossier
              <strong>{{ selectedDossier?.numeroDossier }}</strong>.
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
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Header from '../components/inc/Header.vue'
import AutocompleteRecherche from '../components/inc/AutocompleteRecherche.vue'
import DossierService from '../services/DossierService'
import MvtDossierService from '../services/MvtDossierService'
import DeclarationService from '../services/DeclarationService'
import AdminService from '../services/AdminService'
import DocumentService from '../services/DocumentService'
import { useNotification } from '../composables/useNotification'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const { notifyError, notifySuccess, notifyWarning, confirm } = useNotification()
const peutAnnuler = PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)

const dossierSearchQuery = ref('')
const selectedDossier = ref(null)
const chargementDossier = ref(false)

const operationsStatus = ref([])
const loadingOperations = ref(false)
const operationsError = ref('')
const movementDates = ref({})
const operationsTerminees = computed(() => operationsStatus.value.filter(op => op.estFait).length)
const operationsRestantes = computed(() => operationsStatus.value.length - operationsTerminees.value)
const operationsBloquees = computed(() => operationsStatus.value.filter(op => !op.estFait && op.estBloque).length)
const progression = computed(() => operationsStatus.value.length ? Math.round(operationsTerminees.value / operationsStatus.value.length * 100) : 0)
const prochaineOperation = computed(() => operationsStatus.value.find(op => !op.estFait && !op.estBloque))

function maintenantLocalPourChampDate() {
  const maintenant = new Date()
  const deuxChiffres = valeur => String(valeur).padStart(2, '0')
  return `${maintenant.getFullYear()}-${deuxChiffres(maintenant.getMonth() + 1)}-${deuxChiffres(maintenant.getDate())}`
    + `T${deuxChiffres(maintenant.getHours())}:${deuxChiffres(maintenant.getMinutes())}`
}
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
  dateDeclaration: maintenantLocalPourChampDate(),
  bureauId: null,
  typeDeclarationId: null,
  description: ''
})

const rechercherDossiers = async (numero, limite = 8) => {
  const res = await DossierService.searchDossiers(0, limite, 'numeroDossier', 'asc', { numeroDossier: numero })
  return res.content || []
}

let operationsRequestId = 0

const effacerDossier = () => {
  operationsRequestId += 1
  selectedDossier.value = null
  operationsStatus.value = []
  operationsError.value = ''
  selectedFiles.value = {}
  movementDates.value = {}
  loadingOperations.value = false
}

const onDossierQueryChange = (numero) => {
  if (selectedDossier.value && String(numero) !== String(selectedDossier.value.numeroDossier)) effacerDossier()
}

const selectDossier = (dossier) => {
  effacerDossier()
  selectedDossier.value = dossier
  dossierSearchQuery.value = dossier.numeroDossier
  loadOperationsStatus()
}

const chargerDossierSaisi = async () => {
  const numero = String(dossierSearchQuery.value || '').trim()
  if (!numero) return
  chargementDossier.value = true
  try {
    const dossiers = await rechercherDossiers(numero, 10)
    if (numero !== String(dossierSearchQuery.value || '').trim()) return
    const dossier = dossiers.find(item => String(item.numeroDossier).trim() === numero)
    if (dossier) selectDossier(dossier)
    else notifyWarning('Aucun dossier ne correspond exactement à ce numéro.')
  } catch (err) {
    notifyError(err.response?.data?.erreur || 'Impossible de rechercher ce dossier.')
  } finally {
    chargementDossier.value = false
  }
}

const loadOperationsStatus = async () => {
  if (!selectedDossier.value) return
  const requestId = ++operationsRequestId
  const dossierId = selectedDossier.value.id
  loadingOperations.value = true
  operationsError.value = ''
  try {
    const res = await MvtDossierService.getOperationsStatusForDossier(dossierId)
    if (requestId !== operationsRequestId) return
    operationsStatus.value = res || []

    // Pre-fill movement dates for uncompleted operations
    const dates = {}
    const nowISO = maintenantLocalPourChampDate()
    operationsStatus.value.forEach(op => {
      if (!op.estFait) {
        dates[op.operation.id] = nowISO
      }
    })
    movementDates.value = dates
  } catch (err) {
    if (requestId === operationsRequestId) {
      operationsStatus.value = []
      operationsError.value = err.response?.data?.erreur || 'Impossible de charger les opérations du dossier.'
      notifyError(operationsError.value)
    }
  } finally {
    if (requestId === operationsRequestId) loadingOperations.value = false
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

const annulerOperation = async (opDto) => {
  try {
    const impact = await MvtDossierService.impactAnnulation(opDto.mvtId)
    const autres = impact.mouvementIds.length - 1
    const obligatoires = impact.operationsObligatoiresSuivantes || []
    const message = `Annuler « ${opDto.operation.description} » ? ${autres > 0 ? `${autres} mouvement(s) dépendant(s) seront aussi annulé(s).` : 'Les autres mouvements resteront actifs.'}`
      + (obligatoires.length ? `\nOpérations obligatoires déjà effectuées ensuite : ${obligatoires.join(', ')}.` : '')
    if (!await confirm(message)) return
    savingOpId.value = opDto.operation.id
    await MvtDossierService.annulerMouvement(opDto.mvtId, '', true)
    selectedDossier.value = await DossierService.getDossierById(selectedDossier.value.id)
    await loadOperationsStatus()
    notifySuccess('Mouvement annulé.')
  } catch (e) {
    notifyError(e.response?.data?.erreur || 'Impossible d’annuler le mouvement.')
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
    docsManquants.value = await DocumentService.getDocsObligatoiresManquants(
      selectedDossier.value.id,
      opDto.operation.id
    )
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
      selectedDossier.value = await DossierService.getDossierById(selectedDossier.value.id)
      await loadOperationsStatus()
      const actualise = operationsStatus.value.find(item => item.operation.id === opDto.operation.id)
      if (actualise && !actualise.estFait) await executeMouvement(actualise)
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
      dateDeclaration: opDto.declaration.dateDeclaration ? opDto.declaration.dateDeclaration.slice(0, 16) : maintenantLocalPourChampDate(),
      bureauId: opDto.declaration.bureau ? opDto.declaration.bureau.id : null,
      typeDeclarationId: opDto.declaration.typeDeclaration ? opDto.declaration.typeDeclaration.id : null,
      description: opDto.declaration.description || ''
    }
  } else {
    declarationForm.value = {
      numeroDeclaration: '',
      dateDeclaration: maintenantLocalPourChampDate(),
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
.mvt-main { margin-left: 260px; min-width: 0; }
.mvt-create-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 30px 34px 48px;
}
.mvt-page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 28px; }
.mvt-page-header h1 { margin: 4px 0 6px; color: #1e2a3a; font-size: clamp(1.55rem, 2.3vw, 2rem); font-weight: 700; }
.mvt-page-header p, .mvt-section-heading p { margin: 0; color: #64748b; }
.mvt-eyebrow, .mvt-overline { color: #3b6a9c; font-size: .68rem; font-weight: 800; letter-spacing: .11em; }
.mvt-page-header .btn { flex-shrink: 0; margin-top: 8px; border-color: #cbd5e1; color: #334155; background: #fff; }
.mvt-search-card, .mvt-empty-state, .mvt-overview .card, .mvt-operations-card {
  border: 1px solid #e2e6ec; border-radius: 12px; box-shadow: 0 3px 18px rgba(30, 42, 58, .045);
}
.mvt-section-heading { display: flex; align-items: center; gap: 13px; }
.mvt-section-heading__number {
  display: grid; place-items: center; width: 36px; height: 36px; flex: 0 0 36px;
  border-radius: 9px; color: #3b6a9c; background: #eaf1f8; font-size: .8rem; font-weight: 800;
}
.mvt-section-heading h2 { margin: 0 0 2px; color: #1e2a3a; font-size: 1rem; font-weight: 700; }
.mvt-section-heading p { font-size: .82rem; }
.mvt-search-field { max-width: 620px; }
.mvt-search-field :deep(.form-control) { min-height: 42px; }
.mvt-search-field :deep(.btn-primary), .mvt-operations-card .btn-primary { background: #3b6a9c; border-color: #3b6a9c; }
.mvt-search-help { margin: 0; color: #64748b; font-size: .78rem; }
.dossier-info-box {
  margin-top: 22px; padding: 18px 20px; border: 1px solid #dbe5ef; border-radius: 10px; background: #f7fafd;
}
.dossier-info-box .text-primary { color: #3b6a9c !important; }
.mvt-empty-state { min-height: 300px; }
.mvt-empty-state h2 { color: #1e2a3a; font-size: 1.25rem; font-weight: 700; }
.mvt-empty-state p { max-width: 620px; color: #64748b; }
.mvt-empty-icon {
  display: inline-block; margin-bottom: 14px; padding: 8px 11px; border-radius: 7px;
  background: #eaf1f8; color: #3b6a9c; font-size: .75rem; font-weight: 800; letter-spacing: .08em;
}
.mvt-empty-steps { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; margin-top: 30px; }
.mvt-empty-steps div { display: grid; grid-template-columns: 32px 1fr; column-gap: 10px; align-items: start; }
.mvt-empty-steps span { grid-row: span 2; color: #3b6a9c; font-size: .78rem; font-weight: 800; }
.mvt-empty-steps strong { color: #1e2a3a; font-size: .86rem; }
.mvt-empty-steps small { color: #64748b; }
.mvt-overview {
  display: grid; grid-template-columns: minmax(270px, 2fr) repeat(2, minmax(140px, 1fr)) minmax(220px, 1.5fr);
  gap: 14px; margin-bottom: 22px;
}
.mvt-overview h2 { margin: 4px 0 0; font-size: .98rem; font-weight: 700; color: #1e2a3a; }
.mvt-progress-value { color: #3b6a9c; font-size: 1.25rem; }
.mvt-progress-bar { height: 8px; margin: 24px 0 12px; background: #e8eef5; }
.mvt-progress-bar .progress-bar { background: #3b6a9c; }
.mvt-stat-card strong, .mvt-next-card strong { display: block; margin: 12px 0 3px; color: #1e2a3a; font-size: 1.55rem; line-height: 1.2; }
.mvt-next-card strong { font-size: .98rem; line-height: 1.4; }
.mvt-stat-card small, .mvt-next-card small { color: #64748b; }
.mvt-operations-card { overflow: hidden; }
.mvt-operations-card .card-header { border-bottom: 1px solid #e2e6ec; }
.mvt-operations-card thead th { padding-top: 14px; padding-bottom: 14px; color: #526579; font-size: .7rem; letter-spacing: .035em; }
.mvt-operations-card tbody td { padding-top: 14px; padding-bottom: 14px; }
.mvt-no-operations { padding: 32px; color: #64748b; text-align: center; }
@media (max-width: 1250px) { .mvt-overview { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 768px) {
  .mvt-create-container { padding: 22px 16px 38px; }
  .mvt-page-header { flex-direction: column; }
  .mvt-page-header .btn { margin-top: 0; }
  .mvt-empty-steps { grid-template-columns: 1fr; }
  .mvt-overview { grid-template-columns: 1fr; }
}
</style>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import ConteneurService from '@/services/ConteneurService'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  dossier: {
    type: Object,
    default: null
  },
  conteneurAEditer: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const typesContainers = ref([])
const conteneursExistants = ref([])
const submitting = ref(false)
const meErreur = ref(null)
const meSucces = ref(null)

// Autocomplete state pour le type de conteneur
const typeSearchQuery = ref('')
const showTypeDropdown = ref(false)
const selectedTypeContainer = ref(null)

// Autocomplete state pour le numéro de conteneur
const showNumeroDropdown = ref(false)

const form = reactive({
  id: null,
  idTypeContainer: '',
  numeroContainer: '',
  description: ''
})

const estEligible = computed(() => {
  return ConteneurService.estEligibleConteneur(props.dossier)
})

const modeTransportLabel = computed(() => {
  if (!props.dossier) return 'Non défini'
  return props.dossier.typeProduction?.modeTransport?.nom || 
         props.dossier.typeProduction?.nom || 
         props.dossier.ordreTransit?.typeProduction?.modeTransport?.nom || 
         'Non défini'
})

// Filtrage autocomplétion pour les types de conteneurs
const filteredTypesContainers = computed(() => {
  if (!typeSearchQuery.value) return typesContainers.value
  const q = typeSearchQuery.value.toLowerCase().trim()
  return typesContainers.value.filter(t => 
    t.nom.toLowerCase().includes(q) || 
    (t.description && t.description.toLowerCase().includes(q)) ||
    (t.volume && String(t.volume).includes(q))
  )
})

// Filtrage autocomplétion pour le numéro de conteneur :
// 1. Uniquement quand l'utilisateur tape quelque chose
// 2. Proposer les conteneurs liés aux dossiers ayant le MÊME type de production
// 3. Et dont le dossier n'est PAS clôturé
const filteredConteneursExistants = computed(() => {
  if (!form.numeroContainer || !form.numeroContainer.trim()) {
    return []
  }

  const q = form.numeroContainer.toLowerCase().trim()
  const currentTypeProdId = props.dossier?.typeProduction?.id || 
                            props.dossier?.ordreTransit?.typeProduction?.id

  return conteneursExistants.value.filter(c => {
    // 1. Correspondance du numéro
    const matchNumero = c.numeroContainer && c.numeroContainer.toLowerCase().includes(q)
    if (!matchNumero) return false

    const listDossiers = (c.dossiers && c.dossiers.length > 0) ? c.dossiers : (c.dossier ? [c.dossier] : [])

    // 2. Même type de production
    if (currentTypeProdId && listDossiers.length > 0) {
      const matchType = listDossiers.some(d => {
        const cTypeProdId = d.typeProduction?.id || d.ordreTransit?.typeProduction?.id
        return !cTypeProdId || cTypeProdId === currentTypeProdId
      })
      if (!matchType) return false
    }

    // 3. Aucun des dossiers associés ne doit être clôturé
    const hasCloture = listDossiers.some(d => {
      const statutNom = d.statutDossier?.nom || d.statut?.nom || ''
      const nomLower = statutNom.toLowerCase()
      return nomLower.includes('clot') || nomLower.includes('clôt')
    })
    if (hasCloture) return false

    return true
  })
})

function selectionnerTypeContainer(type) {
  selectedTypeContainer.value = type
  form.idTypeContainer = type.id
  typeSearchQuery.value = type.nom
  showTypeDropdown.value = false
}

function reinitialiserTypeSelection() {
  selectedTypeContainer.value = null
  form.idTypeContainer = ''
  typeSearchQuery.value = ''
  showTypeDropdown.value = true
}

function selectionnerConteneurExistant(c) {
  form.numeroContainer = c.numeroContainer
  showNumeroDropdown.value = false
  
  // Auto-remplissage du type de conteneur s'il est défini
  if (c.typeContainer) {
    selectedTypeContainer.value = c.typeContainer
    form.idTypeContainer = c.typeContainer.id
    typeSearchQuery.value = c.typeContainer.nom
  }

  // Auto-remplissage de la description si non remplie
  if (c.description && !form.description) {
    form.description = c.description
  }
}

function onTypeInputFocus() {
  showTypeDropdown.value = true
}

function onTypeInputBlur() {
  setTimeout(() => {
    showTypeDropdown.value = false
  }, 200)
}

function onNumeroInput() {
  if (form.numeroContainer && form.numeroContainer.trim().length > 0) {
    showNumeroDropdown.value = true
  } else {
    showNumeroDropdown.value = false
  }
}

function onNumeroInputFocus() {
  if (form.numeroContainer && form.numeroContainer.trim().length > 0) {
    showNumeroDropdown.value = true
  }
}

function onNumeroInputBlur() {
  setTimeout(() => {
    showNumeroDropdown.value = false
  }, 200)
}

async function chargerDonnees() {
  try {
    const [types, conteneurs] = await Promise.all([
      ConteneurService.getTypesContainers(),
      ConteneurService.getTousConteneurs()
    ])
    typesContainers.value = types
    conteneursExistants.value = conteneurs
  } catch (e) {
    console.error("Erreur de chargement des référentiels conteneurs", e)
  }
}

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      meErreur.value = null
      meSucces.value = null
      showTypeDropdown.value = false
      showNumeroDropdown.value = false
      chargerDonnees()

      if (props.conteneurAEditer) {
        form.id = props.conteneurAEditer.id
        form.idTypeContainer = props.conteneurAEditer.typeContainer?.id || ''
        form.numeroContainer = props.conteneurAEditer.numeroContainer || ''
        form.description = props.conteneurAEditer.description || ''
        
        selectedTypeContainer.value = props.conteneurAEditer.typeContainer || null
        typeSearchQuery.value = props.conteneurAEditer.typeContainer?.nom || ''
      } else {
        form.id = null
        form.idTypeContainer = ''
        form.numeroContainer = ''
        form.description = ''
        selectedTypeContainer.value = null
        typeSearchQuery.value = ''
      }
    }
  }
)

async function enregistrer() {
  if (!estEligible.value) {
    meErreur.value = "Impossible d'assigner un conteneur : le dossier n'est pas de type maritime."
    return
  }

  if (!form.numeroContainer || !form.numeroContainer.trim()) {
    meErreur.value = "Veuillez saisir un numéro de conteneur."
    return
  }

  if (!form.idTypeContainer) {
    meErreur.value = "Veuillez sélectionner un type de conteneur."
    return
  }

  submitting.value = true
  meErreur.value = null
  meSucces.value = null

  try {
    const payload = {
      id: form.id,
      dossier: { id: props.dossier.id },
      typeContainer: { id: parseInt(form.idTypeContainer) },
      numeroContainer: form.numeroContainer.trim(),
      description: form.description ? form.description.trim() : null
    }

    if (form.id) {
      await ConteneurService.modifierConteneur(form.id, payload)
      meSucces.value = "Conteneur mis à jour avec succès !"
    } else {
      await ConteneurService.assignerConteneur(payload)
      meSucces.value = "Conteneur assigné avec succès !"
    }

    setTimeout(() => {
      emit('saved')
      emit('close')
    }, 1000)
  } catch (e) {
    meErreur.value = e.response?.data?.message || e.message || "Erreur lors de l'enregistrement du conteneur."
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (props.show) {
    chargerDonnees()
  }
})
</script>

<template>
  <div v-if="show" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5); z-index: 1055;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 shadow-lg">
        
        <!-- Header -->
        <div class="modal-header bg-primary text-white py-3">
          <h5 class="modal-title h6 fw-bold mb-0 d-flex align-items-center gap-2">
            <svg viewBox="0 0 16 16" fill="currentColor" width="18" height="18">
              <path d="M0 2.5A1.5 1.5 0 0 1 1.5 1h13A1.5 1.5 0 0 1 16 2.5v11a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 0 13.5zm2 1v9h2v-9zm3 0v9h2v-9zm3 0v9h2v-9zm3 0v9h2v-9z"/>
            </svg>
            {{ form.id ? 'Modifier le conteneur' : 'Assigner un conteneur au dossier' }}
          </h5>
          <button type="button" class="btn-close btn-close-white" @click="emit('close')"></button>
        </div>

        <!-- Body -->
        <div class="modal-body p-4">
          <!-- Information dossier -->
          <div class="p-3 bg-light rounded border mb-3">
            <div class="row g-2 text-dark small">
              <div class="col-6">
                <span class="text-secondary d-block">Dossier :</span>
                <strong class="text-primary fs-6">{{ dossier?.numeroDossier || '—' }}</strong>
              </div>
              <div class="col-6">
                <span class="text-secondary d-block">Client :</span>
                <strong>{{ dossier?.client?.nom || '—' }} {{ dossier?.client?.prenom || '' }}</strong>
              </div>
              <div class="col-6 mt-2">
                <span class="text-secondary d-block">Mode de transport :</span>
                <span class="badge" :class="estEligible ? 'bg-success' : 'bg-warning text-dark'">
                  🚢 {{ modeTransportLabel }}
                </span>
              </div>
              <div class="col-6 mt-2">
                <span class="text-secondary d-block">Éligibilité Conteneur :</span>
                <span v-if="estEligible" class="badge bg-success-subtle text-success border border-success fw-semibold">
                  ✓ Éligible (Maritime)
                </span>
                <span v-else class="badge bg-danger-subtle text-danger border border-danger fw-semibold">
                  ✕ Non éligible (Non maritime)
                </span>
              </div>
            </div>
          </div>

          <!-- Alert error / success -->
          <div v-if="!estEligible" class="alert alert-warning py-2 mb-3 small d-flex align-items-center gap-2">
            <span>⚠️</span>
            <span>Un conteneur ne peut pas être assigné car ce dossier n'est pas de mode transport <strong>Maritime</strong>.</span>
          </div>

          <div v-if="meErreur" class="alert alert-danger py-2 mb-3 small">{{ meErreur }}</div>
          <div v-if="meSucces" class="alert alert-success py-2 mb-3 small">{{ meSucces }}</div>

          <!-- Formulaire -->
          <form @submit.prevent="enregistrer" v-if="estEligible">

            <!-- Champ Numéro Conteneur avec Autocomplétion au cours de la saisie -->
            <div class="mb-3 position-relative">
              <label class="form-label fw-semibold text-dark small">
                Numéro du conteneur <span class="text-danger">*</span>
              </label>

              <div class="input-group input-group-sm">
                <span class="input-group-text bg-white">📦</span>
                <input 
                  type="text" 
                  v-model="form.numeroContainer"
                  @input="onNumeroInput"
                  @focus="onNumeroInputFocus"
                  @blur="onNumeroInputBlur"
                  class="form-control form-control-sm text-uppercase" 
                  placeholder="Tapez pour rechercher (ex: CONT-2026-001...)"
                  required
                />
              </div>

              <!-- Menu d'autocomplétion (S'affiche uniquement lors de la saisie d'au moins un caractère) -->
              <div 
                v-if="showNumeroDropdown && filteredConteneursExistants.length > 0" 
                class="dropdown-menu show w-100 shadow border-1 position-absolute mt-1"
                style="max-height: 200px; overflow-y: auto; z-index: 1060;"
              >
                <button 
                  v-for="c in filteredConteneursExistants" 
                  :key="c.id"
                  type="button"
                  class="dropdown-item py-2 px-3 small border-bottom-light d-flex justify-content-between align-items-center"
                  @mousedown="selectionnerConteneurExistant(c)"
                >
                  <div>
                    <strong class="text-dark">{{ c.numeroContainer }}</strong>
                    <span v-if="c.dossiers && c.dossiers.length > 0" class="text-muted ms-2" style="font-size: 0.75rem;">
                      (Dossier(s): {{ Array.from(c.dossiers).map(d => d.numeroDossier).filter(Boolean).join(', ') }})
                    </span>
                    <span v-else-if="c.dossier?.numeroDossier" class="text-muted ms-2" style="font-size: 0.75rem;">
                      (Dossier: {{ c.dossier.numeroDossier }})
                    </span>
                  </div>
                  <span class="badge bg-light text-dark border ms-2">
                    {{ c.typeContainer?.nom || 'Conteneur' }}
                  </span>
                </button>
              </div>

              <div class="form-text text-muted" style="font-size: 0.75rem;">
                Propose les conteneurs des dossiers du même type de production non clôturés.
              </div>
            </div>
            
            <!-- Champ Type Conteneur avec Autocomplétion -->
            <div class="mb-3 position-relative">
              <label class="form-label fw-semibold text-dark small">
                Type de conteneur <span class="text-danger">*</span>
              </label>
              
              <div class="input-group input-group-sm">
                <span class="input-group-text bg-white">🔍</span>
                <input 
                  type="text" 
                  v-model="typeSearchQuery"
                  @focus="onTypeInputFocus"
                  @blur="onTypeInputBlur"
                  class="form-control form-control-sm" 
                  placeholder="Tapez pour rechercher (ex: 20 pieds, 40, High Cube, Reefer...)"
                  required
                />
                <button 
                  v-if="selectedTypeContainer || typeSearchQuery" 
                  type="button" 
                  class="btn btn-outline-secondary btn-sm"
                  @click="reinitialiserTypeSelection"
                  title="Réinitialiser la sélection"
                >
                  ✕
                </button>
              </div>

              <!-- Menu d'autocomplétion des types -->
              <div 
                v-if="showTypeDropdown && filteredTypesContainers.length > 0" 
                class="dropdown-menu show w-100 shadow border-1 position-absolute mt-1"
                style="max-height: 200px; overflow-y: auto; z-index: 1060;"
              >
                <button 
                  v-for="t in filteredTypesContainers" 
                  :key="t.id"
                  type="button"
                  class="dropdown-item py-2 px-3 small border-bottom-light d-flex justify-content-between align-items-center"
                  @mousedown="selectionnerTypeContainer(t)"
                >
                  <div>
                    <strong class="text-dark">{{ t.nom }}</strong>
                    <span v-if="t.description" class="text-muted d-block" style="font-size: 0.75rem;">
                      {{ t.description }}
                    </span>
                  </div>
                  <span class="badge bg-light text-dark border">
                    {{ t.volume ? t.volume + ' m³' : '' }} {{ t.poidsMax ? ' | Max ' + t.poidsMax + ' kg' : '' }}
                  </span>
                </button>
              </div>

              <!-- Informations sur le type sélectionné -->
              <div v-if="selectedTypeContainer" class="mt-2 p-2 bg-light border rounded small d-flex justify-content-between align-items-center">
                <span>
                  <strong>Sélectionné :</strong> {{ selectedTypeContainer.nom }}
                  <span v-if="selectedTypeContainer.volume" class="text-secondary ms-1">
                    ({{ selectedTypeContainer.volume }} m³)
                  </span>
                </span>
                <span class="text-success small fw-semibold">✓ Valide</span>
              </div>
            </div>

            <!-- Description -->
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark small">
                Description / Remarques
              </label>
              <textarea 
                v-model="form.description" 
                class="form-control form-control-sm" 
                rows="3" 
                placeholder="Ex: Conteneur scellé, état correct..."
              ></textarea>
            </div>

            <div class="d-flex justify-content-end gap-2 mt-4 pt-2 border-top">
              <button type="button" class="btn btn-outline-secondary btn-sm" @click="emit('close')">
                Annuler
              </button>
              <button type="submit" class="btn btn-primary btn-sm px-3" :disabled="submitting">
                <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                {{ submitting ? 'Enregistrement…' : (form.id ? 'Mettre à jour' : 'Assigner le conteneur') }}
              </button>
            </div>
          </form>

          <div v-else class="d-flex justify-content-end mt-3">
            <button type="button" class="btn btn-secondary btn-sm" @click="emit('close')">Fermer</button>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.dropdown-item:hover {
  background-color: #f1f5f9;
}
</style>

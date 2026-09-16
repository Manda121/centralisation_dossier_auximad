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
const submitting = ref(false)
const meErreur = ref(null)
const meSucces = ref(null)

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

async function chargerTypes() {
  try {
    typesContainers.value = await ConteneurService.getTypesContainers()
  } catch (e) {
    console.error("Erreur de chargement des types de conteneur", e)
  }
}

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      meErreur.value = null
      meSucces.value = null
      chargerTypes()

      if (props.conteneurAEditer) {
        form.id = props.conteneurAEditer.id
        form.idTypeContainer = props.conteneurAEditer.typeContainer?.id || ''
        form.numeroContainer = props.conteneurAEditer.numeroContainer || ''
        form.description = props.conteneurAEditer.description || ''
      } else {
        form.id = null
        form.idTypeContainer = ''
        form.numeroContainer = ''
        form.description = ''
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
    chargerTypes()
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
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark small">
                Type de conteneur <span class="text-danger">*</span>
              </label>
              <select v-model="form.idTypeContainer" class="form-select form-select-sm" required>
                <option value="">— Choisir un type —</option>
                <option v-for="t in typesContainers" :key="t.id" :value="t.id">
                  {{ t.nom }} (Vol: {{ t.volume || '—' }} m³ - Max: {{ t.poidsMax ? t.poidsMax + ' kg' : '—' }})
                </option>
              </select>
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold text-dark small">
                Numéro du conteneur <span class="text-danger">*</span>
              </label>
              <input 
                type="text" 
                v-model="form.numeroContainer" 
                class="form-control form-control-sm text-uppercase" 
                placeholder="Ex: CONT-2026-00123" 
                required
              />
              <div class="form-text text-muted" style="font-size: 0.75rem;">
                Numéro d'identification unique du conteneur.
              </div>
            </div>

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
</style>

<script setup>
import { computed } from 'vue'
import { useNotification } from '../../composables/useNotification'

const { notifyWarning } = useNotification()

const props = defineProps({
  show: { type: Boolean, default: false },
  editionMode: { type: Boolean, default: false },
  formModel: { type: Object, required: true },
  unites: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['fermer', 'soumettre'])

const titre = computed(() =>
  props.editionMode ? 'Éditer une marchandise' : 'Ajouter une marchandise'
)

function onSubmit() {
  const validation = props.formModel.valider()
  if (!validation.valide) {
    notifyWarning(validation.erreurs.join('\n'))
    return
  }
  emit('soumettre', props.formModel)
}
</script>

<template>
  <div
    v-if="show"
    class="modal fade show"
    tabIndex="-1"
    style="display: block; background: rgba(0,0,0,0.4);"
    aria-modal="true"
    role="dialog"
  >
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border shadow">
        <div class="modal-header bg-light border-bottom">
          <h5 class="modal-title fw-semibold">{{ titre }}</h5>
          <button type="button" class="btn-close" @click="emit('fermer')"></button>
        </div>

        <div class="modal-body">
          <form @submit.prevent="onSubmit">
            <div class="row g-3">
              <div class="col-12">
                <label class="form-label small fw-semibold text-secondary">
                  Description de la marchandise <span class="text-danger">*</span>
                </label>
                <textarea
                  v-model="formModel.description"
                  class="form-control form-control-sm"
                  rows="3"
                  required
                ></textarea>
              </div>

              <div class="col-md-6">
                <label class="form-label small fw-semibold text-secondary">
                  Unité de Mesure <span class="text-danger">*</span>
                </label>
                <select
                  v-model="formModel.uniteMesure"
                  class="form-select form-select-sm"
                  required
                >
                  <option :value="null">Sélectionnez l'unité</option>
                  <option v-for="u in unites" :key="u.id" :value="u">
                    {{ u.nom }}
                  </option>
                </select>
              </div>

              <div class="col-md-6">
                <label class="form-label small fw-semibold text-secondary">Marque</label>
                <input type="text" v-model="formModel.marque" class="form-control form-control-sm">
              </div>

              <div class="col-md-6">
                <label class="form-label small fw-semibold text-secondary">Modèle</label>
                <input type="text" v-model="formModel.modele" class="form-control form-control-sm">
              </div>

              <div class="col-md-4">
                <label class="form-label small fw-semibold text-secondary">
                  Quantité <span class="text-danger">*</span>
                </label>
                <input
                  type="number"
                  v-model="formModel.quantite"
                  class="form-control form-control-sm"
                  required
                  min="1"
                >
              </div>

              <div class="col-md-4">
                <label class="form-label small fw-semibold text-secondary">Poids (kg)</label>
                <input
                  type="number"
                  step="0.01"
                  v-model="formModel.poids"
                  class="form-control form-control-sm"
                >
              </div>

              <div class="col-md-4">
                <label class="form-label small fw-semibold text-secondary">Volume (m³)</label>
                <input
                  type="number"
                  step="0.01"
                  v-model="formModel.volume"
                  class="form-control form-control-sm"
                >
              </div>
            </div>

            <div class="d-flex justify-content-end gap-2 mt-4">
              <button type="button" class="btn btn-secondary btn-sm" @click="emit('fermer')">
                Annuler
              </button>
              <button type="submit" class="btn btn-primary btn-sm" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
                Enregistrer
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

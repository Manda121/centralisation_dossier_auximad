<script setup>
import { ref, watch } from 'vue'
import OrdreTransitService from '../../services/OrdreTransitService'

const props = defineProps({
  /** Permet de pré-remplir le numéro (ex: depuis route.query) */
  initialNumero: { type: [String, Number], default: '' }
})

const emit = defineEmits(['ot-selected'])

const numero = ref(props.initialNumero || '')
const loading = ref(false)
const selectedOt = ref(null)

watch(() => props.initialNumero, (val) => {
  if (val && val !== numero.value) {
    numero.value = val
    rechercher()
  }
})

async function rechercher() {
  if (!numero.value) return

  loading.value = true
  selectedOt.value = null
  emit('ot-selected', null)

  try {
    const result = await OrdreTransitService.searchOrdreTransits(
      0, 1, 'date_creation', 'desc',
      { numero: numero.value }
    )

    if (result.content?.length > 0) {
      selectedOt.value = result.content[0]
      emit('ot-selected', selectedOt.value)
    } else {
      alert('Aucun ordre de transit trouvé sous ce numéro.')
    }
  } catch (e) {
    console.error(e)
    alert('Erreur technique lors de la recherche.')
  } finally {
    loading.value = false
  }
}

/** Méthode publique pour charger un OT par ID (utile depuis la route) */
async function chargerParId(id) {
  loading.value = true
  try {
    const ot = await OrdreTransitService.getOrdreTransitById(id)
    selectedOt.value = ot
    numero.value = ot.numeroOrdreTransit
    emit('ot-selected', ot)
  } catch (e) {
    console.error('Impossible de charger l\'OT', e)
  } finally {
    loading.value = false
  }
}

defineExpose({ chargerParId })
</script>

<template>
  <div class="card border shadow-sm mb-3">
    <div class="card-body p-3">
      <label class="form-label small fw-semibold text-secondary">
        RECHERCHER ET SÉLECTIONNER UN ORDRE DE TRANSIT
      </label>
      <div class="row g-2 align-items-center">
        <div class="col-md-6">
          <div class="input-group input-group-sm">
            <input
              type="number"
              v-model="numero"
              class="form-control"
              placeholder="Saisissez un numéro d'OT (ex: 10001)..."
              @keyup.enter="rechercher"
            >
            <button
              class="btn btn-primary"
              type="button"
              @click="rechercher"
              :disabled="loading"
            >
              <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
              Rechercher
            </button>
          </div>
        </div>
        <div class="col-md-6 text-end">
          <span v-if="selectedOt" class="badge bg-secondary text-white">
            OT sélectionné : {{ selectedOt.numeroOrdreTransit }}
            (Client: {{ selectedOt.client?.nom }})
          </span>
          <span v-else class="text-secondary small">
            Aucun OT sélectionné. Veuillez faire une recherche.
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
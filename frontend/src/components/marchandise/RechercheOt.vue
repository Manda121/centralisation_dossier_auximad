<script setup>
import { ref, watch } from 'vue'
import OrdreTransitService from '../../services/OrdreTransitService'
import AutocompleteRecherche from '../inc/AutocompleteRecherche.vue'

const props = defineProps({
  /** Permet de pré-remplir le numéro (ex: depuis route.query) */
  initialNumero: { type: [String, Number], default: '' }
})

const emit = defineEmits(['ot-selected'])

const numero = ref(props.initialNumero || '')
const loading = ref(false)
const selectedOt = ref(null)

watch(() => props.initialNumero, (val) => {
  if (val && String(val) !== String(numero.value)) {
    numero.value = val
    rechercherExact()
  }
})

/** Suggestions d'OT au fur et à mesure de la saisie (recherche partielle) */
async function suggestionsOt(q) {
  try {
    return await OrdreTransitService.suggestions(q)
  } catch (e) {
    console.error('Erreur recherche suggestions OT', e)
    return []
  }
}

function libelleClient(client) {
  if (!client) return ''
  return `${client.nom || ''} ${client.prenom || ''}`.trim()
}

/** Un OT a été choisi dans la liste des suggestions */
function onOtSuggestion(ot) {
  if (!ot) return
  selectedOt.value = ot
  emit('ot-selected', ot)
}

/** Recherche exacte par numéro (bouton « Rechercher » et pré-remplissage depuis l'URL) */
async function rechercherExact() {
  const valeur = String(numero.value || '').trim()
  if (!valeur) return
  if (!/^\d+$/.test(valeur)) {
    alert('Le numéro d’OT ne doit contenir que des chiffres.')
    return
  }

  loading.value = true
  selectedOt.value = null
  emit('ot-selected', null)

  try {
    const result = await OrdreTransitService.searchOrdreTransits(
      0, 1, 'date_creation', 'desc',
      { numero: valeur }
    )

    if (result.content?.length > 0) {
      onOtSuggestion(result.content[0])
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

defineExpose({ chargerParId, rechercherExact })
</script>

<template>
  <div class="card border shadow-sm mb-3">
    <div class="card-body p-3">
      <label class="form-label small fw-semibold text-secondary">
        RECHERCHER ET SÉLECTIONNER UN ORDRE DE TRANSIT
      </label>
      <div class="row g-2 align-items-center">
        <div class="col-md-6">
          <AutocompleteRecherche v-model="numero" :fetch-results="suggestionsOt"
            :label-fn="ot => ot.numeroOrdreTransit" :sublabel-fn="ot => libelleClient(ot.client)"
            :value-fn="ot => ot.numeroOrdreTransit" placeholder="Saisissez quelques chiffres (ex: 10001)..."
            message-vide="Aucun OT ne correspond" numerique @selection="onOtSuggestion">
            <template #action>
              <button class="btn btn-primary" type="button" @click="rechercherExact" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
                Rechercher
              </button>
            </template>
          </AutocompleteRecherche>
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
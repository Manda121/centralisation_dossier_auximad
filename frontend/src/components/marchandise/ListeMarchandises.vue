<script setup>
import { ref, watch, computed } from 'vue'
import Skeleton from '../inc/Skeleton.vue'
import ModalMarchandise from './ModalMarchandise.vue'
import Marchandise from '../../models/Marchandise'
import MarchandiseService from '../../services/MarchandiseService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../../utils/permission.service'
import { useNotification } from '../../composables/useNotification'

const { notifySuccess, notifyError } = useNotification()

const props = defineProps({
  ordreTransit: { type: Object, default: null }
})

const marchandises = ref([])
const loading = ref(false)
const showModal = ref(false)
const editionMode = ref(false)
const formModel = ref(new Marchandise())
const unites = ref([])
const natures = ref([])
const formLoading = ref(false)

const peutEditer = computed(() =>
  PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
)

// Charge les référentiels une seule fois
async function chargerReferentiels() {
  try {
    const [uList, nList] = await Promise.all([
      MarchandiseService.getUniteMesures(),
      MarchandiseService.getNatureMarchandises()
    ])
    unites.value = uList
    natures.value = nList
  } catch (e) {
    console.error('Erreur chargement référentiels', e)
  }
}

chargerReferentiels()

// Recharge la liste dès qu'un OT est sélectionné
watch(() => props.ordreTransit, async (ot) => {
  if (!ot) {
    marchandises.value = []
    return
  }
  await chargerMarchandises()
}, { immediate: true })

async function chargerMarchandises() {
  if (!props.ordreTransit) return

  loading.value = true
  try {
    marchandises.value = await MarchandiseService.getMarchandisesByOtId(props.ordreTransit.id)
  } catch (e) {
    console.error('Erreur chargement marchandises', e)
    marchandises.value = []
  } finally {
    loading.value = false
  }
}

function ouvrirAjout() {
  editionMode.value = false
  formModel.value = new Marchandise({
    ordreTransit: props.ordreTransit,
    uniteMesure: unites.value[0] || null,
    natureMarchandise: natures.value[0] || null
  })
  showModal.value = true
}

function ouvrirEdition(m) {
  editionMode.value = true
  formModel.value = new Marchandise({ ...m })
  showModal.value = true
}

function fermerModal() {
  showModal.value = false
}

async function soumettre(form) {
  formLoading.value = true
  try {
    const result = await MarchandiseService.saveMarchandise(form.toJSON())

    if (editionMode.value) {
      const idx = marchandises.value.findIndex(item => item.id === result.id)
      if (idx !== -1) marchandises.value[idx] = result
    } else {
      marchandises.value.push(result)
    }

    fermerModal()
    notifySuccess('Marchandise enregistrée.')
  } catch (e) {
    console.error(e)
    notifyError('Erreur lors de l\'enregistrement de la marchandise.')
  } finally {
    formLoading.value = false
  }
}

async function supprimer(m) {
  

  loading.value = true
  try {
    await MarchandiseService.deleteMarchandise(m.id)
    marchandises.value = marchandises.value.filter(item => item.id !== m.id)
    notifySuccess('Marchandise retirée avec succès.')
  } catch (e) {
        if (e?.code === 'ERR_CANCELED') return
    console.error(e)
    notifyError('Erreur technique lors de la suppression.')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="card border shadow-sm">
    <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center py-2">
      <h5 class="mb-0  tefw-semiboldxt-dark">Contenu du Transit</h5>
      <button v-if="peutEditer" class="btn btn-primary btn-sm" @click="ouvrirAjout">
        + Ajouter une marchandise
      </button>
    </div>

    <div class="card-body p-0">
      <!-- Skeleton -->
      <div v-if="loading" class="p-3">
        <div v-for="n in 5" :key="'msk-' + n" class="d-flex gap-3 align-items-center py-2 border-bottom">
          <Skeleton width="28%" height="12px" />
          <Skeleton width="14%" height="12px" />
          <Skeleton width="18%" height="12px" />
          <Skeleton width="8%" height="12px" />
          <Skeleton width="8%" height="12px" />
          <Skeleton width="8%" height="12px" />
          <Skeleton width="10%" height="12px" />
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="marchandises.length === 0" class="text-center py-5 text-secondary small">
        Aucune marchandise n'est associée à cet ordre de transit.<br>
        Cliquez sur le bouton "Ajouter une marchandise" pour commencer.
      </div>

      <!-- Table -->
      <div v-else class="table-responsive">
        <table class="table table-hover table-sm mb-0 align-middle">
          <thead class="table-light">
            <tr>
              <th>Description</th>
              <th>Nature</th>
              <th>Marque / Modèle</th>
              <th class="text-center">Quantité</th>
              <th class="text-center">Poids (kg)</th>
              <th class="text-center">Volume (m³)</th>
              <th class="text-center">Unité</th>
              <th v-if="peutEditer" class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in marchandises" :key="m.id">
              <td class="fw-semibold">{{ m.description }}</td>
              <td>{{ m.natureMarchandise?.nom }}</td>
              <td>{{ m.marque }} / {{ m.modele || '—' }}</td>
              <td class="text-center">{{ m.quantite }}</td>
              <td class="text-center">{{ m.poids || '—' }}</td>
              <td class="text-center">{{ m.volume || '—' }}</td>
              <td class="text-center text-secondary">{{ m.uniteMesure?.nom }}</td>
              <td v-if="peutEditer" class="text-end">
                <button class="btn btn-outline-secondary btn-sm me-1" @click="ouvrirEdition(m)">
                  Éditer
                </button>
                <button class="btn btn-outline-danger btn-sm" @click="supprimer(m)">
                  Supprimer
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- Modal géré en interne -->
  <ModalMarchandise :show="showModal" :edition-mode="editionMode" :form-model="formModel" :unites="unites"
    :natures="natures" :loading="formLoading" @fermer="fermerModal" @soumettre="soumettre" />
</template>
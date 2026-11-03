<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DossierService from '../../services/DossierService'

defineProps({
  actionLabel: { type: String, default: 'Ouvrir l’assignation' },
  selectionnable: { type: Function, default: () => true },
  motifIndisponible: { type: Function, default: () => '' },
})
const emit = defineEmits(['selection'])

function dateLocale() {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const filtres = reactive({ date: dateLocale(), query: '' })
const filtresAppliques = ref({ ...filtres })
const dossiers = ref([])
const chargement = ref(false)
const erreur = ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
let requete = 0
const titre = computed(() => !filtresAppliques.value.date ? 'Tous les dossiers'
  : filtresAppliques.value.date === dateLocale() ? 'Dossiers du jour'
    : `Dossiers du ${new Date(`${filtresAppliques.value.date}T12:00:00`).toLocaleDateString('fr-FR')}`)

const client = dossier => [dossier.client?.nom, dossier.client?.prenom].filter(Boolean).join(' ') || '—'
const transport = dossier => dossier.typeProduction?.modeTransport?.nom || dossier.ordreTransit?.typeProduction?.modeTransport?.nom || '—'
const dateCreation = valeur => valeur ? new Date(valeur).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—'

async function charger(nouvellePage = 0, appliquer = false) {
  if (appliquer) filtresAppliques.value = { ...filtres }
  const numeroRequete = ++requete
  chargement.value = true
  erreur.value = ''
  try {
    const filtresCourants = filtresAppliques.value
    const resultat = await DossierService.searchDossiers(nouvellePage, 15, 'dateCreation', 'desc', {
      query: filtresCourants.query.trim(), dateMin: filtresCourants.date, dateMax: filtresCourants.date,
    })
    if (numeroRequete !== requete) return
    dossiers.value = resultat.content || []
    totalPages.value = resultat.totalPages || 0
    totalElements.value = resultat.totalElements || 0
    page.value = nouvellePage
  } catch (e) {
    if (numeroRequete !== requete) return
    dossiers.value = []; totalPages.value = 0; totalElements.value = 0
    erreur.value = e.response?.status === 403 ? 'Vous n’avez pas accès à ces dossiers.' : 'Impossible de charger les dossiers. Réessayez.'
  } finally {
    if (numeroRequete === requete) chargement.value = false
  }
}

function changerPeriode(aujourdhui) {
  filtres.date = aujourdhui ? dateLocale() : ''
  charger(0, true)
}
onMounted(() => charger())
</script>

<template>
  <section class="card border-0 shadow-sm assignment-picker" aria-labelledby="choix-dossier-title">
    <div class="card-body p-4">
      <div class="d-flex flex-wrap align-items-start justify-content-between gap-3 mb-3">
        <div>
          <h2 id="choix-dossier-title" class="h5 fw-semibold mb-1">{{ titre }}</h2>
          <p class="small text-secondary mb-0">Sélectionnez un dossier pour ouvrir son espace d’assignation.</p>
        </div>
        <span class="badge rounded-pill text-bg-light border">{{ totalElements }} dossier(s)</span>
      </div>
      <form class="row g-3 align-items-end mb-4" @submit.prevent="charger(0, true)">
        <div class="col-sm-4 col-lg-3">
          <label for="assignation-date" class="form-label small fw-semibold">Date de création</label>
          <input id="assignation-date" v-model="filtres.date" type="date" class="form-control" />
        </div>
        <div class="col-sm-8 col-lg-5">
          <label for="assignation-recherche" class="form-label small fw-semibold">Rechercher un dossier ou un client</label>
          <input id="assignation-recherche" v-model="filtres.query" type="search" class="form-control" placeholder="N° de dossier, nom du client…" />
        </div>
        <div class="col-lg-4 d-flex gap-2 flex-wrap">
          <button class="btn btn-primary" :disabled="chargement">Rechercher</button>
          <button type="button" class="btn btn-outline-secondary" :disabled="chargement" @click="changerPeriode(true)">Aujourd’hui</button>
          <button type="button" class="btn btn-outline-secondary" :disabled="chargement" @click="changerPeriode(false)">Tous les jours</button>
        </div>
      </form>
      <p v-if="chargement" class="py-4 text-secondary text-center" role="status">Chargement des dossiers…</p>
      <div v-else-if="erreur" class="alert alert-danger" role="alert">{{ erreur }}</div>
      <div v-else-if="!dossiers.length" class="assignment-picker__empty">
        <strong>Aucun dossier pour cette recherche.</strong>
        <p class="text-secondary small mb-0 mt-1">Choisissez une autre date ou « Tous les jours » pour retrouver un dossier existant.</p>
      </div>
      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light"><tr>
            <th scope="col">Dossier / Client</th><th scope="col">Agence</th><th scope="col">Production / Transport</th>
            <th scope="col">Créé le</th><th scope="col">Statut</th><th scope="col" class="text-end">Assignation</th>
          </tr></thead>
          <tbody>
            <tr v-for="dossier in dossiers" :key="dossier.id">
              <td><strong class="d-block">{{ dossier.numeroDossier }}</strong><span class="small text-secondary">{{ client(dossier) }}</span></td>
              <td>{{ dossier.agence?.nom || '—' }}</td>
              <td>{{ dossier.typeProduction?.description || dossier.typeProduction?.nom || '—' }}<span class="d-block small text-secondary">{{ transport(dossier) }}</span></td>
              <td class="small">{{ dateCreation(dossier.dateCreation) }}</td>
              <td><span class="badge text-bg-light border">{{ dossier.statutDossier?.nom || '—' }}</span></td>
              <td class="text-end">
                <button v-if="selectionnable(dossier)" type="button" class="btn btn-sm btn-outline-primary text-nowrap" @click="emit('selection', dossier)">
                  {{ actionLabel }}<span class="visually-hidden"> pour le dossier {{ dossier.numeroDossier }}</span>
                </button>
                <span v-else class="text-secondary small">{{ motifIndisponible(dossier) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="totalPages > 1" class="d-flex align-items-center justify-content-between mt-4" aria-label="Pages des dossiers à assigner">
        <span class="small text-secondary">Page {{ page + 1 }} sur {{ totalPages }}</span>
        <div class="d-flex gap-2">
          <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="chargement || page === 0" @click="charger(page - 1)">Précédent</button>
          <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="chargement || page + 1 >= totalPages" @click="charger(page + 1)">Suivant</button>
        </div>
      </nav>
    </div>
  </section>
</template>

<style scoped>
.assignment-picker { min-width: 0; }
.assignment-picker__empty { padding: 3rem 1rem; border: 1px dashed #cbd5e1; border-radius: .75rem; text-align: center; }
.assignment-picker th { font-size: .75rem; color: #526175; white-space: nowrap; }
.assignment-picker td { padding-block: 1rem; }
</style>

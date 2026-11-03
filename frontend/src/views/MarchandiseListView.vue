<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import ChoixDossier from '../components/assignation/ChoixDossier.vue'
import RechercheOt from '../components/marchandise/RechercheOt.vue'
import ListeMarchandises from '../components/marchandise/ListeMarchandises.vue'
import DossierService from '../services/DossierService'
import OrdreTransitService from '../services/OrdreTransitService'

const route = useRoute()
const router = useRouter()
const dossierSelectionne = ref(null)
const selectedOt = ref(null)
const chargement = ref(false)
const erreur = ref('')
let requete = 0
const modeOt = computed(() => route.query.mode === 'ot' || !!route.query.otId || !!route.query.numero)
const espaceOuvert = computed(() => !!route.query.dossierId || modeOt.value)
const peutSelectionner = dossier => !!dossier.ordreTransit?.id

function choisirDossier(dossier) { router.push({ path: '/marchandises', query: { dossierId: dossier.id } }) }
function retourDossiers() { router.push({ path: '/marchandises' }) }
function ouvrirRechercheOt() { router.push({ path: '/marchandises', query: { mode: 'ot' } }) }
function onOtSelected(ot) {
  if (ot) router.replace({ path: '/marchandises', query: { mode: 'ot', otId: ot.id } })
  else selectedOt.value = null
}

watch(() => [route.query.dossierId, route.query.otId, route.query.numero, route.query.mode], async () => {
  const numeroRequete = ++requete
  dossierSelectionne.value = null
  selectedOt.value = null
  erreur.value = ''
  chargement.value = !!(route.query.dossierId || route.query.otId || route.query.numero)
  try {
    let dossier = null
    let ot = null
    if (route.query.dossierId) {
      dossier = await DossierService.getDossierById(route.query.dossierId)
      if (!dossier.ordreTransit?.id) throw new Error('Ce dossier ne possède pas d’ordre de transit pour recevoir des marchandises.')
      ot = await OrdreTransitService.getOrdreTransitById(dossier.ordreTransit.id)
    } else if (route.query.otId) {
      ot = await OrdreTransitService.getOrdreTransitById(route.query.otId)
    } else if (route.query.numero) {
      const resultat = await OrdreTransitService.searchOrdreTransits(0, 1, 'date_creation', 'desc', { numero: route.query.numero })
      if (!resultat.content?.length) throw new Error('Aucun ordre de transit ne correspond à ce numéro.')
      ot = await OrdreTransitService.getOrdreTransitById(resultat.content[0].id)
    }
    if (numeroRequete !== requete) return
    dossierSelectionne.value = dossier
    selectedOt.value = ot
  } catch (e) {
    if (numeroRequete === requete) erreur.value = e.response?.data?.message || e.message || 'Impossible de charger l’assignation de marchandises.'
  } finally {
    if (numeroRequete === requete) chargement.value = false
  }
}, { immediate: true })
</script>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />
    <main class="assignment-main flex-grow-1 p-4">
      <div class="container-fluid">
        <header class="d-flex align-items-start justify-content-between gap-3 flex-wrap mb-4">
          <div>
            <h1 class="h4 fw-bold text-dark mb-1">Assignation des marchandises</h1>
            <p class="text-secondary mb-0 small">Choisissez un dossier, puis gérez les marchandises de son ordre de transit.</p>
          </div>
          <button v-if="espaceOuvert" type="button" class="btn btn-outline-secondary btn-sm" @click="retourDossiers">← Retour aux dossiers</button>
          <button v-else type="button" class="btn btn-outline-secondary btn-sm" @click="ouvrirRechercheOt">Rechercher un OT sans dossier</button>
        </header>

        <ChoixDossier v-show="!espaceOuvert" action-label="Assigner les marchandises" :selectionnable="peutSelectionner"
          :motif-indisponible="() => 'Ordre de transit manquant'" @selection="choisirDossier" />

        <template v-if="espaceOuvert">
          <RechercheOt v-if="modeOt && !route.query.dossierId && !selectedOt && !chargement" :initial-numero="route.query.numero || ''" @ot-selected="onOtSelected" />
          <p v-if="chargement" class="alert alert-light border" role="status">Chargement de l’assignation…</p>
          <p v-else-if="erreur" class="alert alert-danger" role="alert">{{ erreur }}</p>
          <template v-else-if="selectedOt">
            <div class="assignment-context mb-4">
              <div>
                <span class="small text-secondary d-block">{{ dossierSelectionne ? 'Dossier sélectionné' : 'Ordre de transit sélectionné' }}</span>
                <h2 class="h5 fw-semibold mb-1">{{ dossierSelectionne?.numeroDossier || `OT ${selectedOt.numeroOrdreTransit}` }}</h2>
                <span class="small text-secondary">{{ selectedOt.client?.nom }} {{ selectedOt.client?.prenom || '' }} · {{ selectedOt.typeProduction?.description || selectedOt.typeProduction?.nom }}</span>
              </div>
              <div class="small text-end">
                <span class="badge text-bg-light border">OT {{ selectedOt.numeroOrdreTransit }}</span>
                <RouterLink v-if="dossierSelectionne" :to="`/dossiers/${dossierSelectionne.id}`" class="d-block mt-2">Voir le dossier</RouterLink>
                <button v-else type="button" class="btn btn-link btn-sm d-block ms-auto px-0 mt-1" @click="ouvrirRechercheOt">Choisir un autre OT</button>
              </div>
            </div>
            <ListeMarchandises :key="selectedOt.id" :ordre-transit="selectedOt" />
          </template>
        </template>
      </div>
    </main>
  </div>
</template>

<style scoped>
.assignment-main { margin-left: 280px; min-width: 0; }
.assignment-context { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1.25rem; border: 1px solid #d7e2ef; border-left: 4px solid #356493; border-radius: .75rem; background: #fff; }
@media (max-width: 767px) { .assignment-context { align-items: flex-start; flex-direction: column; } }
</style>

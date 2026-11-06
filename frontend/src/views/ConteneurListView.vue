<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import ChoixDossier from '../components/assignation/ChoixDossier.vue'
import ModalConteneur from '../components/conteneur/ModalConteneur.vue'
import ExportActions from '../components/inc/ExportActions.vue'
import DossierService from '../services/DossierService'
import ConteneurService from '../services/ConteneurService'
import PermissionService from '../utils/permission.service'
import { useNotification } from '../composables/useNotification'

const route = useRoute()
const router = useRouter()
const { notifyError, notifySuccess } = useNotification()
const dossierSelectionne = ref(null)
const conteneurs = ref([])
const chargement = ref(false)
const erreur = ref('')
const showModal = ref(false)
const conteneurAEditer = ref(null)
const retraitId = ref(null)
let requete = 0
const peutEditer = computed(() => PermissionService.peutEcrireOT())
const peutRetirer = computed(() => PermissionService.peutSupprimerOT())
const espaceOuvert = computed(() => !!route.query.dossierId)
const colonnesExport = [
  { key: 'numeroContainer', label: 'N° conteneur' },
  { key: 'typeContainer.nom', label: 'Type' },
  { key: 'description', label: 'Description' },
]
async function lignesExport() { return conteneurs.value }

function estMaritime(dossier) { return ConteneurService.estEligibleConteneur(dossier) }
function choisirDossier(dossier) { router.push({ path: '/conteneurs', query: { dossierId: dossier.id } }) }
function retourDossiers() { router.push({ path: '/conteneurs' }) }
function ouvrirModal(conteneur = null) {
  if (!estMaritime(dossierSelectionne.value) || !peutEditer.value) return
  conteneurAEditer.value = conteneur
  showModal.value = true
}

async function chargerConteneurs() {
  const dossierId = dossierSelectionne.value?.id
  if (!dossierId) return
  try {
    const resultat = await ConteneurService.getConteneursParDossier(dossierId)
    if (dossierSelectionne.value?.id === dossierId) conteneurs.value = resultat
  } catch (e) {
    if (dossierSelectionne.value?.id === dossierId) notifyError(e.response?.data?.message || 'Impossible de charger les conteneurs de ce dossier.')
  }
}

async function onConteneurEnregistre() { await chargerConteneurs() }
async function retirer(conteneur) {
  const dossierId = dossierSelectionne.value?.id
  if (!dossierId || !peutRetirer.value || retraitId.value !== null) return
  retraitId.value = conteneur.id
  try {
    await ConteneurService.supprimerConteneur(conteneur.id, dossierId)
    if (dossierSelectionne.value?.id === dossierId) await chargerConteneurs()
    notifySuccess('Conteneur retiré du dossier.')
  } catch (e) {
    if (e?.code !== 'ERR_CANCELED') notifyError(e.response?.data?.message || 'Impossible de retirer ce conteneur.')
  } finally { retraitId.value = null }
}

watch(() => route.query.dossierId, async id => {
  const numeroRequete = ++requete
  dossierSelectionne.value = null
  conteneurs.value = []
  erreur.value = ''
  showModal.value = false
  chargement.value = !!id
  if (!id) return
  try {
    const dossier = await DossierService.getDossierById(id)
    if (!estMaritime(dossier)) throw new Error('L’assignation de conteneurs est réservée aux dossiers maritimes.')
    const resultat = await ConteneurService.getConteneursParDossier(dossier.id)
    if (numeroRequete !== requete) return
    dossierSelectionne.value = dossier
    conteneurs.value = resultat
  } catch (e) {
    if (numeroRequete === requete) erreur.value = e.response?.data?.message || e.message || 'Impossible de charger l’assignation des conteneurs.'
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
            <h1 class="h4 fw-bold text-dark mb-1">Assignation des conteneurs</h1>
            <p class="text-secondary mb-0 small">Sélectionnez un dossier maritime pour consulter et gérer ses conteneurs.</p>
          </div>
          <button v-if="espaceOuvert" type="button" class="btn btn-outline-secondary btn-sm" @click="retourDossiers">← Retour aux dossiers</button>
        </header>

        <ChoixDossier v-show="!espaceOuvert" action-label="Assigner les conteneurs" :selectionnable="estMaritime"
          :motif-indisponible="() => 'Transport non maritime'" @selection="choisirDossier" />

        <template v-if="espaceOuvert">
          <p v-if="chargement" class="alert alert-light border" role="status">Chargement de l’assignation…</p>
          <p v-else-if="erreur" class="alert alert-danger" role="alert">{{ erreur }}</p>
          <template v-else-if="dossierSelectionne">
            <div class="assignment-context mb-4">
              <div>
                <span class="small text-secondary d-block">Dossier sélectionné</span>
                <h2 class="h5 fw-semibold mb-1">{{ dossierSelectionne.numeroDossier }}</h2>
                <span class="small text-secondary">{{ dossierSelectionne.client?.nom }} {{ dossierSelectionne.client?.prenom || '' }} · {{ dossierSelectionne.agence?.nom }} · {{ dossierSelectionne.typeProduction?.description }}</span>
              </div>
              <div class="small text-end">
                <span class="badge text-bg-primary">Maritime</span>
                <RouterLink :to="`/dossiers/${dossierSelectionne.id}`" class="d-block mt-2">Voir le dossier</RouterLink>
              </div>
            </div>

            <section class="card border-0 shadow-sm" aria-labelledby="conteneurs-dossier-title">
              <div class="card-header bg-white d-flex align-items-center justify-content-between gap-3 flex-wrap p-3">
                <h3 id="conteneurs-dossier-title" class="h6 fw-semibold mb-0">Conteneurs du dossier <span class="badge text-bg-light border ms-1">{{ conteneurs.length }}</span></h3>
                <div class="d-flex gap-2 flex-wrap">
                  <ExportActions title="Conteneurs du dossier" :columns="colonnesExport" :load-rows="lignesExport" :disabled="!conteneurs.length" />
                  <button v-if="peutEditer" type="button" class="btn btn-primary btn-sm" @click="ouvrirModal()">+ Assigner un conteneur</button>
                </div>
              </div>
              <div v-if="!conteneurs.length" class="p-5 text-center text-secondary">
                <strong>Aucun conteneur assigné à ce dossier.</strong>
                <p v-if="peutEditer" class="small mb-0 mt-2">Utilisez « Assigner un conteneur » pour en ajouter un ou choisir un conteneur existant.</p>
              </div>
              <div v-else class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light"><tr><th scope="col">N° conteneur</th><th scope="col">Type</th><th scope="col">Description</th><th v-if="peutEditer || peutRetirer" scope="col" class="text-end">Actions</th></tr></thead>
                  <tbody><tr v-for="conteneur in conteneurs" :key="conteneur.id">
                    <td class="fw-semibold">{{ conteneur.numeroContainer }}</td><td>{{ conteneur.typeContainer?.nom || '—' }}</td><td>{{ conteneur.description || '—' }}</td>
                    <td v-if="peutEditer || peutRetirer" class="text-end text-nowrap">
                      <button v-if="peutEditer" type="button" class="btn btn-sm btn-outline-primary me-2" :disabled="retraitId !== null" @click="ouvrirModal(conteneur)">Modifier<span class="visually-hidden"> {{ conteneur.numeroContainer }}</span></button>
                      <button v-if="peutRetirer" type="button" class="btn btn-sm btn-outline-danger" :disabled="retraitId !== null" @click="retirer(conteneur)">{{ retraitId === conteneur.id ? 'Retrait…' : 'Retirer du dossier' }}<span class="visually-hidden"> {{ conteneur.numeroContainer }}</span></button>
                    </td>
                  </tr></tbody>
                </table>
              </div>
            </section>
          </template>
        </template>
      </div>
    </main>
    <ModalConteneur :show="showModal" :dossier="dossierSelectionne" :conteneur-a-editer="conteneurAEditer" @close="showModal = false" @saved="onConteneurEnregistre" />
  </div>
</template>

<style scoped>
.assignment-main { margin-left: 280px; min-width: 0; }
.assignment-context { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1.25rem; border: 1px solid #d7e2ef; border-left: 4px solid #356493; border-radius: .75rem; background: #fff; }
th { font-size: .75rem; color: #526175; }
td { padding: 1rem; }
@media (max-width: 767px) { .assignment-context { align-items: flex-start; flex-direction: column; } }
</style>

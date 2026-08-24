<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '../components/inc/Header.vue'
import Skeleton from '../components/inc/Skeleton.vue'
import Marchandise from '../models/Marchandise'
import MarchandiseService from '../services/MarchandiseService'
import OrdreTransitService from '../services/OrdreTransitService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const route = useRoute()

const rechercheNumeroOt = ref('')
const rechercheLoading = ref(false)
const selectedOt = ref(null)

const marchandises = ref([])
const loadingMarchandises = ref(false)

const showModal = ref(false)
const modalEditionMode = ref(false)
const formEnCours = ref(false)

const formModel = ref(new Marchandise())
const unites = ref([])
const natures = ref([])

const peutEditer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
})

onMounted(async () => {
    try {
        const [uList, nList] = await Promise.all([
            MarchandiseService.getUniteMesures(),
            MarchandiseService.getNatureMarchandises()
        ])
        unites.value = uList
        natures.value = nList
    } catch (e) {
        console.error('Erreur chargement référentiels marchandises', e)
    }

    const otIdParam = route.query.otId
    if (otIdParam) {
        chargerOtParIdDirect(otIdParam)
    }
})

async function chargerOtParIdDirect(id) {
    rechercheLoading.value = true
    try {
        const ot = await OrdreTransitService.getOrdreTransitById(id)
        selectedOt.value = ot
        rechercheNumeroOt.value = ot.numeroOrdreTransit
        chargerMarchandisesDuSelectedOt()
    } catch (e) {
        console.error('Impossible de charger l\'OT direct', e)
    } finally {
        rechercheLoading.value = false
    }
}

async function rechercherOt() {
    if (!rechercheNumeroOt.value) return
    rechercheLoading.value = true
    selectedOt.value = null
    marchandises.value = []
    try {
        const otsResult = await OrdreTransitService.searchOrdreTransits(0, 1, 'date_creation', 'desc', {
            numero: rechercheNumeroOt.value
        })
        if (otsResult.content && otsResult.content.length > 0) {
            selectedOt.value = otsResult.content[0]
            chargerMarchandisesDuSelectedOt()
        } else {
            alert('Aucun ordre de transit trouvé sous ce numéro.')
        }
    } catch (e) {
        alert('Erreur technique lors de la recherche.')
    } finally {
        rechercheLoading.value = false
    }
}

async function chargerMarchandisesDuSelectedOt() {
    if (!selectedOt.value) return
    loadingMarchandises.value = true
    try {
        marchandises.value = await MarchandiseService.getMarchandisesByOtId(selectedOt.value.id)
    } catch (e) {
        console.error('Erreur chargement marchandises', e)
    } finally {
        loadingMarchandises.value = false
    }
}

function ouvrirAjoutModal() {
    modalEditionMode.value = false
    formModel.value = new Marchandise({
        ordreTransit: selectedOt.value,
        uniteMesure: unites.value[0] || null,
        natureMarchandise: natures.value[0] || null
    })
    showModal.value = true
}

function ouvrirEditionModal(m) {
    modalEditionMode.value = true
    formModel.value = new Marchandise({ ...m })
    showModal.value = true
}

function fermerModal() {
    showModal.value = false
}

async function soumettreFormulaire() {
    const val = formModel.value.valider()
    if (!val.valide) {
        alert(val.erreurs.join('\n'))
        return
    }

    formEnCours.value = true
    try {
        const result = await MarchandiseService.saveMarchandise(formModel.value.toJSON())
        if (modalEditionMode.value) {
            const index = marchandises.value.findIndex(item => item.id === result.id)
            if (index !== -1) {
                marchandises.value[index] = result
            }
        } else {
            marchandises.value.push(result)
        }
        fermerModal()
        alert('Marchandise enregistrée.')
    } catch (e) {
        alert('Erreur lors de l\'enregistrement de la marchandise.')
    } finally {
        formEnCours.value = false
    }
}

async function supprimerMarchandise(m) {
    if (confirm('Voulez-vous supprimer cette ligne de marchandise définitivement ?')) {
        loadingMarchandises.value = true
        try {
            await MarchandiseService.deleteMarchandise(m.id)
            marchandises.value = marchandises.value.filter(item => item.id !== m.id)
            alert('Marchandise retirée avec succès.')
        } catch (e) {
            alert('Erreur technique lors de la suppression.')
        } finally {
            loadingMarchandises.value = false
        }
    }
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-4">
                    <h1 class="h4 fw-bold text-dark mb-1">Marchandises par Ordre de Transit</h1>
                    <p class="text-secondary mb-0 small">Gestion, conditionnement et détails logistiques des marchandises.</p>
                </div>

                <div class="card border shadow-sm mb-3">
                    <div class="card-body p-3">
                        <label class="form-label small fw-semibold text-secondary">RECHERCHER ET SÉLECTIONNER UN ORDRE DE TRANSIT</label>
                        <div class="row g-2 align-items-center">
                            <div class="col-md-6">
                                <div class="input-group input-group-sm">
                                    <input type="number" v-model="rechercheNumeroOt" class="form-control"
                                        placeholder="Saisissez un numéro d'OT (ex: 10001)..."
                                        @keyup.enter="rechercherOt">
                                    <button class="btn btn-primary" type="button" @click="rechercherOt" :disabled="rechercheLoading">
                                        <span v-if="rechercheLoading" class="spinner-border spinner-border-sm me-1" role="status"></span>
                                        Rechercher
                                    </button>
                                </div>
                            </div>
                            <div class="col-md-6 text-end">
                                <span v-if="selectedOt" class="badge bg-secondary text-white">
                                    OT sélectionné : {{ selectedOt.numeroOrdreTransit }} (Client: {{ selectedOt.client?.nom }})
                                </span>
                                <span v-else class="text-secondary small">
                                    Aucun OT sélectionné. Veuillez faire une recherche.
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="selectedOt" class="card border shadow-sm">
                    <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center py-2">
                        <h5 class="mb-0 fw-semibold text-dark">Contenu du Transit</h5>
                        <button v-if="peutEditer" class="btn btn-primary btn-sm" @click="ouvrirAjoutModal">
                            + Ajouter une marchandise
                        </button>
                    </div>
                    <div class="card-body p-0">
                        <div v-if="loadingMarchandises" class="p-3">
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

                        <div v-else-if="marchandises.length === 0" class="text-center py-5 text-secondary small">
                            Aucune marchandise n'est associée à cet ordre de transit. Cliquez sur le bouton "Ajouter une marchandise" pour commencer.
                        </div>

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
                                            <button class="btn btn-outline-secondary btn-sm me-1" @click="ouvrirEditionModal(m)">Éditer</button>
                                            <button class="btn btn-outline-danger btn-sm" @click="supprimerMarchandise(m)">Supprimer</button>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </main>

        <div class="modal fade show" v-if="showModal" tabIndex="-1" style="display: block; background: rgba(0,0,0,0.4);" aria-modal="true" role="dialog">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border shadow">
                    <div class="modal-header bg-light border-bottom">
                        <h5 class="modal-title fw-semibold">{{ modalEditionMode ? 'Éditer' : 'Ajouter une marchandise' }}</h5>
                        <button type="button" class="btn-close" @click="fermerModal"></button>
                    </div>
                    <div class="modal-body">
                        <form @submit.prevent="soumettreFormulaire">
                            <div class="row g-3">
                                <div class="col-12">
                                    <label class="form-label small fw-semibold text-secondary">Description de la marchandise <span class="text-danger">*</span></label>
                                    <textarea v-model="formModel.description" class="form-control form-control-sm" rows="3" required></textarea>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Nature Marchandise <span class="text-danger">*</span></label>
                                    <select v-model="formModel.natureMarchandise" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez la nature</option>
                                        <option v-for="n in natures" :key="n.id" :value="n">{{ n.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Unité de Mesure <span class="text-danger">*</span></label>
                                    <select v-model="formModel.uniteMesure" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez l'unité</option>
                                        <option v-for="u in unites" :key="u.id" :value="u">{{ u.nom }}</option>
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
                                    <label class="form-label small fw-semibold text-secondary">Quantité <span class="text-danger">*</span></label>
                                    <input type="number" v-model="formModel.quantite" class="form-control form-control-sm" required min="1">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-semibold text-secondary">Poids (kg)</label>
                                    <input type="number" step="0.01" v-model="formModel.poids" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label small fw-semibold text-secondary">Volume (m³)</label>
                                    <input type="number" step="0.01" v-model="formModel.volume" class="form-control form-control-sm">
                                </div>
                            </div>
                            <div class="d-flex justify-content-end gap-2 mt-4">
                                <button type="button" class="btn btn-secondary btn-sm" @click="fermerModal">Annuler</button>
                                <button type="submit" class="btn btn-primary btn-sm" :disabled="formEnCours">
                                    <span v-if="formEnCours" class="spinner-border spinner-border-sm me-1" role="status"></span>
                                    Enregistrer
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
</style>
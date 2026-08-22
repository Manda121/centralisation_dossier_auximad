<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- Page Title -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h3 fw-bold text-primary mb-1">Marchandises par Ordre de Transit</h1>
                        <p class="text-secondary mb-0">Gestion, conditionnement et détails logistiques des marchandises.
                        </p>
                    </div>
                </div>

                <!-- SELECTEUR D'ORDRE DE TRANSIT (Par Recherche Numéro) -->
                <div class="card border-0 shadow-sm mb-4">
                    <div class="card-body p-4">
                        <label class="form-label text-xs fw-bold text-secondary">RECHERCHER ET SÉLECTIONNER UN ORDRE DE
                            TRANSIT</label>
                        <div class="row g-3 align-items-center">
                            <div class="col-md-6">
                                <div class="input-group input-group-sm">
                                    <span class="input-group-text bg-white border-end-0">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14"
                                            fill="currentColor" class="text-muted" viewBox="0 0 16 16">
                                            <path
                                                d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001q.044.06.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1 1 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0" />
                                        </svg>
                                    </span>
                                    <input type="number" v-model="rechercheNumeroOt"
                                        class="form-control border-start-0 ps-0"
                                        placeholder="Saisissez un numéro d'OT (ex: 10001)..."
                                        @keyup.enter="rechercherOt">
                                    <button class="btn btn-primary" type="button" @click="rechercherOt"
                                        :disabled="rechercheLoading">
                                        Rechercher
                                    </button>
                                </div>
                            </div>
                            <div class="col-md-6 text-end">
                                <span v-if="selectedOt"
                                    class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2 fs-7">
                                    OT sélectionné : OT-{{ selectedOt.numeroOrdreTransit }} (Client: {{
                                    selectedOt.client?.nom }})
                                </span>
                                <span v-else class="text-warning text-xs fw-bold">
                                    Auncun OT sélectionné. Veuillez faire une recherche.
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- CARGO LIST FOR SELECTED OT -->
                <div v-if="selectedOt" class="card border-0 shadow-sm p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
                        <h5 class="fw-bold mb-0">Contenu du Transit</h5>
                        <button v-if="peutEditer" class="btn btn-primary btn-sm d-flex align-items-center gap-1"
                            @click="ouvrirAjoutModal">
                            + Ajouter une marchandise
                        </button>
                    </div>

                    <div v-if="loadingMarchandises" class="text-center py-4">
                        <div class="spinner-border text-primary" role="status"></div>
                    </div>

                    <div v-else-if="marchandises.length === 0" class="text-center py-5 text-muted text-xs">
                        Aucune marchandise n'est associée à cet ordre de transit. Cliquez sur le bouton "Ajouter une
                        marchandise" pour commencer.
                    </div>

                    <div v-else class="table-responsive">
                        <table class="table table-hover align-middle rounded-3 overflow-hidden text-xs">
                            <thead class="table-dark">
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
                                    <td class="fw-semibold text-dark">{{ m.description }}</td>
                                    <td>
                                        <span class="badge bg-light text-secondary border border-secondary-subtle">
                                            {{ m.natureMarchandise?.nom }}
                                        </span>
                                    </td>
                                    <td>{{ m.marque }} / {{ m.modele || '—' }}</td>
                                    <td class="text-center fw-bold">{{ m.quantite }}</td>
                                    <td class="text-center">{{ m.poids || '—' }}</td>
                                    <td class="text-center">{{ m.volume || '—' }}</td>
                                    <td class="text-center text-muted">{{ m.uniteMesure?.nom }}</td>
                                    <td v-if="peutEditer" class="text-end">
                                        <div class="d-inline-flex gap-2">
                                            <button class="btn btn-secondary btn-xs"
                                                @click="ouvrirEditionModal(m)">Éditer</button>
                                            <button class="btn btn-outline-danger btn-xs"
                                                @click="supprimerMarchandise(m)">Supprimer</button>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </main>

        <!-- MODAL AJOUT/EDITION MARCHANDISE (Simulée via backdrop Bootstrap propre) -->
        <div class="modal fade show" v-if="showModal" tabIndex="-1" style="display: block; background: rgba(0,0,0,0.5);"
            aria-modal="true" role="dialog">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title fw-bold fs-6">{{ modalEditionMode ? 'Éditer' : 'Ajouter une marchandise'
                        }}</h5>
                        <button type="button" class="btn-close btn-close-white" @click="fermerModal"></button>
                    </div>
                    <div class="modal-body p-4 text-xs">
                        <form @submit.prevent="soumettreFormulaire">
                            <div class="row g-3">
                                <div class="col-md-12">
                                    <label class="form-label fw-semibold">Description de la marchandise <span
                                            class="text-danger">*</span></label>
                                    <textarea v-model="formModel.description" class="form-control form-control-sm"
                                        rows="3" required></textarea>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Nature Marchandise <span
                                            class="text-danger">*</span></label>
                                    <select v-model="formModel.natureMarchandise" class="form-select form-select-sm"
                                        required>
                                        <option :value="null">Sélectionnez la nature</option>
                                        <option v-for="n in natures" :key="n.id" :value="n">{{ n.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Unité de Mesure <span
                                            class="text-danger">*</span></label>
                                    <select v-model="formModel.uniteMesure" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez l'unité</option>
                                        <option v-for="u in unites" :key="u.id" :value="u">{{ u.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Marque</label>
                                    <input type="text" v-model="formModel.marque" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label fw-semibold">Modèle</label>
                                    <input type="text" v-model="formModel.modele" class="form-control form-control-sm">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label fw-semibold">Quantité <span
                                            class="text-danger">*</span></label>
                                    <input type="number" v-model="formModel.quantite"
                                        class="form-control form-control-sm" required min="1">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label fw-semibold">Poids (kg)</label>
                                    <input type="number" step="0.01" v-model="formModel.poids"
                                        class="form-control form-control-sm">
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label fw-semibold">Volume (m³)</label>
                                    <input type="number" step="0.01" v-model="formModel.volume"
                                        class="form-control form-control-sm">
                                </div>
                            </div>
                            <div class="modal-footer border-0 px-0 pb-0 mt-4">
                                <button type="button" class="btn btn-secondary btn-sm"
                                    @click="fermerModal">Annuler</button>
                                <button type="submit" class="btn btn-primary btn-sm"
                                    :disabled="formEnCours">Enregistrer</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '../components/inc/Header.vue'
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

    // charger depuis l'URL si otId y est
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
        rechercheNumeroOt.value = ot.numeroOrdreTransit // préremplir la recherche
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
        // Appeler le service de recherche d'OT avec filtres exacts
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
    // copie superficielle
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
                // Remplacer directement
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

<style scoped>
.text-xs {
    font-size: 0.75rem;
}

.text-xxs {
    font-size: 0.65rem;
}

.btn-xs {
    padding: 0.25rem 0.5rem;
    font-size: 0.75rem;
    border-radius: 4px;
}

.fs-7 {
    font-size: 0.75rem;
}
</style>
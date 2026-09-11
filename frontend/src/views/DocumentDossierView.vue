<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">

                <div class="mb-4">
                    <h1 class="h4 fw-bold text-primary mb-1">📄 Gestion des Documents</h1>
                    <p class="text-secondary small mb-0">Associer des documents aux dossiers clients</p>
                </div>

                <!-- ═══ Alertes remises urgentes ═══ -->
                <div v-if="alertesRemise.length > 0" class="mb-4">
                    <div class="alert alert-warning border-warning d-flex align-items-start gap-3 py-3">
                        <svg viewBox="0 0 16 16" fill="currentColor" width="20" height="20"
                            class="flex-shrink-0 text-warning mt-1">
                            <path
                                d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5m.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2" />
                        </svg>
                        <div class="flex-grow-1">
                            <div class="fw-semibold">⚠️ {{ alertesRemise.length }} dossier(s) avec remise urgent</div>
                            <div class="small mt-1">
                                <span v-for="d in alertesRemise.slice(0, 5)" :key="d.id" class="me-3">
                                    <a href="#" @click.prevent="selectionnerDossier(d)"
                                        class="text-warning-emphasis fw-semibold">
                                        {{ d.numeroDossier }}
                                    </a>
                                    — limite : {{ formatarDate(d.dateLimiteRemise) }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row g-4">
                    <!-- ═══ Panneau gauche : recherche + liste dossiers ═══ -->
                    <div class="col-lg-5">
                        <div class="card border-0 shadow-sm">
                            <div class="card-header bg-white border-bottom py-3">
                                <h6 class="fw-semibold mb-0">Sélectionner un dossier</h6>
                            </div>
                            <div class="card-body p-3">
                                <!-- Filtres multicritères -->
                                <div class="row g-2 mb-3">
                                    <div class="col-12">
                                        <input type="text" v-model="filtres.query" class="form-control form-control-sm"
                                            placeholder="🔍 N° dossier, nom client…" @input="rechercherDossiers" />
                                    </div>
                                    <div class="col-6">
                                        <input type="text" v-model="filtres.cpClient"
                                            class="form-control form-control-sm" placeholder="CP Client"
                                            @input="rechercherDossiers" />
                                    </div>
                                    <div class="col-6">
                                        <select v-model="filtres.typeProductionId" class="form-select form-select-sm"
                                            @change="rechercherDossiers">
                                            <option :value="null">Tous types</option>
                                            <option v-for="tp in typeProductions" :key="tp.id" :value="tp.id">{{ tp.nom
                                                }}</option>
                                        </select>
                                    </div>
                                    <div class="col-12">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="checkbox" id="filtreSansDocs"
                                                v-model="filtres.sansDocsComplets" @change="rechercherDossiers" />
                                            <label class="form-check-label small" for="filtreSansDocs">
                                                Documents incomplets
                                            </label>
                                        </div>
                                    </div>
                                </div>

                                <!-- Liste des dossiers -->
                                <div v-if="chargementDossiers" class="text-center py-3">
                                    <span class="spinner-border spinner-border-sm text-primary"></span>
                                </div>
                                <div v-else class="dossier-list">
                                    <div v-if="dossiers.length === 0"
                                        class="text-secondary small text-center py-3 fst-italic">
                                        Aucun dossier trouvé
                                    </div>
                                    <div v-for="d in dossiers" :key="d.id" class="dossier-item"
                                        :class="{ 'dossier-item--active': dossierSelectionne?.id === d.id }"
                                        @click="selectionnerDossier(d)">
                                        <div class="d-flex justify-content-between align-items-start">
                                            <div>
                                                <div class="fw-semibold text-dark" style="font-size:0.85rem;">{{
                                                    d.numeroDossier }}</div>
                                                <div class="text-secondary" style="font-size:0.75rem;">
                                                    {{ d.client?.nom }} {{ d.client?.prenom }}
                                                    <span v-if="d.client?.cpClient"
                                                        class="ms-1 badge bg-light text-secondary border">{{
                                                        d.client.cpClient }}</span>
                                                </div>
                                            </div>
                                            <div class="d-flex flex-column align-items-end gap-1">
                                                <span class="badge"
                                                    :class="docsComplets(d) ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'">
                                                    {{ docsComplets(d) ? '✓ OK' : '! Docs' }}
                                                </span>
                                                <span v-if="d.dateLimiteRemise"
                                                    class="badge bg-danger-subtle text-danger"
                                                    style="font-size:0.65rem;">
                                                    ⏰ {{ formatarDateCourte(d.dateLimiteRemise) }}
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- Pagination -->
                                    <div v-if="pagination.totalPages > 1"
                                        class="d-flex justify-content-center gap-2 mt-3">
                                        <button class="btn btn-outline-secondary btn-sm"
                                            :disabled="pagination.page === 0" @click="changerPage(-1)">‹</button>
                                        <span class="small text-secondary align-self-center">{{ pagination.page + 1 }} /
                                            {{ pagination.totalPages }}</span>
                                        <button class="btn btn-outline-secondary btn-sm"
                                            :disabled="pagination.page >= pagination.totalPages - 1"
                                            @click="changerPage(1)">›</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- ═══ Panneau droit : détail + documents ═══ -->
                    <div class="col-lg-7">
                        <div v-if="!dossierSelectionne"
                            class="card border-0 shadow-sm h-100 d-flex align-items-center justify-content-center">
                            <div class="text-center text-secondary py-5">
                                <svg viewBox="0 0 16 16" fill="currentColor" width="40" height="40"
                                    class="text-muted mb-3 opacity-50">
                                    <path
                                        d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z" />
                                </svg>
                                <p class="mb-0">Sélectionnez un dossier pour gérer ses documents</p>
                            </div>
                        </div>

                        <div v-else class="card border-0 shadow-sm">
                            <div
                                class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
                                <div>
                                    <h6 class="fw-bold mb-0 text-primary">{{ dossierSelectionne.numeroDossier }}</h6>
                                    <div class="text-secondary small">
                                        {{ dossierSelectionne.client?.nom }} {{ dossierSelectionne.client?.prenom }}
                                        <span v-if="dossierSelectionne.typeProduction"> — {{
                                            dossierSelectionne.typeProduction?.nom }}</span>
                                    </div>
                                </div>
                                <router-link :to="`/dossiers/${dossierSelectionne.id}`"
                                    class="btn btn-outline-primary btn-sm">
                                    Voir le dossier →
                                </router-link>
                            </div>

                            <!-- Infos clés du dossier -->
                            <div class="card-body border-bottom pb-3">
                                <div class="row g-2 small">
                                    <div class="col-md-4">
                                        <div class="text-secondary text-uppercase" style="font-size:0.7rem;">Phase</div>
                                        <div class="fw-semibold">{{ dossierSelectionne.phaseDossier?.nom || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="text-secondary text-uppercase" style="font-size:0.7rem;">Statut
                                        </div>
                                        <div class="fw-semibold">{{ dossierSelectionne.statutDossier?.nom || '—' }}
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="text-secondary text-uppercase" style="font-size:0.7rem;">Date limite
                                            remise</div>
                                        <div class="fw-semibold" :class="dateLimiteUrgente ? 'text-danger' : ''">
                                            {{ dossierSelectionne.dateLimiteRemise ?
                                                formatarDate(dossierSelectionne.dateLimiteRemise) : '—' }}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Section documents -->
                            <div class="card-body">
                                <h6 class="fw-semibold mb-3">Documents nécessaires</h6>
                                <DocumentsSection :dossierId="dossierSelectionne.id"
                                    :typeProductionId="dossierSelectionne.typeProduction?.id"
                                    @documents-valides="onDocumentsValides"
                                    @docs-obligatoires-complets="onObligatoiresComplets" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Header from '@/components/inc/Header.vue'
import DocumentsSection from '@/components/DocumentsSection.vue'
import DossierService from '@/services/DossierService'
import DocumentService from '@/services/DocumentService'
import OrdreTransitService from '@/services/OrdreTransitService'

const dossiers = ref([])
const dossierSelectionne = ref(null)
const alertesRemise = ref([])
const chargementDossiers = ref(false)
const typeProductions = ref([])

const filtres = ref({
    query: '',
    cpClient: '',
    typeProductionId: null,
    sansDocsComplets: false
})

const pagination = ref({
    page: 0,
    size: 15,
    totalPages: 0
})

const dossiersAvecDocs = ref(new Set()) // IDs dossiers avec docs complets

onMounted(async () => {
    await Promise.all([
        rechercherDossiers(),
        chargerAlertes(),
        chargerTypeProductions()
    ])
})

async function chargerTypeProductions() {
    try {
        typeProductions.value = await OrdreTransitService.getAllTypeProductions()
    } catch (e) {
        console.error('Erreur chargement types de production', e)
    }
}

async function chargerAlertes() {
    try {
        alertesRemise.value = await DocumentService.getDossiersRemiseUrgente(7)
    } catch (e) {
        console.error('Erreur alertes', e)
    }
}

let rechercheTimeout = null
async function rechercherDossiers() {
    clearTimeout(rechercheTimeout)
    rechercheTimeout = setTimeout(async () => {
        chargementDossiers.value = true
        try {
            if (filtres.value.sansDocsComplets) {
                const res = await DocumentService.getDossiersSansDocuments()
                dossiers.value = res
                pagination.value = { page: 0, totalPages: 1, size: 50 }
            } else {
                const filters = {}
                if (filtres.value.query) filters.query = filtres.value.query
                if (filtres.value.typeProductionId) filters.typeProductionId = filtres.value.typeProductionId
                if (filtres.value.cpClient) filters.cpClient = filtres.value.cpClient

                const res = await DossierService.searchDossiers(
                    pagination.value.page, pagination.value.size,
                    'date_creation', 'desc', filters
                )
                dossiers.value = res.content || []
                pagination.value.totalPages = res.totalPages || 0
            }
        } catch (e) {
            console.error('Erreur recherche dossiers', e)
        } finally {
            chargementDossiers.value = false
        }
    }, 300)
}

async function selectionnerDossier(d) {
    dossierSelectionne.value = d
}

function changerPage(delta) {
    pagination.value.page = Math.max(0, pagination.value.page + delta)
    rechercherDossiers()
}

function docsComplets(dossier) {
    // On marque comme complets si pas dans la liste des incomplets
    // (approximatif côté frontend, la vraie vérif est côté backend)
    return !alertesRemise.value.find(a => a.id === dossier.id)
}

const dateLimiteUrgente = computed(() => {
    if (!dossierSelectionne.value?.dateLimiteRemise) return false
    const limite = new Date(dossierSelectionne.value.dateLimiteRemise)
    const dans7j = new Date()
    dans7j.setDate(dans7j.getDate() + 7)
    return limite <= dans7j
})

function onDocumentsValides({ succes }) {
    if (succes) {
        rechercherDossiers()
    }
}

function onObligatoiresComplets(complet) {
    // Mise à jour UI si besoin
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    })
}

function formatarDateCourte(dt) {
    if (!dt) return ''
    return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })
}
</script>

<style scoped>
.dossier-list {
    max-height: 500px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.dossier-item {
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid transparent;
    transition: all 0.15s;
}

.dossier-item:hover {
    background: #f0f4ff;
    border-color: #c7d7ff;
}

.dossier-item--active {
    background: #e8f0fe;
    border-color: #3b6a9c;
}

.dossier-item--active .fw-semibold {
    color: #2c5f8a;
}
</style>

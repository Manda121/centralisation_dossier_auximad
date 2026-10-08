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
                            <div class="fw-semibold">⚠️ {{ alertesRemise.length }} remise(s) de document(s) urgente(s)</div>
                            <div class="small mt-1">
                                <span v-for="r in alertesRemise.slice(0, 5)"
                                    :key="`${r.dossier?.id}-${r.typeDocument?.id}`" class="me-3">
                                    <a href="#" @click.prevent="selectionnerDossier(r.dossier)"
                                        class="text-warning-emphasis fw-semibold">
                                        {{ r.dossier?.numeroDossier }}
                                    </a>
                                    — remise : {{ formatarDate(r.dateRemise) }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row g-4">
                    <!-- ═══ Panneau gauche : recherche + liste dossiers ═══ -->
                    <div class="col-lg-5">
                        <div class="card border-0 shadow-sm">
                            <div class="card-header bg-white border-bottom py-3 d-flex flex-wrap gap-2 justify-content-between align-items-center">
                                <h6 class="fw-semibold mb-0">Sélectionner un dossier</h6>
                                <ExportActions title="Liste des dossiers - documents" :columns="colonnesExportDocuments" :load-rows="chargerToutesLesLignes" />
                            </div>
                            <div class="card-body p-3">
                                <p class="small text-secondary mb-2">{{ modeListe === 'incomplets' ? 'Dossiers avec au moins un document manquant' : 'Recherche dans tous les dossiers' }}</p>
                                <form class="row g-2 mb-3" @submit.prevent="appliquerFiltres">
                                    <div class="col-12">
                                        <label class="form-label small mb-1" for="doc-query">Recherche libre</label>
                                        <input id="doc-query" type="text" v-model="filtres.query" class="form-control form-control-sm" placeholder="N° dossier, client, lieu…" />
                                    </div>
                                    <div v-for="champ in filtresTextePrincipaux" :key="champ.key" class="col-6">
                                        <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                        <input :id="`doc-${champ.key}`" v-model="filtres[champ.key]" type="text" class="form-control form-control-sm" />
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label small mb-1" for="doc-agence">Agence</label>
                                        <select id="doc-agence" v-model="filtres.agenceId" class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="a in agences" :key="a.id" :value="a.id">{{ a.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-6">
                                        <label class="form-label small mb-1" for="doc-production">Type de production</label>
                                        <select id="doc-production" v-model="filtres.typeProductionId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="tp in typeProductions" :key="tp.id" :value="tp.id">{{ tp.nom }}</option>
                                        </select>
                                    </div>
                                    <details class="col-12 mt-2">
                                        <summary class="small fw-semibold text-primary">Filtres avancés</summary>
                                        <div class="row g-2 mt-1">
                                            <div v-for="champ in filtresTexteAvances" :key="champ.key" class="col-6">
                                                <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                                <input :id="`doc-${champ.key}`" v-model="filtres[champ.key]" type="text" class="form-control form-control-sm" />
                                            </div>
                                            <div v-for="champ in filtresNumeriques" :key="champ.key" class="col-6">
                                                <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                                <input :id="`doc-${champ.key}`" v-model="filtres[champ.key]" type="number" :min="champ.min ?? 0" :step="champ.step || 1" class="form-control form-control-sm" />
                                            </div>
                                            <div v-for="champ in filtresDates" :key="champ.key" class="col-6">
                                                <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                                <input :id="`doc-${champ.key}`" v-model="filtres[champ.key]" type="date" class="form-control form-control-sm" />
                                            </div>
                                            <div v-for="champ in filtresSelects" :key="champ.key" class="col-6">
                                                <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                                <select :id="`doc-${champ.key}`" v-model="filtres[champ.key]" class="form-select form-select-sm">
                                                    <option value="">Tous</option>
                                                    <option v-for="option in champ.options.value" :key="option.id" :value="option.id">{{ option.nom }}</option>
                                                </select>
                                            </div>
                                            <div v-for="champ in filtresBooleens" :key="champ.key" class="col-6">
                                                <label class="form-label small mb-1" :for="`doc-${champ.key}`">{{ champ.label }}</label>
                                                <select :id="`doc-${champ.key}`" v-model="filtres[champ.key]" class="form-select form-select-sm">
                                                    <option value="">Tous</option><option value="true">Oui</option><option value="false">Non</option>
                                                </select>
                                            </div>
                                            <div class="col-6">
                                                <label class="form-label small mb-1" for="doc-type-dossier">Type de dossier</label>
                                                <select id="doc-type-dossier" v-model="filtres.typeDossier" class="form-select form-select-sm">
                                                    <option value="">Tous</option><option value="INTERAGENCE">Inter-agences</option><option value="MERE">Mères</option><option value="SOUS_DOSSIER">Sous-dossiers</option>
                                                </select>
                                            </div>
                                        </div>
                                    </details>
                                    <div class="col-12 d-flex flex-wrap gap-2 mt-3">
                                        <button type="submit" class="btn btn-primary btn-sm">Rechercher</button>
                                        <button type="button" class="btn btn-outline-secondary btn-sm" @click="reinitialiserFiltres">Réinitialiser</button>
                                        <button v-if="modeListe !== 'tous'" type="button" class="btn btn-outline-primary btn-sm" @click="afficherTous">Afficher tous</button>
                                    </div>
                                </form>

                                <!-- Liste des dossiers -->
                                <div v-if="chargementDossiers" class="text-center py-3">
                                    <span class="spinner-border spinner-border-sm text-primary"></span>
                                </div>
                                <div v-else class="dossier-list">
                                    <p v-if="erreurDossiers" class="alert alert-danger small" role="alert">{{ erreurDossiers }}</p>
                                    <div v-if="dossiers.length === 0 && !erreurDossiers"
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
                                                <span class="badge" :class="classeEtatDocuments(d)" :title="libelleEtatDocuments(d)">
                                                    {{ libelleEtatDocuments(d) }}
                                                </span>
                                                <span v-if="remisesParDossier[d.id]"
                                                    class="badge bg-danger-subtle text-danger"
                                                    style="font-size:0.65rem;">
                                                    ⏰ {{ formatarDateCourte(remisesParDossier[d.id]) }}
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
                                        <div class="text-secondary text-uppercase" style="font-size:0.7rem;">Prochaine remise de doc</div>
                                        <div class="fw-semibold" :class="remiseUrgenteSelection ? 'text-danger' : ''">
                                            {{ remiseUrgenteSelection ?
                                                formatarDate(remiseUrgenteSelection.dateRemise) : '—' }}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Section documents -->
                            <div class="card-body">
                                <h6 class="fw-semibold mb-3">Documents nécessaires</h6>
                                <DocumentsSection :dossierId="dossierSelectionne.id"
                                    :typeProductionId="dossierSelectionne.typeProduction?.id"
                                    :natureMarchandiseId="dossierSelectionne.ordreTransit?.natureMarchandise?.id"
                                    @documents-valides="onDocumentsValides" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/inc/Header.vue'
import DocumentsSection from '@/components/DocumentsSection.vue'
import DossierService from '@/services/DossierService'
import ClientService from '@/services/ClientService'
import DocumentService from '@/services/DocumentService'
import OrdreTransitService from '@/services/OrdreTransitService'
import AgenceService from '@/services/AgenceService'
import ExportActions from '@/components/inc/ExportActions.vue'

const route = useRoute()
const router = useRouter()

const dossiers = ref([])
const dossierSelectionne = ref(null)
const alertesRemise = ref([])
const remisesParDossier = ref({})
const chargementDossiers = ref(false)
const erreurDossiers = ref('')
const typeProductions = ref([])
const agences = ref([])
const phases = ref([])
const statuts = ref([])
const pays = ref([])
const incontermes = ref([])

const filtresInitiaux = {
    query: '', numeroDossier: '', clientNom: '', cpClient: '', agenceId: '', typeProductionId: '',
    numeroOt: '', paysOrigineId: '', paysDestinationId: '', incontermeId: '', pourCompte: '',
    depart: '', adresseLivraison: '', lieuPaiementFret: '', estPrepaye: '', estProvisionne: '',
    provisionMin: '', provisionMax: '', echeanceMin: '', echeanceMax: '',
    dateLivraisonMin: '', dateLivraisonMax: '', phaseId: '', statutId: '', typeDossier: '',
    estGroupage: '', dateMin: '', dateMax: ''
}
const filtres = ref({ ...filtresInitiaux })
const filtresAppliques = ref({})
const afficherTousSansFiltre = ref(false)
const modeListe = computed(() => Object.keys(filtresAppliques.value).length > 0 || afficherTousSansFiltre.value ? 'tous' : 'incomplets')
const filtresTextePrincipaux = [
    { key: 'numeroDossier', label: 'N° dossier' }, { key: 'clientNom', label: 'Nom client' },
    { key: 'cpClient', label: 'CP client' }
]
const filtresTexteAvances = [
    { key: 'pourCompte', label: 'Pour compte' }, { key: 'depart', label: 'Lieu de départ' },
    { key: 'adresseLivraison', label: 'Adresse de livraison' },
    { key: 'lieuPaiementFret', label: 'Paiement du fret' }
]
const filtresNumeriques = [
    { key: 'numeroOt', label: 'N° ordre de transit' },
    { key: 'provisionMin', label: 'Provision min.', step: '0.01' },
    { key: 'provisionMax', label: 'Provision max.', step: '0.01' }
]
const filtresDates = [
    { key: 'echeanceMin', label: 'Échéance du' }, { key: 'echeanceMax', label: 'Échéance au' },
    { key: 'dateLivraisonMin', label: 'Livraison du' }, { key: 'dateLivraisonMax', label: 'Livraison au' },
    { key: 'dateMin', label: 'Création du' }, { key: 'dateMax', label: 'Création au' }
]
const filtresSelects = [
    { key: 'phaseId', label: 'Phase', options: phases },
    { key: 'statutId', label: 'Statut', options: statuts },
    { key: 'paysOrigineId', label: 'Pays d’origine', options: pays },
    { key: 'paysDestinationId', label: 'Pays de destination', options: pays },
    { key: 'incontermeId', label: 'Incoterm', options: incontermes }
]
const filtresBooleens = [
    { key: 'estPrepaye', label: 'Prépayé' },
    { key: 'estProvisionne', label: 'Provisionné' },
    { key: 'estGroupage', label: 'Groupage' }
]

const pagination = ref({
    page: 0,
    size: 15,
    totalPages: 0
})

const colonnesExportDocuments = [
    { key: 'numeroDossier', label: 'N° dossier' },
    { key: 'client.nom', label: 'Client', get: d => [d.client?.nom, d.client?.prenom].filter(Boolean).join(' ') },
    { key: 'client.cpClient', label: 'C.P. client' },
    { key: 'agence.nom', label: 'Agence' },
    { key: 'phaseDossier.nom', label: 'Phase' },
    { key: 'statutDossier.nom', label: 'Statut' },
    { key: 'etatDocuments', label: 'Documents', get: libelleEtatDocuments },
    { key: 'dateCreation', label: 'Création', format: 'date' }
]

async function chargerToutesLesLignes() {
    const params = { ...filtresAppliques.value }
    if (modeListe.value === 'incomplets') params.documentsIncomplets = true
    const resultats = []
    let page = 0
    let nombrePages = 1
    while (page < nombrePages) {
        const reponse = await DossierService.searchDossiers(page, 200, 'date_creation', 'desc', params)
        resultats.push(...(reponse.content || []))
        nombrePages = reponse.totalPages || 0
        page++
    }
    return resultats
}

onMounted(async () => {
    // Pré-sélection du dossier transmis par le détail dossier (?dossierId=...)
    await preselecterDossierDepuisUrl()
    await Promise.all([
        chargerDossiers(),
        chargerAlertes(),
        chargerReferentiels()
    ])
})

/** Charge et sélectionne le dossier passé dans l'URL (?dossierId=...) */
async function preselecterDossierDepuisUrl() {
    const dossierId = route.query.dossierId
    if (!dossierId) return
    try {
        const d = await DossierService.getDossierById(dossierId)
        if (!d) return
        dossierSelectionne.value = d
    } catch (e) {
        console.error('Erreur lors de la pré-sélection du dossier', e)
    }
}

// Suivre les changements de ?dossierId= sans remontage (retour/avance navigateur)
watch(() => route.query.dossierId, async (nouvelId) => {
    if (nouvelId == null) return
    if (String(dossierSelectionne.value?.id ?? '') === String(nouvelId)) return
    await preselecterDossierDepuisUrl()
})

async function chargerReferentiels() {
    const chargements = [
        [OrdreTransitService.getAllTypeProductions(), typeProductions],
        [AgenceService.getAllAgences(), agences],
        [DossierService.getAllPhases(), phases],
        [DossierService.getAllStatuts(), statuts],
        [ClientService.getAllPays(), pays],
        [DossierService.getAllIncontermes(), incontermes]
    ]
    const resultats = await Promise.allSettled(chargements.map(([promesse]) => promesse))
    resultats.forEach((resultat, index) => {
        if (resultat.status === 'fulfilled') chargements[index][1].value = resultat.value
    })
}

async function chargerAlertes() {
    try {
        // Remises urgentes depuis la table remise
        alertesRemise.value = await DocumentService.getRemisesUrgentes(7)
        // Prochaine remise par dossier (pour les badges ⏰ de la liste)
        const map = {}
        alertesRemise.value.forEach(r => {
            const dossierId = r.dossier?.id
            if (!dossierId) return
            if (!map[dossierId] || new Date(r.dateRemise) < new Date(map[dossierId])) {
                map[dossierId] = r.dateRemise
            }
        })
        remisesParDossier.value = map
    } catch (e) {
        console.error('Erreur alertes', e)
    }
}

function appliquerFiltres() {
    filtresAppliques.value = Object.fromEntries(Object.entries(filtres.value)
        .filter(([, valeur]) => valeur !== '' && valeur !== null && valeur !== undefined))
    afficherTousSansFiltre.value = false
    pagination.value.page = 0
    chargerDossiers()
}

function reinitialiserFiltres() {
    filtres.value = { ...filtresInitiaux }
    filtresAppliques.value = {}
    afficherTousSansFiltre.value = false
    pagination.value.page = 0
    chargerDossiers()
}

function afficherTous() {
    filtres.value = { ...filtresInitiaux }
    filtresAppliques.value = {}
    afficherTousSansFiltre.value = true
    pagination.value.page = 0
    chargerDossiers()
}

let numeroRequete = 0
async function chargerDossiers() {
    const requete = ++numeroRequete
    chargementDossiers.value = true
    erreurDossiers.value = ''
    const params = { ...filtresAppliques.value }
    if (modeListe.value === 'incomplets') params.documentsIncomplets = true
    try {
        const res = await DossierService.searchDossiers(
            pagination.value.page, pagination.value.size, 'date_creation', 'desc', params
        )
        if (requete !== numeroRequete) return
        dossiers.value = res.content || []
        pagination.value.totalPages = res.totalPages || 0
    } catch (e) {
        if (requete !== numeroRequete) return
        console.error('Erreur recherche dossiers', e)
        dossiers.value = []
        pagination.value.totalPages = 0
        erreurDossiers.value = 'Impossible de charger les dossiers. Réessayez.'
    } finally {
        if (requete === numeroRequete) chargementDossiers.value = false
    }
}

async function selectionnerDossier(d) {
    dossierSelectionne.value = d
    // Garder l'URL synchronisée (partage du lien / rafraîchissement)
    if (d?.id != null && String(route.query.dossierId || '') !== String(d.id)) {
        router.replace({ path: route.path, query: { ...route.query, dossierId: d.id } })
    }
}

function changerPage(delta) {
    pagination.value.page = Math.max(0, pagination.value.page + delta)
    chargerDossiers()
}

function libelleEtatDocuments(dossier) {
    if (dossier.etatDocuments === 'OBLIGATOIRES_MANQUANTS') return 'Obligatoires manquants'
    if (dossier.etatDocuments === 'FACULTATIFS_MANQUANTS') return 'Facultatifs manquants'
    if (dossier.etatDocuments === 'COMPLET') return 'Complet'
    return 'État indisponible'
}

function classeEtatDocuments(dossier) {
    if (dossier.etatDocuments === 'OBLIGATOIRES_MANQUANTS') return 'bg-danger-subtle text-danger'
    if (dossier.etatDocuments === 'FACULTATIFS_MANQUANTS') return 'bg-warning-subtle text-warning-emphasis'
    if (dossier.etatDocuments === 'COMPLET') return 'bg-success-subtle text-success'
    return 'bg-secondary-subtle text-secondary'
}

const remiseUrgenteSelection = computed(() => {
    const id = dossierSelectionne.value?.id
    return id ? (remisesParDossier.value[id] || null) : null
})

async function onDocumentsValides({ succes }) {
    if (succes) {
        const id = dossierSelectionne.value?.id
        await chargerDossiers()
        if (id) {
            try { dossierSelectionne.value = await DossierService.getDossierById(id) }
            catch (e) { console.error('Erreur actualisation dossier', e) }
        }
    }
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

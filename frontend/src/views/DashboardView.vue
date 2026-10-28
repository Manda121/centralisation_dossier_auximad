<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DashboardChart from '../components/dashboard/DashboardChart.vue'
import DashboardService from '../services/DashboardService'
import AgenceService from '../services/AgenceService'
import OrdreTransitService from '../services/OrdreTransitService'
import PermissionService from '../utils/permission.service'

const router = useRouter()

// ──────────────────────────────────────────────
// PERMISSIONS
// ──────────────────────────────────────────────
const peutVoirToutesAgences = computed(() => PermissionService.peutLireToutesAgencesOT())
const agenceUtilisateur = JSON.parse(localStorage.getItem('auximad_user_info') || '{}').idAgence

// ──────────────────────────────────────────────
// FILTRES GLOBAUX
// ──────────────────────────────────────────────
const filtrePeriode = ref('30j')
const dateDebutPerso = ref('')
const dateFinPerso = ref('')
const filtreAgenceId = ref(peutVoirToutesAgences.value ? '' : String(agenceUtilisateur || ''))
const filtreTypeProductionId = ref('')

const periodesDisponibles = [
    { cle: '7j', label: '7 derniers jours' },
    { cle: '30j', label: '30 derniers jours' },
    { cle: 'mois', label: 'Ce mois' },
    { cle: 'annee', label: 'Cette année' },
    { cle: 'perso', label: 'Dates personnalisées' }
]

function isoDate(d) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const periode = computed(() => {
    const aujourdhui = new Date()
    const fin = isoDate(aujourdhui)
    switch (filtrePeriode.value) {
        case '7j': {
            return { debut: isoDate(new Date(aujourdhui.getTime() - 6 * 86400000)), fin }
        }
        case 'mois': {
            return { debut: isoDate(new Date(aujourdhui.getFullYear(), aujourdhui.getMonth(), 1)), fin }
        }
        case 'annee': {
            return { debut: `${aujourdhui.getFullYear()}-01-01`, fin }
        }
        case 'perso': {
            return { debut: dateDebutPerso.value, fin: dateFinPerso.value || fin }
        }
        default: {
            return { debut: isoDate(new Date(aujourdhui.getTime() - 29 * 86400000)), fin }
        }
    }
})

const periodeComplete = computed(() => filtrePeriode.value !== 'perso' || (dateDebutPerso.value && dateFinPerso.value))

// ──────────────────────────────────────────────
// ÉTAT DU CHARGEMENT
// ──────────────────────────────────────────────
const loading = ref(true)
const error = ref(null)
const data = ref(null)
const donneesPerimees = ref(false)

// ──────────────────────────────────────────────
// CHARGEMENT DES DONNÉES (PostgreSQL, côté serveur)
// ──────────────────────────────────────────────
async function charger(silencieux = false) {
    if (!silencieux) loading.value = true
    error.value = null
    try {
        data.value = await DashboardService.getVueDossiers({
            dateDebut: periode.value.debut || undefined,
            dateFin: periode.value.fin || undefined,
            agenceId: peutVoirToutesAgences.value ? (filtreAgenceId.value || undefined) : undefined,
            typeProductionId: filtreTypeProductionId.value || undefined
        })
        donneesPerimees.value = false
    } catch (e) {
        console.error('Erreur tableau de bord :', e)
        error.value = 'Impossible de charger les données du tableau de bord. Vérifiez la connexion au serveur et réessayez.'
        donneesPerimees.value = Boolean(data.value)
    } finally {
        if (!silencieux) loading.value = false
    }
}

// Actualisation automatique périodique si la page reste ouverte
let minuteur = null
onMounted(() => {
    charger()
    minuteur = setInterval(() => {
        if (document.visibilityState === 'visible' && periodeComplete.value) charger(true)
    }, 60000)
})
onBeforeUnmount(() => {
    if (minuteur) clearInterval(minuteur)
})

// Rechargement simultané dès qu'un filtre global change
watch([filtrePeriode, filtreAgenceId, filtreTypeProductionId], () => {
    if (periodeComplete.value) charger()
})

const agences = ref([])
const typeProductions = ref([])

// ──────────────────────────────────────────────
// LIBELLÉS DE PÉRIMÈTRE
// ──────────────────────────────────────────────
const libelleAgence = computed(() => {
    if (!peutVoirToutesAgences.value) return 'mon agence'
    if (!filtreAgenceId.value) return 'toutes les agences'
    const agence = agences.value.find(a => String(a.id) === String(filtreAgenceId.value))
    return agence ? `agence ${agence.nom}` : 'toutes les agences'
})

const libelleTypeProduction = computed(() => {
    if (!filtreTypeProductionId.value) return 'tous les types'
    const type = typeProductions.value.find(t => String(t.id) === String(filtreTypeProductionId.value))
    return type ? type.nom : 'tous les types'
})

const libellePeriodeTexte = computed(() => {
    const { debut, fin } = periode.value
    if (!debut || !fin) return 'aucune période définie'
    const nf = (iso) => {
        try { return new Date(`${iso}T00:00:00`).toLocaleDateString('fr-FR') } catch { return iso }
    }
    return `du ${nf(debut)} au ${nf(fin)}`
})

// ──────────────────────────────────────────────
// KPI CLIQUABLES
// ──────────────────────────────────────────────
function ouvrirListeDossiers(requete = {}) {
    router.push({ path: '/dossiers', query: requete })
}

function construireRequete(extra = {}, inclurePeriode = false) {
    const requete = {}
    if (peutVoirToutesAgences.value && filtreAgenceId.value) requete.agenceId = filtreAgenceId.value
    if (filtreTypeProductionId.value) requete.typeProductionId = filtreTypeProductionId.value
    if (inclurePeriode && periode.value.debut) requete.dateMin = periode.value.debut
    if (inclurePeriode && periode.value.fin) requete.dateMax = periode.value.fin
    Object.assign(requete, extra)
    return requete
}

const ICONES_KPI = {
    dossier: 'M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
    coche: 'M9 12l2.5 2.5L16 9',
    document: 'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5',
    horloge: 'M12 8v4l3 2',
    echanges: 'M5 8h12M14 5l3 3-3 3M19 16H7M10 13l-3 3 3 3',
    calendrier: 'M16 3v4M8 3v4M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z'
}

const kpis = computed(() => {
    const k = data.value?.kpi || {}
    return [
        {
            cle: 'en-cours',
            libelle: 'Dossiers en cours',
            valeur: k.enCours ?? 0,
            icone: ICONES_KPI.dossier,
            couleur: '#3b6a9c',
            regle: 'Situation actuelle — toutes les échéances confondues',
            requete: construireRequete({ estActif: 'true' })
        },
        {
            cle: 'clotures',
            libelle: 'Dossiers clôturés',
            valeur: k.clotures ?? 0,
            icone: ICONES_KPI.coche,
            cercle: true,
            couleur: '#4d9e8a',
            regle: 'Situation actuelle — toutes les périodes',
            requete: construireRequete({ statutCloture: 'true' })
        },
        {
            cle: 'documents',
            libelle: 'Documents incomplets',
            valeur: k.documentsIncomplets ?? 0,
            icone: ICONES_KPI.document,
            couleur: '#c98b3d',
            regle: 'Documents obligatoires manquants — situation actuelle',
            requete: construireRequete({ documentsIncomplets: 'true' })
        },
        {
            cle: 'echeances',
            libelle: 'Échéances dépassées',
            valeur: k.echeancesDepassees ?? 0,
            icone: ICONES_KPI.horloge,
            cercle: true,
            couleur: '#a85454',
            regle: 'Dossiers en cours dont l\'échéance est passée',
            requete: construireRequete({ estActif: 'true', echeanceDepassee: 'true' })
        },
        {
            cle: 'demandes',
            libelle: 'Demandes interagences en attente',
            valeur: k.demandesEnAttente ?? 0,
            icone: ICONES_KPI.echanges,
            couleur: '#8a5aa8',
            regle: 'Demandes reçues non traitées — situation actuelle',
            requete: construireRequete({ demandeEnAttente: 'true' })
        },
        {
            cle: 'creees',
            libelle: 'Dossiers créés sur la période',
            valeur: k.dossiersPeriode ?? 0,
            icone: ICONES_KPI.calendrier,
            couleur: '#3d8fc9',
            regle: `Activité sur la période ${libellePeriodeTexte.value}`,
            requete: construireRequete({}, true)
        }
    ]
})

// ──────────────────────────────────────────────
// GRAPHIQUES CLIQUABLES
// ──────────────────────────────────────────────
const parPhase = computed(() => data.value?.parPhase || [])
const evolution = computed(() => data.value?.evolution?.points || [])
const granularite = computed(() => data.value?.evolution?.granularite || 'JOUR')
const parAgence = computed(() => data.value?.parAgence || [])

const libelleGranularite = computed(() => ({
    JOUR: 'par jour', SEMAINE: 'par semaine', MOIS: 'par mois'
}[granularite.value] || 'selon la période'))

function ouvrirPhase(index) {
    const phase = parPhase.value[index]
    if (!phase) return
    const requete = construireRequete({ estActif: 'true' })
    if (phase.id !== null && phase.id !== undefined) requete.phaseId = String(phase.id)
    ouvrirListeDossiers(requete)
}

function ouvrirPointEvolution(index) {
    const point = evolution.value[index]
    if (!point) return
    ouvrirListeDossiers(construireRequete({ dateMin: point.debut, dateMax: point.fin }))
}

function ouvrirAgence(index) {
    const agence = parAgence.value[index]
    if (!agence) return
    ouvrirListeDossiers(construireRequete({ agenceId: String(agence.id), estActif: 'true' }))
}

// ──────────────────────────────────────────────
// DOSSIERS PRIORITAIRES
// ──────────────────────────────────────────────
const prioritaires = computed(() => data.value?.prioritaires || [])

function badgesAnomalie(dossier) {
    const badges = []
    if (dossier.echeance_depassee) badges.push({ libelle: 'Échéance dépassée', classe: 'bg-danger' })
    if (dossier.docs_manquants) badges.push({ libelle: 'Documents manquants', classe: 'bg-warning text-dark' })
    if (dossier.demande_attente) badges.push({ libelle: 'Inter-agences', classe: 'bg-info text-dark' })
    if (dossier.echeance_proche) badges.push({ libelle: 'Échéance proche', classe: 'bg-warning text-dark' })
    return badges
}

function niveauPriorite(dossier) {
    if (dossier.echeance_depassee) return { libelle: 'Urgent', classe: 'bg-danger' }
    if (badgesAnomalie(dossier).length) return { libelle: 'À traiter', classe: 'bg-warning text-dark' }
    return { libelle: 'Suivi', classe: 'bg-secondary' }
}

function dateCourte(valeur) {
    if (!valeur) return '—'
    try {
        return new Date(valeur).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
    } catch { return valeur }
}

// ──────────────────────────────────────────────
// ACTIVITÉS RÉCENTES
// ──────────────────────────────────────────────
const activites = computed(() => data.value?.activites || [])

const stylesActivite = {
    MOUVEMENT: { couleur: '#3b6a9c', libelle: 'Mouvement' },
    DOSSIER: { couleur: '#4d9e8a', libelle: 'Dossier' },
    DEMANDE: { couleur: '#c98b3d', libelle: 'Inter-agences' }
}

function dateHeureCourte(valeur) {
    if (!valeur) return ''
    try {
        return new Date(valeur).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    } catch { return valeur }
}

// ──────────────────────────────────────────────
// RÉFÉRENTIELS DES FILTRES
// ──────────────────────────────────────────────
onMounted(async () => {
    const resultats = await Promise.allSettled([
        AgenceService.getAllAgences(),
        OrdreTransitService.getAllTypeProductions()
    ])
    if (resultats[0].status === 'fulfilled') agences.value = resultats[0].value
    if (resultats[1].status === 'fulfilled') typeProductions.value = resultats[1].value
})
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="db-main flex-grow-1 p-4">
            <div class="container-fluid">
                <!-- ───── EN-TÊTE ───── -->
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Tableau de bord</h1>
                        <p class="text-secondary mb-0 small">Vue d'ensemble des opérations Auximad</p>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span v-if="loading && !error" class="small text-secondary d-none d-md-inline">Chargement…</span>
                        <button type="button" class="btn btn-primary btn-sm" @click="charger()" :disabled="loading">
                            <span v-if="loading" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
                            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="me-1"
                                 aria-hidden="true">
                                <path d="M21 12a9 9 0 1 1-2.64-6.36M21 3v6h-6" />
                            </svg>
                            Actualiser
                        </button>
                    </div>
                </div>

                <!-- ───── ERREUR ───── -->
                <div v-if="error" class="alert alert-danger d-flex align-items-center py-2" role="alert">
                    <span>{{ error }}</span>
                    <button v-if="donneesPerimees" type="button" class="btn btn-sm btn-outline-danger ms-auto"
                            @click="charger()">Réessayer</button>
                </div>

                <!-- ───── FILTRES GLOBAUX ───── -->
                <div class="card border shadow-sm mb-3">
                    <div class="card-body p-3">
                        <div class="d-flex flex-wrap align-items-end gap-2">
                            <div class="db-filtre-champ">
                                <label for="db-periode" class="form-label small fw-semibold text-secondary mb-1">Période</label>
                                <select id="db-periode" v-model="filtrePeriode" class="form-select form-select-sm">
                                    <option v-for="p in periodesDisponibles" :key="p.cle" :value="p.cle">{{ p.label }}</option>
                                </select>
                            </div>
                            <template v-if="filtrePeriode === 'perso'">
                                <div class="db-filtre-champ">
                                    <label for="db-debut" class="form-label small fw-semibold text-secondary mb-1">Du</label>
                                    <input id="db-debut" v-model="dateDebutPerso" type="date"
                                           class="form-control form-control-sm">
                                </div>
                                <div class="db-filtre-champ">
                                    <label for="db-fin" class="form-label small fw-semibold text-secondary mb-1">Au</label>
                                    <input id="db-fin" v-model="dateFinPerso" type="date" class="form-control form-control-sm">
                                </div>
                            </template>
                            <div class="db-filtre-champ">
                                <label for="db-agence" class="form-label small fw-semibold text-secondary mb-1">Agence</label>
                                <select id="db-agence" v-model="filtreAgenceId" class="form-select form-select-sm"
                                        :disabled="!peutVoirToutesAgences">
                                    <template v-if="peutVoirToutesAgences">
                                        <option value="">Toutes les agences</option>
                                        <option v-for="a in agences" :key="a.id" :value="String(a.id)">{{ a.nom }}</option>
                                    </template>
                                    <template v-else>
                                        <option :value="String(agenceUtilisateur)">
                                            {{ (agences.find(a => String(a.id) === String(agenceUtilisateur)) || {}).nom || 'Mon agence' }}
                                        </option>
                                    </template>
                                </select>
                            </div>
                            <div class="db-filtre-champ">
                                <label for="db-type" class="form-label small fw-semibold text-secondary mb-1">Type de production</label>
                                <select id="db-type" v-model="filtreTypeProductionId" class="form-select form-select-sm">
                                    <option value="">Tous les types</option>
                                    <option v-for="t in typeProductions" :key="t.id" :value="String(t.id)">{{ t.nom }}</option>
                                </select>
                            </div>
                        </div>
                        <p v-if="periodeComplete" class="db-perimetre small text-secondary mb-0 mt-2">
                            Périmètre : {{ libelleAgence }} · {{ libelleTypeProduction }} · période {{ libellePeriodeTexte }}.
                            Les indicateurs décrits comme « situation actuelle » portent sur l'état d'aujourd'hui ;
                            « Dossiers créés » et le graphique d'évolution portent sur la période sélectionnée.
                        </p>
                    </div>
                </div>

                <!-- ───── SQUELETTES ───── -->
                <template v-if="loading && !data">
                    <div class="db-kpis">
                        <div v-for="i in 6" :key="i" class="card border shadow-sm db-squelette-kpi"></div>
                    </div>
                    <div class="row g-3 mb-3">
                        <div class="col-12 col-lg-4"><div class="card border shadow-sm db-squelette"></div></div>
                        <div class="col-12 col-lg-8"><div class="card border shadow-sm db-squelette"></div></div>
                    </div>
                </template>

                <!-- ───── CONTENU ───── -->
                <template v-else-if="data">
                    <!-- ═══ KPI cliquables ═══ -->
                    <div class="db-kpis mb-3">
                        <button v-for="kpi in kpis" :key="kpi.cle" type="button"
                                class="card border shadow-sm db-kpi text-start"
                                :title="`${kpi.regle} — voir la liste`"
                                @click="ouvrirListeDossiers(kpi.requete)">
                            <div class="card-body p-3">
                                <div class="d-flex align-items-start gap-2">
                                    <span class="db-kpi-icone" :style="{ '--couleur-kpi': kpi.couleur }" aria-hidden="true">
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                             stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                            <path :d="kpi.icone" />
                                            <circle v-if="kpi.cercle" cx="12" cy="12" r="9" fill="none" stroke="currentColor" />
                                        </svg>
                                    </span>
                                    <div class="min-w-0 flex-grow-1">
                                        <div class="h4 fw-bold text-dark mb-0 lh-1">{{ Number(kpi.valeur).toLocaleString('fr-FR') }}</div>
                                        <div class="small text-secondary mt-1">{{ kpi.libelle }}</div>
                                    </div>
                                </div>
                            </div>
                        </button>
                    </div>

                    <!-- ═══ Graphique de phases + Évolution des créations ═══ -->
                    <div class="row g-3 mb-3">
                        <div class="col-12 col-lg-5">
                            <div class="card border shadow-sm h-100">
                                <div class="card-header bg-white border-bottom py-3">
                                    <h2 class="h6 fw-semibold mb-1">Répartition par phase</h2>
                                    <p class="small text-secondary mb-0">Dossiers en cours, par phase — cliquez sur une portion pour ouvrir la liste.</p>
                                </div>
                                <div class="card-body p-3">
                                    <div v-if="parPhase.length === 0" class="text-secondary small text-center py-4">
                                        Aucun dossier en cours sur ce périmètre
                                    </div>
                                    <div v-else class="db-graphique db-graphique--anneau">
                                        <DashboardChart type="doughnut"
                                                        aria-label="Répartition des dossiers en cours par phase"
                                                        :labels="parPhase.map(ph => ph.label)"
                                                        :valeurs="parPhase.map(ph => Number(ph.valeur))"
                                                        @selection="ouvrirPhase" />
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="col-12 col-lg-7">
                            <div class="card border shadow-sm h-100">
                                <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between flex-wrap gap-2">
                                    <div>
                                        <h2 class="h6 fw-semibold mb-1">Évolution des créations</h2>
                                        <p class="small text-secondary mb-0">
                                            Dossiers créés {{ libelleGranularite }} — cliquez sur un point pour afficher la liste.
                                        </p>
                                    </div>
                                </div>
                                <div class="card-body p-3">
                                    <div v-if="evolution.length === 0" class="text-secondary small text-center py-4">
                                        Aucune donnée sur cette période
                                    </div>
                                    <div v-else class="db-graphique db-graphique--courbe">
                                        <DashboardChart type="line"
                                                        aria-label="Évolution des créations de dossiers"
                                                        :labels="evolution.map(pt => pt.label)"
                                                        :valeurs="evolution.map(pt => Number(pt.valeur))"
                                                        @selection="ouvrirPointEvolution" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- ═══ Répartition par agence (utilisateurs multi-agences uniquement) ═══ -->
                    <div v-if="peutVoirToutesAgences" class="card border shadow-sm mb-3">
                        <div class="card-header bg-white border-bottom py-3">
                            <h2 class="h6 fw-semibold mb-1">Répartition des dossiers par agence</h2>
                            <p class="small text-secondary mb-0">Dossiers en cours par agence — cliquez sur une barre pour ouvrir la liste.</p>
                        </div>
                        <div class="card-body p-3">
                            <div v-if="parAgence.length === 0" class="text-secondary small text-center py-4">
                                Aucun dossier en cours
                            </div>
                            <div v-else class="db-graphique db-graphique--barres">
                                <DashboardChart type="barH"
                                                aria-label="Répartition des dossiers par agence"
                                                :labels="parAgence.map(a => a.label)"
                                                :valeurs="parAgence.map(a => Number(a.valeur))"
                                                @selection="ouvrirAgence" />
                            </div>
                        </div>
                    </div>

                    <!-- ═══ Dossiers prioritaires + Activités récentes ═══ -->
                    <div class="row g-3">
                        <div class="col-12 col-lg-7">
                            <div class="card border shadow-sm h-100">
                                <div class="card-header bg-white border-bottom py-3">
                                    <h2 class="h6 fw-semibold mb-1">Dossiers prioritaires</h2>
                                    <p class="small text-secondary mb-0">Anomalies nécessitant une intervention (urgences d'abord).</p>
                                </div>
                                <div class="card-body p-0">
                                    <div v-if="prioritaires.length === 0" class="text-secondary small text-center py-4">
                                        Aucun dossier prioritaire sur ce périmètre
                                    </div>
                                    <div v-else class="table-responsive">
                                        <table class="table table-hover align-middle mb-0 db-table">
                                            <thead class="table-light">
                                                <tr>
                                                    <th scope="col">N° Dossier</th>
                                                    <th scope="col">Client</th>
                                                    <th scope="col">Anomalie</th>
                                                    <th scope="col">Échéance</th>
                                                    <th scope="col">Priorité</th>
                                                    <th scope="col" class="text-end">Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <tr v-for="dossier in prioritaires" :key="dossier.id">
                                                    <td>
                                                        <router-link :to="`/dossiers/${dossier.id}`"
                                                                     class="fw-semibold text-primary text-decoration-none">
                                                            {{ dossier.numero_dossier }}
                                                        </router-link>
                                                    </td>
                                                    <td class="db-cell-tronquee" :title="dossier.client">
                                                        {{ dossier.client || '—' }}
                                                    </td>
                                                    <td>
                                                        <span v-if="badgesAnomalie(dossier).length === 0" class="text-secondary">—</span>
                                                        <span v-for="(badge, i) in badgesAnomalie(dossier)" :key="i"
                                                              class="badge me-1 mb-1 d-inline-block" :class="badge.classe">
                                                            {{ badge.libelle }}
                                                        </span>
                                                    </td>
                                                    <td>{{ dateCourte(dossier.echeance) }}</td>
                                                    <td>
                                                        <span class="badge" :class="niveauPriorite(dossier).classe">
                                                            {{ niveauPriorite(dossier).libelle }}
                                                        </span>
                                                    </td>
                                                    <td class="text-end">
                                                        <router-link :to="`/dossiers/${dossier.id}`"
                                                                     class="btn btn-sm btn-outline-primary">Voir</router-link>
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-12 col-lg-5">
                            <div class="card border shadow-sm h-100">
                                <div class="card-header bg-white border-bottom py-3">
                                    <h2 class="h6 fw-semibold mb-1">Activités récentes</h2>
                                    <p class="small text-secondary mb-0">Dernières opérations significatives.</p>
                                </div>
                                <div class="card-body p-3">
                                    <div v-if="activites.length === 0" class="text-secondary small text-center py-4">
                                        Aucune activité récente
                                    </div>
                                    <ul v-else class="db-activite">
                                        <li v-for="(evt, i) in activites" :key="i" class="db-activite-row">
                                            <span class="db-activite-point"
                                                  :style="{ '--couleur-ev': (stylesActivite[evt.type] || {}).couleur }"
                                                  aria-hidden="true">
                                                <svg v-if="evt.type === 'MOUVEMENT'" width="13" height="13" viewBox="0 0 24 24"
                                                     fill="none" stroke="currentColor" stroke-width="2"
                                                     stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M5 8h12M14 5l3 3-3 3M19 16H7M10 13l-3 3 3 3" />
                                                </svg>
                                                <svg v-else-if="evt.type === 'DOSSIER'" width="13" height="13" viewBox="0 0 24 24"
                                                     fill="none" stroke="currentColor" stroke-width="2"
                                                     stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M12 5v14M5 12h14" />
                                                </svg>
                                                <svg v-else width="13" height="13" viewBox="0 0 24 24"
                                                     fill="none" stroke="currentColor" stroke-width="2"
                                                     stroke-linecap="round" stroke-linejoin="round">
                                                    <path d="M17 2l4 4-4 4M21 6H9M7 22l-4-4 4-4M3 18h12" />
                                                </svg>
                                            </span>
                                            <div class="min-w-0 flex-grow-1">
                                                <router-link :to="`/dossiers/${evt.id_dossier}`"
                                                             class="db-activite-texte fw-semibold text-decoration-none text-dark">
                                                    {{ evt.libelle || evt.type }}
                                                    <span class="text-primary">· {{ evt.numero_dossier }}</span>
                                                </router-link>
                                                <div class="small text-secondary overflow-wrap-anywhere">
                                                    {{ (stylesActivite[evt.type] || {}).libelle || evt.type }}
                                                    <template v-if="evt.utilisateur"> · {{ evt.utilisateur }}</template>
                                                    · {{ dateHeureCourte(evt.date_evt) }}
                                                </div>
                                            </div>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </main>
    </div>
</template>

<style scoped>
.db-main { margin-left: 280px; min-width: 0; width: calc(100% - 280px); }

.db-filtre-champ { min-width: 170px; }

.db-perimetre { color: #5e6f88; }

/* ── KPI ── */
.db-kpis { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; }
.db-kpi {
    display: block;
    width: 100%;
    background: #fff;
    transition: box-shadow 0.15s ease, transform 0.15s ease, border-color 0.15s ease;
    cursor: pointer;
}
.db-kpi:hover {
    border-color: var(--couleur-kpi, #3b6a9c);
    box-shadow: 0 0.35rem 0.75rem rgba(59, 106, 156, 0.15) !important;
    transform: translateY(-2px);
}
.db-kpi:focus-visible { outline: 2px solid var(--couleur-kpi, #3b6a9c); outline-offset: 2px; }
.db-kpi-icone {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    border-radius: 8px;
    flex-shrink: 0;
    color: var(--couleur-kpi, #3b6a9c);
    background: color-mix(in srgb, var(--couleur-kpi, #3b6a9c) 12%, #ffffff);
}
.min-w-0 { min-width: 0; }
.overflow-wrap-anywhere { overflow-wrap: anywhere; }

/* ── Graphiques ── */
.db-graphique { position: relative; }
.db-graphique--anneau { height: 240px; }
.db-graphique--courbe { height: 240px; }
.db-graphique--barres { height: 300px; }

/* ── Prioritaires ── */
.db-table th { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.03em; color: #5e6f88; }
.db-cell-tronquee { max-width: 14rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── Activités ── */
.db-activite { list-style: none; margin: 0; padding: 0; }
.db-activite-row {
    display: flex;
    align-items: flex-start;
    gap: 0.65rem;
    padding: 0.65rem 0;
    border-bottom: 1px solid #e2e6ec;
}
.db-activite-row:last-child { border-bottom: 0; padding-bottom: 0; }
.db-activite-point {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    flex-shrink: 0;
    margin-top: 2px;
    color: var(--couleur-ev, #3b6a9c);
    background: color-mix(in srgb, var(--couleur-ev, #3b6a9c) 14%, #ffffff);
}
.db-activite-texte { font-size: 0.85rem; display: block; }

/* ── Squelettes ── */
.db-squelette-kpi { min-height: 84px; background: var(--bs-secondary-bg); }
.db-squelette { min-height: 300px; background: var(--bs-secondary-bg); }

/* ── Responsive ── */
@media (max-width: 1100px) {
    .db-kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
    .db-kpis { grid-template-columns: minmax(0, 1fr); }
    .db-filtre-champ { width: 100%; }
}
</style>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- Back Link -->
                <div class="mb-4">
                    <router-link to="/ordres-transit"
                        class="text-decoration-none text-muted d-inline-flex align-items-center gap-2">
                        &larr; Retour à la liste des ordres de transit
                    </router-link>
                </div>

                <div v-if="loading" class="text-center py-5">
                    <div class="spinner-border text-primary" role="status"></div>
                </div>

                <div v-else-if="erreur" class="alert alert-danger" role="alert">
                    {{ erreur }}
                </div>

                <div v-else>
                    <!-- Header Info / Actions -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <div class="d-flex align-items-center gap-2 mb-2">
                                <span class="badge bg-primary text-uppercase">{{ otModel.typeProduction?.nom }}</span>
                                <span :class="obtenirBadgeClasse(otModel.statutOt?.nom)">
                                    {{ otModel.statutOt?.nom }}
                                </span>
                            </div>
                            <h1 class="h3 fw-bold text-primary mb-1">
                                Ordre de Transit OT-{{ otModel.numeroOrdreTransit }}
                            </h1>
                            <p class="text-secondary mb-0">
                                Créé le {{ formatarDate(otModel.dateCreation) }} par {{ otModel.utilisateur?.nom }}
                                (Agence: {{ otModel.agence?.nom }})
                            </p>
                        </div>
                        <div class="d-flex gap-2">
                            <button v-if="peutValider && otModel.statutOt?.id !== 3" type="button"
                                class="btn btn-success btn-sm d-flex align-items-center gap-1" @click="validerOT">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                    viewBox="0 0 16 16">
                                    <path
                                        d="M12.736 3.97a.733.733 0 0 1 1.047 0c.286.289.29.756.01 1.05L7.88 12.01a.733.733 0 0 1-1.065.02L3.217 8.384a.757.757 0 0 1 0-1.06.733.733 0 0 1 1.047 0l3.052 3.093 5.4-6.425z" />
                                </svg>
                                Valider l'OT
                            </button>
                            <button v-if="peutSupprimer" type="button" class="btn btn-outline-danger btn-sm"
                                @click="supprimerOT">Supprimer</button>
                        </div>
                    </div>

                    <!-- Fiche detaillee -->
                    <div class="row g-4">
                        <!-- Col Gauche: Details Administratifs -->
                        <div class="col-lg-8">
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <h5 class="fw-bold mb-4 border-bottom pb-2">Informations Générales</h5>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">CLIENT DONNEUR
                                            D'ORDRE</label>
                                        <div class="fw-semibold">{{ otModel.client?.nom }} {{ otModel.client?.prenom }}
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">COMPTE DE
                                            FACTURATION</label>
                                        <div class="fw-semibold">{{ otModel.destinationFacture ?
                                            `${otModel.destinationFacture.nom} ${otModel.destinationFacture.prenom ||
                                            ''}` : 'Même que le client' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">RÉGIME DOUANIER</label>
                                        <div><span class="badge bg-light text-dark border">{{ otModel.typeRegime?.code
                                                }}</span> - {{ otModel.typeRegime?.nom }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">N° DE COMMANDE</label>
                                        <div class="fw-semibold">{{ otModel.numeroCommande || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">ASSURANCE</label>
                                        <div class="fw-semibold">{{ otModel.estAssure ? 'Assuré d\'office' : 'Aucune' }}
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">DÉPART /
                                            EXPÉDITEUR</label>
                                        <div class="fw-semibold">{{ otModel.expediteur?.nom }} {{
                                            otModel.expediteur?.prenom }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">DESTINATAIRE
                                            (CONSIGNEE)</label>
                                        <div class="fw-semibold">{{ otModel.destinataire?.nom }} {{
                                            otModel.destinataire?.prenom }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">BUREAU DE
                                            DÉDOUANEMENT</label>
                                        <div class="fw-semibold">{{ otModel.bureauDedouanement?.codeBureau }} - {{
                                            otModel.bureauDedouanement?.nom }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">DESTINATION
                                            FINALE</label>
                                        <div class="fw-semibold">{{ otModel.destinationFinale || '—' }}</div>
                                    </div>
                                </div>
                            </div>

                            <!-- Contrat de transport -->
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
                                    <h5 class="fw-bold mb-0">Contrat de transport (B/L ou LTA)</h5>
                                </div>
                                <div v-if="!contrat" class="text-xs text-muted">
                                    Aucun contrat de transport n'est associé à cet ordre de transit.
                                </div>
                                <div v-else class="row g-3">
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">NUMÉRO B/L OU
                                            LTA</label>
                                        <div class="fw-bold text-primary">{{ contrat.numero || 'Non spécifié' }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">COMPAGNIE /
                                            PORTEUR</label>
                                        <div class="fw-semibold">{{ contrat.compagnie?.nom || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">NAVIRE</label>
                                        <div class="fw-semibold">{{ contrat.navire?.nom || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">N° DU VOL</label>
                                        <div class="fw-semibold">{{ contrat.n_vol || contrat.numeroVol || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label text-xs fw-semibold text-muted">LIEU D'ÉMISSION</label>
                                        <div class="fw-semibold">{{ contrat.lieuEmission || '—' }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">DATE PRÉVUE
                                            DÉPART/ARRIVÉE</label>
                                        <div class="fw-semibold">{{ formatarDate(contrat.datePrevue) }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label text-xs fw-semibold text-muted">DATE ARRIVÉE
                                            CRITIQUE</label>
                                        <div class="fw-semibold">{{ formatarDate(contrat.dateArrivee) }}</div>
                                    </div>
                                    <div class="col-md-12">
                                        <label class="form-label text-xs fw-semibold text-muted">NOTIFY PARTY</label>
                                        <div class="fw-semibold bg-light p-2 rounded text-xs">{{ contrat.notify || '—'
                                            }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Col Droite: Instructions & Marchandises Summary -->
                        <div class="col-lg-4">
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <h5 class="fw-bold mb-3 border-bottom pb-2">Instructions Spéciales</h5>
                                <div class="instructions-list mb-3" style="max-height: 200px; overflow-y: auto;">
                                    <div v-if="instructions.length === 0" class="text-xs text-muted">
                                        Aucune instruction saisie
                                    </div>
                                    <div v-for="inst in instructions" :key="inst.id" class="border-bottom py-2 text-xs">
                                        <p class="mb-1 text-dark">{{ inst.instruction }}</p>
                                        <span class="text-muted text-xxs">Ajouté le {{ formatarDate(inst.dateCreation)
                                            }}</span>
                                    </div>
                                </div>
                                <div class="input-group input-group-sm">
                                    <input type="text" v-model="nouvelleInstruction" class="form-control"
                                        placeholder="Ajouter une instruction...">
                                    <button class="btn btn-primary" type="button"
                                        @click="ajouterInstruction">Ajouter</button>
                                </div>
                            </div>

                            <!-- Marchandises (Placeholder/Lien vers CRUD Phase 5) -->
                            <div class="card border-0 shadow-sm p-4">
                                <h5 class="fw-bold mb-3 border-bottom pb-2">Marchandises associées</h5>
                                <p class="text-muted text-xs">Les marchandises liées à cet ordre de transit sont gérées
                                    dans le module Marchandises.</p>

                                <div class="d-grid gap-2">
                                    <router-link :to="`/marchandises?otId=${otModel.id}`"
                                        class="btn btn-outline-primary btn-sm btn-block text-xs">
                                        Consulter les marchandises &rarr;
                                    </router-link>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import OrdreTransitService from '../services/OrdreTransitService'
import PermissionService, { PORTEE_PROPRE_VALIDATION, PORTEE_PROPRE_SUPPRESSION, ACTION_OT } from '../utils/permission.service'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const erreur = ref(null)

const otModel = ref(null)
const contrat = ref(null)
const instructions = ref([])
const nouvelleInstruction = ref('')

const peutValider = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_VALIDATION, ACTION_OT)
})

const peutSupprimer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_SUPPRESSION, ACTION_OT)
})

onMounted(() => {
    const id = route.params.id
    chargerTout(id)
})

async function chargerTout(id) {
    loading.value = true
    try {
        const [otData, contratData, instList] = await Promise.all([
            OrdreTransitService.getOrdreTransitById(id),
            OrdreTransitService.getContratByOtId(id).catch(e => null), // peut être 204 no content
            OrdreTransitService.getInstructionsByOtId(id)
        ])
        otModel.value = otData
        contrat.value = contratData
        instructions.value = instList
        loading.value = false
    } catch (e) {
        erreur.value = "Impossible de récupérer l'ordre de transit spécifié."
        loading.value = false
    }
}

async function validerOT() {
    if (confirm("Êtes-vous sûr de vouloir valider cet ordre de transit ? Cette action est irréversible.")) {
        loading.value = true
        try {
            const up = await OrdreTransitService.validateOrdreTransit(otModel.value.id)
            otModel.value = up
            alert("Ordre de transit validé avec succès.")
        } catch (e) {
            alert("Erreur technique lors de la validation.")
        } finally {
            loading.value = false
        }
    }
}

async function supprimerOT() {
    if (confirm("Supprimer définitivement cet ordre de transit ? Les contrats et instructions liés seront éliminés.")) {
        loading.value = true
        try {
            await OrdreTransitService.deleteOrdreTransit(otModel.value.id)
            alert("Ordre de transit supprimé avec succès.")
            router.push('/ordres-transit')
        } catch (e) {
            alert("Impossible de supprimer cet ordre de transit.")
            loading.value = false
        }
    }
}

async function ajouterInstruction() {
    if (!nouvelleInstruction.value.trim()) return
    try {
        const inst = await OrdreTransitService.addInstruction(otModel.value.id, nouvelleInstruction.value)
        instructions.value.push(inst)
        nouvelleInstruction.value = ''
    } catch (e) {
        alert("Erreur lors de l'ajout de l'instruction.")
    }
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    })
}

function obtenirBadgeClasse(status) {
    if (!status) return 'badge bg-warning-subtle text-warning border border-warning-subtle'
    switch (status.toLowerCase()) {
        case 'validé':
        case 'valide':
            return 'badge bg-success-subtle text-success border border-success-subtle'
        case 'brouillon':
            return 'badge bg-secondary-subtle text-secondary border border-secondary-subtle'
        default:
            return 'badge bg-warning-subtle text-warning border border-warning-subtle'
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
</style>

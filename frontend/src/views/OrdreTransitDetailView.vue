<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
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
            OrdreTransitService.getContratByOtId(id).catch(e => null),
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
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-3">
                    <router-link to="/ordres-transit" class="text-decoration-none text-secondary">
                        &larr; Retour à la liste des ordres de transit
                    </router-link>
                </div>

                <PageSkeleton v-if="loading" variant="detail" :with-layout="false" />

                <div v-else-if="erreur" class="alert alert-danger" role="alert">
                    {{ erreur }}
                </div>

                <div v-else>
                    <div class="d-flex justify-content-between align-items-start mb-4">
                        <div>
                            <h1 class="h4 fw-bold text-dark mb-1">
                                Ordre de Transit: {{ otModel.numeroOrdreTransit }}
                            </h1>
                            <p class="text-secondary mb-0 small">
                                Créé le {{ formatarDate(otModel.dateCreation) }} par {{ otModel.utilisateur?.nom }}
                                (Agence: {{ otModel.agence?.nom }})
                            </p>
                        </div>
                        <div class="d-flex gap-2">
                            <button v-if="peutValider && otModel.statutOt?.id !== 3" type="button"
                                class="btn btn-success btn-sm" @click="validerOT">
                                Valider l'OT
                            </button>
                            <button v-if="peutSupprimer" type="button" class="btn btn-outline-danger btn-sm" @click="supprimerOT">Supprimer</button>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-lg-8">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Informations Générales</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3 small">
                                        <div class="col-md-6">
                                            <div class="text-secondary">CLIENT DONNEUR D'ORDRE</div>
                                            <div class="fw-semibold">{{ otModel.client?.nom }} {{ otModel.client?.prenom }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">COMPTE DE FACTURATION</div>
                                            <div class="fw-semibold">{{ otModel.destinationFacture ? `${otModel.destinationFacture.nom} ${otModel.destinationFacture.prenom || ''}` : 'Même que le client' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">RÉGIME DOUANIER</div>
                                            <div class="fw-semibold">{{ otModel.typeRegime?.code }} - {{ otModel.typeRegime?.nom }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">N° DE COMMANDE</div>
                                            <div class="fw-semibold">{{ otModel.numeroCommande || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">ASSURANCE</div>
                                            <div class="fw-semibold">{{ otModel.estAssure ? 'Assuré d\'office' : 'Aucune' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DÉPART / EXPÉDITEUR</div>
                                            <div class="fw-semibold">{{ otModel.expediteur?.nom }} {{ otModel.expediteur?.prenom }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DESTINATAIRE (CONSIGNEE)</div>
                                            <div class="fw-semibold">{{ otModel.destinataire?.nom }} {{ otModel.destinataire?.prenom }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">BUREAU DE DÉDOUANEMENT</div>
                                            <div class="fw-semibold">{{ otModel.bureauDedouanement?.codeBureau }} - {{ otModel.bureauDedouanement?.nom }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DESTINATION FINALE</div>
                                            <div class="fw-semibold">{{ otModel.destinationFinale || '—' }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Contrat de transport (B/L ou LTA)</h5>
                                </div>
                                <div class="card-body">
                                    <div v-if="!contrat" class="text-secondary small">
                                        Aucun contrat de transport n'est associé à cet ordre de transit.
                                    </div>
                                    <div v-else class="row g-3 small">
                                        <div class="col-md-6">
                                            <div class="text-secondary">NUMÉRO B/L OU LTA</div>
                                            <div class="fw-bold text-dark">{{ contrat.numero || 'Non spécifié' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">COMPAGNIE / PORTEUR</div>
                                            <div class="fw-semibold">{{ contrat.compagnie?.nom || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">NAVIRE</div>
                                            <div class="fw-semibold">{{ contrat.navire?.nom || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">N° DU VOL</div>
                                            <div class="fw-semibold">{{ contrat.n_vol || contrat.numeroVol || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">LIEU D'ÉMISSION</div>
                                            <div class="fw-semibold">{{ contrat.lieuEmission || '—' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DATE PRÉVUE DÉPART/ARRIVÉE</div>
                                            <div class="fw-semibold">{{ formatarDate(contrat.datePrevue) }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DATE ARRIVÉE CRITIQUE</div>
                                            <div class="fw-semibold">{{ formatarDate(contrat.dateArrivee) }}</div>
                                        </div>
                                        <div class="col-12">
                                            <div class="text-secondary">NOTIFY PARTY</div>
                                            <div class="fw-semibold">{{ contrat.notify || '—' }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-4">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Instructions Spéciales</h5>
                                </div>
                                <div class="card-body">
                                    <div class="mb-3" style="max-height: 200px; overflow-y: auto;">
                                        <div v-if="instructions.length === 0" class="text-secondary small">
                                            Aucune instruction saisie
                                        </div>
                                        <div v-for="inst in instructions" :key="inst.id" class="border-bottom py-2 small">
                                            <p class="mb-1">{{ inst.instruction }}</p>
                                            <span class="text-secondary">Ajouté le {{ formatarDate(inst.dateCreation) }}</span>
                                        </div>
                                    </div>
                                    <div class="input-group input-group-sm">
                                        <input type="text" v-model="nouvelleInstruction" class="form-control" placeholder="Ajouter une instruction...">
                                        <button class="btn btn-primary" type="button" @click="ajouterInstruction">Ajouter</button>
                                    </div>
                                </div>
                            </div>

                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Marchandises associées</h5>
                                </div>
                                <div class="card-body">
                                    <p class="text-secondary small mb-2">Les marchandises liées à cet ordre de transit sont gérées dans le module Marchandises.</p>
                                    <router-link :to="`/marchandises?otId=${otModel.id}`" class="btn btn-outline-primary btn-sm w-100">
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

<style scoped>
</style>
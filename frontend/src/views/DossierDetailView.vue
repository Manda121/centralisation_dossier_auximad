<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-4">
                    <router-link to="/dossiers" class="text-decoration-none text-muted">&larr; Retour aux
                        dossiers</router-link>
                </div>

                <div v-if="loading" class="text-center py-5">
                    <div class="spinner-border text-primary"></div>
                </div>
                <div v-else-if="erreur" class="alert alert-danger">{{ erreur }}</div>
                <div v-else>
                    <!-- Header -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <div class="d-flex align-items-center gap-2 mb-1">
                                <span class="badge bg-primary text-uppercase">{{ dossier.typeProduction?.nom }}</span>
                                <span :class="obtenirBadgeStatut(dossier.statutDossier?.nom)">{{
                                    dossier.statutDossier?.nom || '—' }}</span>
                                <span v-if="dossier.estGroupage"
                                    class="badge bg-info-subtle text-info border border-info-subtle">Groupage</span>
                            </div>
                            <h1 class="h3 fw-bold text-primary mb-0">{{ dossier.numeroDossier }}</h1>
                            <p class="text-secondary mb-0 text-xs">Créé le {{ formatarDate(dossier.dateCreation) }}</p>
                        </div>
                        <div class="d-flex gap-2">
                            <router-link v-if="dossier.ordreTransit" :to="`/ordres-transit/${dossier.ordreTransit.id}`"
                                class="btn btn-outline-primary btn-sm">
                                Voir l'OT associé OT-{{ dossier.ordreTransit.numeroOrdreTransit }}
                            </router-link>
                            <button v-if="peutSupprimer" class="btn btn-outline-danger btn-sm"
                                @click="supprimerDossier">Supprimer</button>
                        </div>
                    </div>

                    <div class="row g-4">
                        <!-- Col principale -->
                        <div class="col-lg-8">
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <h5 class="fw-bold mb-4 border-bottom pb-2">Informations du Dossier</h5>
                                <div class="row g-3 text-xs">
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">CLIENT</label>
                                        <div class="fw-semibold">{{ dossier.client?.nom }} {{ dossier.client?.prenom }}
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">POUR COMPTE DE</label>
                                        <div class="fw-semibold">{{ dossier.pourCompte || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">PHASE</label>
                                        <div class="fw-semibold">{{ dossier.phaseDossier?.nom || '—' }}</div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">INCOTERM</label>
                                        <div><span class="badge bg-light text-dark border">{{
                                                dossier.inconterme?.abreviation || '—' }}</span></div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">FRET</label>
                                        <div class="fw-semibold">{{ dossier.estPrepaye ? 'Prépayé' : 'Collect' }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">PAYS ORIGINE → DESTINATION</label>
                                        <div class="fw-semibold">{{ dossier.paysOrigine?.nom || '—' }} → {{
                                            dossier.paysDestination?.nom || '—' }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">LIEU PAIEMENT FRET</label>
                                        <div class="fw-semibold">{{ dossier.lieuPaiementFret || '—' }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">ÉCHÉANCE</label>
                                        <div class="fw-semibold">{{ formatarDate(dossier.echeance) }}</div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="text-muted fw-semibold">DATE DE LIVRAISON</label>
                                        <div class="fw-semibold">{{ formatarDate(dossier.dateLivraison) }}</div>
                                    </div>
                                    <div class="col-md-12">
                                        <label class="text-muted fw-semibold">ADRESSE LIVRAISON</label>
                                        <div class="fw-semibold">{{ dossier.adresseLivraison || '—' }}</div>
                                    </div>
                                </div>
                            </div>

                            <!-- Provision financière -->
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <h5 class="fw-bold mb-3 border-bottom pb-2">Provision Financière</h5>
                                <div class="row g-3 text-xs">
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">PROVISION DEMANDÉE</label>
                                        <div class="fw-bold text-primary fs-5">
                                            {{ Number(dossier.provisionDemandee || 0).toLocaleString('fr-MG') }} MGA
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">STATUT PROVISION</label>
                                        <div>
                                            <span
                                                :class="dossier.estProvisionne ? 'badge bg-success-subtle text-success border border-success-subtle' : 'badge bg-warning-subtle text-warning border border-warning-subtle'">
                                                {{ dossier.estProvisionne ? 'Provisionné' : 'En attente' }}
                                            </span>
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="text-muted fw-semibold">FRET PRÉPAYÉ</label>
                                        <div class="fw-semibold">{{ dossier.estPrepaye ? 'Oui' : 'Non' }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Col latérale: Marchandises + OT -->
                        <div class="col-lg-4">
                            <div class="card border-0 shadow-sm p-4 mb-4">
                                <h5 class="fw-bold mb-3 border-bottom pb-2">Ordre de Transit lié</h5>
                                <div v-if="!dossier.ordreTransit" class="text-muted text-xs">Aucun OT lié</div>
                                <div v-else class="text-xs">
                                    <div class="fw-bold text-primary mb-1">OT-{{ dossier.ordreTransit.numeroOrdreTransit
                                        }}</div>
                                    <div><strong>Expéditeur :</strong> {{ dossier.ordreTransit.expediteur?.nom }}</div>
                                    <div><strong>Destinataire :</strong> {{ dossier.ordreTransit.destinataire?.nom }}
                                    </div>
                                    <div><strong>Régime :</strong> {{ dossier.ordreTransit.typeRegime?.code }}</div>
                                </div>
                            </div>

                            <div class="card border-0 shadow-sm p-4">
                                <h5 class="fw-bold mb-3 border-bottom pb-2">Marchandises</h5>
                                <p class="text-muted text-xs">Les marchandises sont gérées via le module dédié.</p>
                                <router-link :to="`/marchandises?otId=${dossier.ordreTransit?.id}`"
                                    class="btn btn-outline-primary btn-sm w-100">
                                    Consulter les marchandises &rarr;
                                </router-link>
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
import DossierService from '../services/DossierService'
import PermissionService, { PORTEE_PROPRE_SUPPRESSION, ACTION_OT } from '../utils/permission.service'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const erreur = ref(null)
const dossier = ref({})

const peutSupprimer = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_SUPPRESSION, ACTION_OT))

onMounted(async () => {
    try {
        dossier.value = await DossierService.getDossierById(route.params.id)
    } catch (e) {
        erreur.value = "Impossible de récupérer ce dossier."
    } finally {
        loading.value = false
    }
})

async function supprimerDossier() {
    if (confirm("Supprimer définitivement ce dossier ?")) {
        loading.value = true
        try {
            await DossierService.deleteDossier(dossier.value.id)
            alert("Dossier supprimé.")
            router.push('/dossiers')
        } catch (e) {
            alert("Erreur lors de la suppression.")
            loading.value = false
        }
    }
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function obtenirBadgeStatut(nom) {
    if (!nom) return 'badge bg-secondary-subtle text-secondary'
    switch (nom.toLowerCase()) {
        case 'ouvert': return 'badge bg-success-subtle text-success border border-success-subtle'
        case 'clôturé': case 'cloture': return 'badge bg-secondary-subtle text-secondary'
        case 'en litige': return 'badge bg-danger-subtle text-danger border border-danger-subtle'
        default: return 'badge bg-warning-subtle text-warning border border-warning-subtle'
    }
}
</script>

<style scoped>
.text-xs {
    font-size: 0.75rem;
}
</style>

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
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-3">
                    <router-link to="/dossiers" class="text-decoration-none text-secondary">&larr; Retour aux dossiers</router-link>
                </div>

                <div v-if="loading" class="text-center py-5">
                    <div class="spinner-border text-secondary"></div>
                </div>
                <div v-else-if="erreur" class="alert alert-danger">{{ erreur }}</div>
                <div v-else>
                    <div class="d-flex justify-content-between align-items-start mb-4">
                        <div>
                            <h1 class="h4 fw-bold text-dark mb-1">{{ dossier.numeroDossier }}</h1>
                            <p class="text-secondary mb-0 small">Créé le {{ formatarDate(dossier.dateCreation) }}</p>
                        </div>
                        <div class="d-flex gap-2">
                            <router-link v-if="dossier.ordreTransit" :to="`/ordres-transit/${dossier.ordreTransit.id}`"
                                class="btn btn-outline-primary btn-sm">
                                Voir l'OT associé OT-{{ dossier.ordreTransit.numeroOrdreTransit }}
                            </router-link>
                            <button v-if="peutSupprimer" class="btn btn-outline-danger btn-sm" @click="supprimerDossier">Supprimer</button>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-lg-8">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Informations du Dossier</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3 small">
                                        <div class="col-md-6">
                                            <div class="text-secondary">CLIENT</div>
                                            <div class="fw-semibold">{{ dossier.client?.nom }} {{ dossier.client?.prenom }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">POUR COMPTE DE</div>
                                            <div class="fw-semibold">{{ dossier.pourCompte || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">PHASE</div>
                                            <div class="fw-semibold">{{ dossier.phaseDossier?.nom || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">INCOTERM</div>
                                            <div class="fw-semibold">{{ dossier.inconterme?.abreviation || '—' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">FRET</div>
                                            <div class="fw-semibold">{{ dossier.estPrepaye ? 'Prépayé' : 'Collect' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">PAYS ORIGINE → DESTINATION</div>
                                            <div class="fw-semibold">{{ dossier.paysOrigine?.nom || '—' }} → {{ dossier.paysDestination?.nom || '—' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">LIEU PAIEMENT FRET</div>
                                            <div class="fw-semibold">{{ dossier.lieuPaiementFret || '—' }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">ÉCHÉANCE</div>
                                            <div class="fw-semibold">{{ formatarDate(dossier.echeance) }}</div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">DATE DE LIVRAISON</div>
                                            <div class="fw-semibold">{{ formatarDate(dossier.dateLivraison) }}</div>
                                        </div>
                                        <div class="col-12">
                                            <div class="text-secondary">ADRESSE LIVRAISON</div>
                                            <div class="fw-semibold">{{ dossier.adresseLivraison || '—' }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Provision Financière</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3 small">
                                        <div class="col-md-4">
                                            <div class="text-secondary">PROVISION DEMANDÉE</div>
                                            <div class="fw-bold text-dark">
                                                {{ Number(dossier.provisionDemandee || 0).toLocaleString('fr-MG') }} MGA
                                            </div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">STATUT PROVISION</div>
                                            <div class="fw-semibold">{{ dossier.estProvisionne ? 'Provisionné' : 'En attente' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">FRET PRÉPAYÉ</div>
                                            <div class="fw-semibold">{{ dossier.estPrepaye ? 'Oui' : 'Non' }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-4">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Ordre de Transit lié</h5>
                                </div>
                                <div class="card-body small">
                                    <div v-if="!dossier.ordreTransit" class="text-secondary">Aucun OT lié</div>
                                    <div v-else>
                                        <div class="fw-bold text-dark mb-1">{{ dossier.ordreTransit.numeroOrdreTransit }}</div>
                                        <div><strong>Expéditeur :</strong> {{ dossier.ordreTransit.expediteur?.nom }}</div>
                                        <div><strong>Destinataire :</strong> {{ dossier.ordreTransit.destinataire?.nom }}</div>
                                        <div><strong>Régime :</strong> {{ dossier.ordreTransit.typeRegime?.code }}</div>
                                    </div>
                                </div>
                            </div>

                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Marchandises</h5>
                                </div>
                                <div class="card-body">
                                    <p class="text-secondary small mb-2">Les marchandises sont gérées via le module dédié.</p>
                                    <router-link :to="`/marchandises?otId=${dossier.ordreTransit?.id}`" class="btn btn-outline-primary btn-sm w-100">
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
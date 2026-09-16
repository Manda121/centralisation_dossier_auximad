<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
import DossierService from '../services/DossierService'
import InterAgenceService from '../services/InterAgenceService'
import DemandeInteragenceService from '../services/DemandeInteragenceService'
import ListeMarchandises from '@/components/marchandise/ListeMarchandises.vue'
import DocumentsSection from '@/components/DocumentsSection.vue'
import PermissionService, {
    PORTEE_PROPRE_SUPPRESSION, PORTEE_PROPRE_ECRITURE,
    ACTION_OT, ACTION_SOUS_DOSSIER
} from '../utils/permission.service'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const erreur = ref(null)
const dossier = ref({})
const dossierMereLien = ref(null)   // lien interagence si ce dossier est un sous-dossier
const sousDossiers = ref([])         // sous-dossiers de ce dossier
const demandesInteragence = ref([]) // demandes d'interagence pour ce dossier

const showDemandeModal = ref(false)
const agencesDisponibles = ref([])
const submittingDemande = ref(false)
const demandeErreur = ref(null)
const demandeSucces = ref(null)
const formDemande = reactive({
    idAgenceDestinataire: '',
    motif: ''
})

const peutSupprimer = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_SUPPRESSION, ACTION_OT))
const peutCreerSousDossier = computed(() => PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_SOUS_DOSSIER))

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

async function fetchDossier(id) {
    loading.value = true
    erreur.value = null

    loading.value = true
    erreur.value = null
    dossier.value = {}
    dossierMereLien.value = null
    sousDossiers.value = []
    demandesInteragence.value = []
    try {
        dossier.value = await DossierService.getDossierById(route.params.id)

        // Charger les données interagences et demandes en parallèle
        const [merenLien, enfants, demandesRes] = await Promise.allSettled([
            InterAgenceService.getDossierMere(route.params.id),
            InterAgenceService.getSousDossiers(route.params.id),
            DemandeInteragenceService.getDemandesParDossier(route.params.id)
        ])

        if (merenLien.status === 'fulfilled') {
            dossierMereLien.value = merenLien.value
        }
        if (enfants.status === 'fulfilled') {
            sousDossiers.value = enfants.value
        }
        if (demandesRes.status === 'fulfilled') {
            demandesInteragence.value = demandesRes.value || []
        }
    } catch (e) {
        erreur.value = "Impossible de récupérer ce dossier."
    } finally {
        loading.value = false
    }
}
onMounted(async () => {

    await fetchDossier(route.params.id)

    // try {
    //     dossier.value = await DossierService.getDossierById(route.params.id)

    //     // Charger les données interagences et demandes en parallèle
    //     const [merenLien, enfants, demandesRes] = await Promise.allSettled([
    //         InterAgenceService.getDossierMere(route.params.id),
    //         InterAgenceService.getSousDossiers(route.params.id),
    //         DemandeInteragenceService.getDemandesParDossier(route.params.id)
    //     ])

    //     if (merenLien.status === 'fulfilled') {
    //         dossierMereLien.value = merenLien.value
    //     }
    //     if (enfants.status === 'fulfilled') {
    //         sousDossiers.value = enfants.value
    //     }
    //     if (demandesRes.status === 'fulfilled') {
    //         demandesInteragence.value = demandesRes.value || []
    //     }
    // } catch (e) {
    //     erreur.value = "Impossible de récupérer ce dossier."
    // } finally {
    //     loading.value = false
    // }
})

watch(
    () => route.params.id,
    (newId) => {
        if (newId) {
            fetchDossier(newId)
        }
    }
)

async function ouvrirDemandeModal() {
    demandeErreur.value = null
    demandeSucces.value = null
    formDemande.idAgenceDestinataire = ''
    formDemande.motif = ''
    showDemandeModal.value = true
    try {
        const res = await axios.get('http://localhost:8080/api/agences', { headers: authHeader() })
        agencesDisponibles.value = res.data.filter(a => a.id !== dossier.value.agence?.id)
    } catch (e) {
        console.error("Erreur chargement des agences", e)
    }
}

async function envoyerDemande() {
    if (!formDemande.idAgenceDestinataire) {
        demandeErreur.value = "Veuillez sélectionner une agence destinataire."
        return
    }
    submittingDemande.value = true
    demandeErreur.value = null
    demandeSucces.value = null
    try {
        const payload = {
            idDossier: dossier.value.id,
            idAgenceDestinataire: parseInt(formDemande.idAgenceDestinataire),
            motif: formDemande.motif || null
        }
        await DemandeInteragenceService.creerDemande(payload)
        demandeSucces.value = "Demande d'inter-agence envoyée avec succès !"
        demandesInteragence.value = await DemandeInteragenceService.getDemandesParDossier(dossier.value.id)
        setTimeout(() => {
            showDemandeModal.value = false
        }, 1200)
    } catch (e) {
        demandeErreur.value = e.response?.data || "Erreur lors de l'envoi de la demande."
    } finally {
        submittingDemande.value = false
    }
}

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
                    <router-link to="/dossiers" class="text-decoration-none text-secondary">&larr; Retour aux
                        dossiers</router-link>
                </div>

                <PageSkeleton v-if="loading" variant="detail" :with-layout="false" />
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
                            <button v-if="!dossierMereLien" class="btn btn-outline-primary btn-sm"
                                @click="ouvrirDemandeModal">
                                ✉ Demander un inter-agence
                            </button>
                            <button v-if="peutSupprimer" class="btn btn-outline-danger btn-sm"
                                @click="supprimerDossier">Supprimer</button>
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
                                            <div class="fw-semibold">{{ dossier.client?.nom }} {{ dossier.client?.prenom
                                            }}</div>
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
                                            <div class="fw-semibold">{{ dossier.estPrepaye ? 'Prépayé' : 'Collect' }}
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="text-secondary">PAYS ORIGINE → DESTINATION</div>
                                            <div class="fw-semibold">{{ dossier.paysOrigine?.nom || '—' }} → {{
                                                dossier.paysDestination?.nom || '—' }}</div>
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
                                            <div class="fw-semibold">{{ dossier.estProvisionne ? 'Provisionné' :
                                                'attente' }}</div>
                                        </div>
                                        <div class="col-md-4">
                                            <div class="text-secondary">FRET PRÉPAYÉ</div>
                                            <div class="fw-semibold">{{ dossier.estPrepaye ? 'Oui' : 'Non' }}</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Marchandises -->
                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Marchandises</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3 small">
                                        <p class="text-secondary small mb-2">Les marchandises sont gérées via le module
                                            dédié.</p>
                                        <router-link :to="`/marchandises?otId=${dossier.ordreTransit?.id}`"
                                            class="btn btn-outline-primary btn-sm w-100">
                                            modifier &rarr;
                                        </router-link>
                                        <ListeMarchandises :ordreTransit="dossier.ordreTransit" />
                                    </div>
                                </div>
                            </div>

                            <!-- Documents du dossier -->
                            <div class="card border shadow-sm mt-3">
                                <div
                                    class="card-header bg-white border-bottom py-2 d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0 fw-semibold text-dark">Documents</h5>
                                    <router-link
                                        :to="{ path: '/documents/dossiers', query: { dossierId: dossier.id } }"
                                        class="btn btn-outline-primary btn-sm">
                                        Gérer les documents
                                    </router-link>
                                </div>
                                <div class="card-body">
                                    <DocumentsSection :dossierId="dossier.id"
                                        :typeProductionId="dossier.typeProduction?.id" :readOnly="true" />
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-4">
                            <!-- Dossier mère (si sous-dossier) -->
                            <div v-if="dossierMereLien" class="card border-warning shadow-sm mb-3">
                                <div
                                    class="card-header bg-warning-subtle border-bottom py-2 d-flex align-items-center gap-2">
                                    <svg viewBox="0 0 16 16" fill="currentColor" width="14" height="14"
                                        class="text-warning-emphasis">
                                        <path
                                            d="M1 0 0 1l2.2 3.081a1 1 0 0 0 .815.419h.07a1 1 0 0 1 .708.293l2.675 2.675-2.617 2.654A3.003 3.003 0 0 0 0 13a3 3 0 1 0 5.878-.851l2.654-2.617.968.968-.305.914a1 1 0 0 0 .242 1.023l3.27 3.27a.997.997 0 0 0 1.414 0l1.586-1.586a.997.997 0 0 0 0-1.414l-3.27-3.27a1 1 0 0 0-1.023-.242L10.5 9.5l-.96-.96 2.68-2.643A3.005 3.005 0 0 0 16 3q0-.405-.102-.777l-2.14 2.141L12 4l-.364-1.757L13.777.102a3 3 0 0 0-3.675 3.68L7.462 6.46 4.793 3.793a1 1 0 0 1-.293-.707v-.071a1 1 0 0 0-.419-.814z" />
                                    </svg>
                                    <span class="fw-semibold text-warning-emphasis small">Sous-dossier de</span>
                                </div>
                                <div class="card-body small">
                                    <div class="fw-bold text-dark mb-1">
                                        <router-link :to="`/dossiers/${dossierMereLien.dossierMere.id}`"
                                            class="text-decoration-none text-primary">
                                            {{ dossierMereLien.dossierMere.numeroDossier }}
                                        </router-link>
                                    </div>
                                    <div v-if="dossierMereLien.motif" class="text-secondary mt-1">
                                        <strong>Motif :</strong> {{ dossierMereLien.motif }}
                                    </div>
                                    <div class="text-secondary mt-1">
                                        <strong>Agence mère :</strong> {{ dossierMereLien.dossierMere.agence?.nom || '—'
                                        }}
                                    </div>
                                </div>
                            </div>

                            <!-- OT lié -->
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Ordre de Transit lié</h5>
                                </div>
                                <div class="card-body small">
                                    <div v-if="!dossier.ordreTransit" class="text-secondary">Aucun OT lié</div>
                                    <div v-else>
                                        <div class="fw-bold text-dark mb-1">{{ dossier.ordreTransit.numeroOrdreTransit
                                        }}</div>
                                        <div><strong>Expéditeur :</strong> {{ dossier.ordreTransit.expediteur?.nom }}
                                        </div>
                                        <div><strong>Destinataire :</strong> {{ dossier.ordreTransit.destinataire?.nom
                                        }}</div>
                                        <div><strong>Régime :</strong> {{ dossier.ordreTransit.typeRegime?.code }}</div>
                                    </div>
                                </div>
                            </div>

                            <!-- Sous-dossiers inter-agences -->
                            <div class="card border shadow-sm mb-3">
                                <div
                                    class="card-header bg-white border-bottom py-2 d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0 fw-semibold text-dark">Sous-Dossiers</h5>
                                    <router-link v-if="peutCreerSousDossier && !dossierMereLien"
                                        :to="`/dossiers/${dossier.id}/sous-dossier/creer`"
                                        class="btn btn-outline-primary btn-sm">
                                        + Inter-agence
                                    </router-link>
                                </div>
                                <div class="card-body small">
                                    <div v-if="sousDossiers.length === 0" class="text-secondary">Aucun sous-dossier.
                                    </div>
                                    <div v-else class="d-flex flex-column gap-2">
                                        <div v-for="lien in sousDossiers" :key="lien.id"
                                            class="d-flex justify-content-between align-items-center border rounded px-2 py-2">
                                            <div>
                                                <router-link :to="`/dossiers/${lien.sousDossier.id}`"
                                                    class="fw-semibold text-primary text-decoration-none">
                                                    {{ lien.sousDossier.numeroDossier }}
                                                </router-link>
                                                <div class="text-secondary" style="font-size:0.75rem">
                                                    {{ lien.sousDossier.agence?.nom || '—' }}
                                                    <span v-if="lien.motif"> — {{ lien.motif }}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Demandes Inter-agences -->
                            <div class="card border shadow-sm mb-3">
                                <div
                                    class="card-header bg-white border-bottom py-2 d-flex justify-content-between align-items-center">
                                    <h5 class="mb-0 fw-semibold text-dark">Demandes Inter-Agences</h5>
                                    <button v-if="!dossierMereLien" class="btn btn-sm btn-link p-0 text-decoration-none"
                                        @click="ouvrirDemandeModal">
                                        + Demander
                                    </button>
                                </div>
                                <div class="card-body small">
                                    <div v-if="demandesInteragence.length === 0" class="text-secondary">Aucune demande
                                        effectuée.</div>
                                    <div v-else class="d-flex flex-column gap-2">
                                        <div v-for="dem in demandesInteragence" :key="dem.id"
                                            class="p-2 border rounded bg-light">
                                            <div class="d-flex justify-content-between align-items-center mb-1">
                                                <span class="fw-semibold text-dark">
                                                    Vers {{ dem.agenceDestinataire?.nom }}
                                                </span>
                                                <span v-if="dem.statut === 'EN_ATTENTE'"
                                                    class="badge bg-warning text-dark">⏳ En attente</span>
                                                <span v-else-if="dem.statut === 'ACCEPTEE'"
                                                    class="badge bg-success text-white">✓ Acceptée</span>
                                                <span v-else-if="dem.statut === 'REFUSEE'"
                                                    class="badge bg-danger text-white">✕ Refusée</span>
                                            </div>
                                            <div class="text-secondary" style="font-size:0.75rem">
                                                Fait le {{ formatarDate(dem.dateDemande) }}
                                                <span v-if="dem.motif"> — {{ dem.motif }}</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
        </main>

        <!-- Modal Demande Inter-Agence -->
        <div v-if="showDemandeModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5)">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow">
                    <div class="modal-header bg-light py-3">
                        <h5 class="modal-title h6 fw-bold text-dark mb-0">Demander un transfert / partage inter-agence
                        </h5>
                        <button type="button" class="btn-close" @click="showDemandeModal = false"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div v-if="demandeErreur" class="alert alert-danger py-2 mb-3">{{ demandeErreur }}</div>
                        <div v-if="demandeSucces" class="alert alert-success py-2 mb-3">{{ demandeSucces }}</div>

                        <form @submit.prevent="envoyerDemande">
                            <div class="mb-3">
                                <label class="form-label fw-semibold text-dark">
                                    Agence destinataire <span class="text-danger">*</span>
                                </label>
                                <select v-model="formDemande.idAgenceDestinataire" class="form-select" required>
                                    <option value="">— Choisir l'agence —</option>
                                    <option v-for="a in agencesDisponibles" :key="a.id" :value="a.id">
                                        {{ a.nom }} ({{ a.ville }})
                                    </option>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label fw-semibold text-dark">Motif / Instruction pour la
                                    demande</label>
                                <textarea v-model="formDemande.motif" class="form-control" rows="3"
                                    placeholder="Ex: Demande de prise en charge des formalités douanières à l'arrivée…"></textarea>
                            </div>

                            <div class="d-flex gap-2 justify-content-end mt-4">
                                <button type="button" class="btn btn-outline-secondary"
                                    @click="showDemandeModal = false">Annuler</button>
                                <button type="submit" class="btn btn-primary" :disabled="submittingDemande">
                                    <span v-if="submittingDemande" class="spinner-border spinner-border-sm me-2"></span>
                                    {{ submittingDemande ? 'Envoi…' : 'Envoyer la demande' }}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped></style>
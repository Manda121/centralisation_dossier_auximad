<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
import OrdreTransit from '../models/OrdreTransit'
import OrdreTransitService from '../services/OrdreTransitService'
import ClientService from '../services/ClientService'

const router = useRouter()
const currentStep = ref(1)
const sauvegardeEnCours = ref(false)
const chargement = ref(true)

const otModel = ref(new OrdreTransit())
const contratModel = ref({
    numero: '',
    compagnie: null,
    navire: null,
    numeroVol: '',
    lieuEmission: '',
    notify: '',
    datePrevue: '',
    dateArrivee: ''
})

const clients = ref([])
const typeProductions = ref([])
const typeRegimes = ref([])
const bureaux = ref([])
const compagnies = ref([])
const navires = ref([])

const isMaritime = computed(() => {
    const mode = otModel.value.typeProduction?.idModeTransport
        ?? otModel.value.typeProduction?.id_mode_transport
        ?? otModel.value.typeProduction?.modeTransport?.id
    return mode === 1
})

const isAerien = computed(() => {
    const mode = otModel.value.typeProduction?.idModeTransport
        ?? otModel.value.typeProduction?.id_mode_transport
        ?? otModel.value.typeProduction?.modeTransport?.id
    return mode === 2
})

onMounted(async () => {
    try {
        const [cls, productions, regimes, bur, comps] = await Promise.all([
            ClientService.searchClients(0, 1000, 'nom', 'asc', {}),
            OrdreTransitService.getAllTypeProductions(),
            OrdreTransitService.getAllTypeRegimes(),
            OrdreTransitService.getAllBureaux(),
            OrdreTransitService.getCompagnies(1)
        ])
        clients.value = cls.content || []
        typeProductions.value = productions
        typeRegimes.value = regimes
        bureaux.value = bur
        compagnies.value = comps
    } catch (e) {
        console.error('Erreur chargement wizard', e)
    } finally {
        chargement.value = false
    }
})

async function notifierCompagnieChange() {
    contratModel.value.navire = null
    if (contratModel.value.compagnie) {
        try {
            navires.value = await OrdreTransitService.getNaviresByCompagnie(contratModel.value.compagnie.id)
        } catch (e) {
            console.error('Erreur navires', e)
        }
    } else {
        navires.value = []
    }
}

async function chargerCompagniesSelonMode() {
    contratModel.value.compagnie = null
    contratModel.value.navire = null
    navires.value = []
    const mode = otModel.value.typeProduction?.idModeTransport
        ?? otModel.value.typeProduction?.id_mode_transport
        ?? otModel.value.typeProduction?.modeTransport?.id
    if (!mode) {
        compagnies.value = []
        return
    }
    try {
        compagnies.value = await OrdreTransitService.getCompagnies(mode)
    } catch (e) {
        console.error('Erreur chargement compagnies', e)
        compagnies.value = []
    }
}

function etapeSuivante() {
    if (currentStep.value === 1) {
        if (!otModel.value.client || !otModel.value.typeProduction || !otModel.value.typeRegime) {
            alert('Veuillez renseigner tous les champs obligatoires (*) de l\'étape 1.')
            return
        }
        chargerCompagniesSelonMode()
    } else if (currentStep.value === 2) {
        if (!otModel.value.expediteur || !otModel.value.destinataire || !otModel.value.bureauDedouanement) {
            alert('Veuillez renseigner tous les champs obligatoires (*) de l\'étape 2.')
            return
        }
    }
    currentStep.value++
}

function etapePrecedente() {
    if (currentStep.value > 1) {
        currentStep.value--
    }
}

async function creerOrdreTransit() {
    sauvegardeEnCours.value = true
    try {
        const otPayload = otModel.value.toJSON()
        const savedOt = await OrdreTransitService.createOrdreTransit(otPayload)

        if (contratModel.value.numero || contratModel.value.compagnie || contratModel.value.numeroVol) {
            const contratPayload = {
                numero: contratModel.value.numero,
                compagnie: contratModel.value.compagnie ? { id: contratModel.value.compagnie.id } : null,
                navire: contratModel.value.navire ? { id: contratModel.value.navire.id } : null,
                n_vol: contratModel.value.numeroVol,
                lieu_emission: contratModel.value.lieuEmission,
                notify: contratModel.value.notify,
                date_prevue: contratModel.value.datePrevue ? new Date(contratModel.value.datePrevue).toISOString() : null,
                date_arrivee: contratModel.value.dateArrivee ? new Date(contratModel.value.dateArrivee).toISOString() : null
            }
            await OrdreTransitService.saveContrat(savedOt.id, contratPayload)
        }

        alert('Ordre de transit créé avec succès.')
        router.push(`/ordres-transit/${savedOt.id}`)
    } catch (e) {
        alert('Erreur technique lors de la création de l\'ordre de transit.')
    } finally {
        sauvegardeEnCours.value = false
    }
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-3">
                    <router-link to="/ordres-transit" class="text-decoration-none text-secondary">
                        &larr; Annuler et retourner aux ordres de transit
                    </router-link>
                </div>

                <PageSkeleton v-if="chargement" variant="form" :with-layout="false" />

                <template v-else>
                <div class="card border shadow-sm mb-3">
                    <div class="card-body p-3">
                        <div class="d-flex justify-content-between align-items-center">
                            <div class="text-center flex-fill" :class="{ 'fw-bold text-primary': currentStep === 1, 'text-secondary': currentStep !== 1 }">
                                <span class="badge rounded-pill me-1" :class="currentStep >= 1 ? 'bg-primary' : 'bg-secondary'">1</span>
                                Donneur d'ordre & Régime
                            </div>
                            <div class="text-center flex-fill" :class="{ 'fw-bold text-primary': currentStep === 2, 'text-secondary': currentStep !== 2 }">
                                <span class="badge rounded-pill me-1" :class="currentStep >= 2 ? 'bg-primary' : 'bg-secondary'">2</span>
                                Destinations & Acteurs
                            </div>
                            <div class="text-center flex-fill" :class="{ 'fw-bold text-primary': currentStep === 3, 'text-secondary': currentStep !== 3 }">
                                <span class="badge rounded-pill me-1" :class="currentStep >= 3 ? 'bg-primary' : 'bg-secondary'">3</span>
                                Contrat de transport
                            </div>
                            <div class="text-center flex-fill" :class="{ 'fw-bold text-primary': currentStep === 4, 'text-secondary': currentStep !== 4 }">
                                <span class="badge rounded-pill me-1" :class="currentStep >= 4 ? 'bg-primary' : 'bg-secondary'">4</span>
                                Validation
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card border shadow-sm">
                    <div class="card-body p-4">
                        <!-- Étape 1 -->
                        <div v-show="currentStep === 1">
                            <h4 class="h5 fw-bold text-dark mb-3">Étape 1 — Donneur d'ordre & Régime</h4>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Client Donneur d'ordre <span class="text-danger">*</span></label>
                                    <select v-model="otModel.client" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez le client</option>
                                        <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Type de Production / Transport Mode <span class="text-danger">*</span></label>
                                    <select v-model="otModel.typeProduction" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez type production</option>
                                        <option v-for="p in typeProductions" :key="p.id" :value="p">{{ p.nom }} {{ p.modeTransport.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Régime Douanier <span class="text-danger">*</span></label>
                                    <select v-model="otModel.typeRegime" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez le régime</option>
                                        <option v-for="r in typeRegimes" :key="r.id" :value="r">{{ r.code }} - {{ r.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">N° Commande (facultatif)</label>
                                    <input type="text" v-model="otModel.numeroCommande" class="form-control form-control-sm" placeholder="ex: CMD-2026-001">
                                </div>
                            </div>
                        </div>

                        <!-- Étape 2 -->
                        <div v-show="currentStep === 2">
                            <h4 class="h5 fw-bold text-dark mb-3">Étape 2 — Destinations & Acteurs</h4>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Expéditeur (Shipper) <span class="text-danger">*</span></label>
                                    <select v-model="otModel.expediteur" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez l'expéditeur</option>
                                        <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Destinataire (Consignee) <span class="text-danger">*</span></label>
                                    <select v-model="otModel.destinataire" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez le destinataire</option>
                                        <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Bureau de dédouanement <span class="text-danger">*</span></label>
                                    <select v-model="otModel.bureauDedouanement" class="form-select form-select-sm" required>
                                        <option :value="null">Sélectionnez le bureau</option>
                                        <option v-for="b in bureaux" :key="b.id" :value="b">{{ b.codeBureau }} - {{ b.nom }}</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Destination Finale</label>
                                    <input type="text" v-model="otModel.destinationFinale" class="form-control form-control-sm" placeholder="ex: Zone Franche Tanjombato">
                                </div>
                                <div class="col-md-6">
                                    <div class="form-check form-switch mt-4">
                                        <input class="form-check-input" type="checkbox" id="estAssure" v-model="otModel.estAssure">
                                        <label class="form-check-label small fw-semibold" for="estAssure">Marchandise Assurée</label>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label small fw-semibold text-secondary">Adresse de Facturation (Client)</label>
                                    <select v-model="otModel.destinationFacture" class="form-select form-select-sm">
                                        <option :value="null">Sélectionnez le payeur</option>
                                        <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                        <!-- Étape 3 — Contrat de transport (conditionnel) -->
                        <div v-show="currentStep === 3">
                            <h4 class="h5 fw-bold text-dark mb-3">Étape 3 — Contrat de transport</h4>

                            <div v-if="!otModel.typeProduction" class="alert alert-secondary small">
                                Veuillez d'abord sélectionner un type de production à l'étape 1.
                            </div>

                            <div v-else-if="isMaritime">
                                <p class="text-secondary small mb-3">Mode maritime détecté — renseignez les informations du connaissement (B/L).</p>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Compagnie maritime</label>
                                        <select v-model="contratModel.compagnie" class="form-select form-select-sm" @change="notifierCompagnieChange">
                                            <option :value="null">Sélectionnez la compagnie</option>
                                            <option v-for="comp in compagnies" :key="comp.id" :value="comp">{{ comp.nom }} ({{ comp.code }})</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Navire</label>
                                        <select v-model="contratModel.navire" class="form-select form-select-sm" :disabled="!contratModel.compagnie">
                                            <option :value="null">Sélectionnez le navire</option>
                                            <option v-for="nav in navires" :key="nav.id" :value="nav">{{ nav.nom }} (IMO: {{ nav.imo }})</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Numéro B/L</label>
                                        <input type="text" v-model="contratModel.numero" class="form-control form-control-sm" placeholder="ex: HBLM4991209">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Lieu d'émission</label>
                                        <input type="text" v-model="contratModel.lieuEmission" class="form-control form-control-sm" placeholder="ex: Antananarivo">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Notify party</label>
                                        <input type="text" v-model="contratModel.notify" class="form-control form-control-sm" placeholder="ex: Auximad Tana">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Date prévue de départ / d'arrivée</label>
                                        <input type="datetime-local" v-model="contratModel.datePrevue" class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Date d'arrivée effective</label>
                                        <input type="datetime-local" v-model="contratModel.dateArrivee" class="form-control form-control-sm">
                                    </div>
                                </div>
                            </div>

                            <div v-else-if="isAerien">
                                <p class="text-secondary small mb-3">Mode aérien détecté — renseignez les informations de la LTA / vol.</p>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Compagnie aérienne</label>
                                        <select v-model="contratModel.compagnie" class="form-select form-select-sm">
                                            <option :value="null">Sélectionnez la compagnie</option>
                                            <option v-for="comp in compagnies" :key="comp.id" :value="comp">{{ comp.nom }} ({{ comp.code }})</option>
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">N° de Vol</label>
                                        <input type="text" v-model="contratModel.numeroVol" class="form-control form-control-sm" placeholder="ex: MD051">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Numéro LTA</label>
                                        <input type="text" v-model="contratModel.numero" class="form-control form-control-sm" placeholder="ex: 057-12345678">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Lieu d'émission</label>
                                        <input type="text" v-model="contratModel.lieuEmission" class="form-control form-control-sm" placeholder="ex: Antananarivo">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Notify party</label>
                                        <input type="text" v-model="contratModel.notify" class="form-control form-control-sm" placeholder="ex: Auximad Tana">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Date prévue de départ / d'arrivée</label>
                                        <input type="datetime-local" v-model="contratModel.datePrevue" class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small fw-semibold text-secondary">Date d'arrivée effective</label>
                                        <input type="datetime-local" v-model="contratModel.dateArrivee" class="form-control form-control-sm">
                                    </div>
                                </div>
                            </div>

                            <div v-else class="alert alert-secondary small">
                                Mode de transport non reconnu pour ce type de production. Les champs de contrat ne sont pas disponibles.
                            </div>
                        </div>

                        <!-- Étape 4 -->
                        <div v-show="currentStep === 4">
                            <h4 class="h5 fw-bold text-dark mb-3">Étape 4 — Consultation & Validation</h4>
                            <div class="alert alert-secondary small py-2">
                                Veuillez vérifier les informations ci-dessous avant d'enregistrer l'ordre de transit.
                            </div>
                            <div class="row g-3">
                                <div class="col-md-6">
                                    <div class="card border bg-light">
                                        <div class="card-body p-3">
                                            <h6 class="fw-semibold border-bottom pb-2">Résumé administratif</h6>
                                            <ul class="list-unstyled small mb-0">
                                                <li class="mb-1"><strong>Donneur d'ordre :</strong> {{ otModel.client?.nom }} {{ otModel.client?.prenom }}</li>
                                                <li class="mb-1"><strong>Type de production :</strong> {{ otModel.typeProduction?.nom }}</li>
                                                <li class="mb-1"><strong>Régime douanier :</strong> {{ otModel.typeRegime?.code }} - {{ otModel.typeRegime?.nom }}</li>
                                                <li class="mb-1"><strong>Numéro de commande :</strong> {{ otModel.numeroCommande || 'Non fourni' }}</li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="card border bg-light">
                                        <div class="card-body p-3">
                                            <h6 class="fw-semibold border-bottom pb-2">Résumé logistique</h6>
                                            <ul class="list-unstyled small mb-0">
                                                <li class="mb-1"><strong>Expéditeur :</strong> {{ otModel.expediteur?.nom }} {{ otModel.expediteur?.prenom }}</li>
                                                <li class="mb-1"><strong>Destinataire :</strong> {{ otModel.destinataire?.nom }} {{ otModel.destinataire?.prenom }}</li>
                                                <li class="mb-1"><strong>Bureau dédouanement :</strong> {{ otModel.bureauDedouanement?.nom }}</li>
                                                <li class="mb-1"><strong>Assuré :</strong> {{ otModel.estAssure ? 'Oui' : 'Non' }}</li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-12">
                                    <div class="card border bg-light">
                                        <div class="card-body p-3">
                                            <h6 class="fw-semibold border-bottom pb-2">Contrat de transport associé</h6>
                                            <div class="row small">
                                                <div class="col-md-4"><strong>Numéro B/L ou LTA :</strong> {{ contratModel.numero || 'Aucun' }}</div>
                                                <div class="col-md-4"><strong>Compagnie :</strong> {{ contratModel.compagnie?.nom || 'Non spécifiée' }}</div>
                                                <div class="col-md-4">
                                                    <strong v-if="isMaritime">Navire :</strong>
                                                    <strong v-else-if="isAerien">Vol :</strong>
                                                    <strong v-else>Navire / Vol :</strong>
                                                    {{ contratModel.navire?.nom || contratModel.numeroVol || 'Aucun' }}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="d-flex justify-content-between mt-4 pt-3 border-top">
                            <button type="button" class="btn btn-secondary btn-sm" :disabled="currentStep === 1" @click="etapePrecedente">
                                &larr; Précédent
                            </button>
                            <div>
                                <button v-if="currentStep < 4" type="button" class="btn btn-primary btn-sm" @click="etapeSuivante">
                                    Suivant &rarr;
                                </button>
                                <button v-else type="button" class="btn btn-success btn-sm" @click="creerOrdreTransit" :disabled="sauvegardeEnCours">
                                    <span v-if="sauvegardeEnCours" class="spinner-border spinner-border-sm me-1" role="status"></span>
                                    Finaliser la création
                                </button>
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
</style>
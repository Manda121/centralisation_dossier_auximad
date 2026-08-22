<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- Back link -->
                <div class="mb-4">
                    <router-link to="/ordres-transit"
                        class="text-decoration-none text-muted d-inline-flex align-items-center gap-2">
                        &larr; Annuler et retourner aux ordres de transit
                    </router-link>
                </div>

                <!-- Wizard Progress Indicator -->
                <div class="card border-0 shadow-sm mb-4">
                    <div class="card-body p-4">
                        <div class="d-flex justify-content-between align-items-center wizard-steps">
                            <div class="step-item"
                                :class="{ 'active': currentStep === 1, 'completed': currentStep > 1 }">
                                <span class="step-badge">1</span>
                                <span class="step-label">Donneur d'ordre & Regime</span>
                            </div>
                            <div class="step-divider"></div>
                            <div class="step-item"
                                :class="{ 'active': currentStep === 2, 'completed': currentStep > 2 }">
                                <span class="step-badge">2</span>
                                <span class="step-label">Destinations & Acteurs</span>
                            </div>
                            <div class="step-divider"></div>
                            <div class="step-item"
                                :class="{ 'active': currentStep === 3, 'completed': currentStep > 3 }">
                                <span class="step-badge">3</span>
                                <span class="step-label">Contrat de transport</span>
                            </div>
                            <div class="step-divider"></div>
                            <div class="step-item"
                                :class="{ 'active': currentStep === 4, 'completed': currentStep > 4 }">
                                <span class="step-badge">4</span>
                                <span class="step-label">Validation</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Multistep wizard container with smooth left/right transitions simulation -->
                <div class="card border-0 shadow-sm p-4 relative" style="min-height: 400px;">

                    <!-- Etape 1: Donneur d'ordre, Type Production, Regime -->
                    <div v-show="currentStep === 1" class="transition-slide">
                        <h4 class="fw-bold mb-3 text-primary">Étape 1 — Donneur d'ordre & Régime</h4>
                        <div class="row g-4">
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Client Donneur d'ordre <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.client" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez le client</option>
                                    <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Type de Production / Transport Mode <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.typeProduction" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez type production</option>
                                    <option v-for="p in typeProductions" :key="p.id" :value="p">{{ p.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Régime Douanier <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.typeRegime" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez le régime</option>
                                    <option v-for="r in typeRegimes" :key="r.id" :value="r">{{ r.code }} - {{ r.nom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">N° Commande (facultatif)</label>
                                <input type="text" v-model="otModel.numeroCommande" class="form-control form-control-sm"
                                    placeholder="ex: CMD-2026-001">
                            </div>
                        </div>
                    </div>

                    <!-- Etape 2: Acteurs (Expediteur, Destinataire, Facturation) & Bureau/Dest finale -->
                    <div v-show="currentStep === 2" class="transition-slide">
                        <h4 class="fw-bold mb-3 text-primary">Étape 2 — Destinations & Acteurs</h4>
                        <div class="row g-4">
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Expéditeur (Shipper) <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.expediteur" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez l'expéditeur</option>
                                    <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Destinataire (Consignee) <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.destinataire" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez le destinataire</option>
                                    <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Bureau de dédouanement <span
                                        class="text-danger">*</span></label>
                                <select v-model="otModel.bureauDedouanement" class="form-select form-select-sm"
                                    required>
                                    <option :value="null">Sélectionnez le bureau</option>
                                    <option v-for="b in bureaux" :key="b.id" :value="b">{{ b.codeBureau }} - {{ b.nom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Destination Finale</label>
                                <input type="text" v-model="otModel.destinationFinale"
                                    class="form-control form-control-sm" placeholder="ex: Zone Franche Tanjombato">
                            </div>
                            <div class="col-md-6">
                                <div class="form-check form-switch mt-4">
                                    <input class="form-check-input" type="checkbox" id="estAssure"
                                        v-model="otModel.estAssure">
                                    <label class="form-check-label fw-semibold text-xs" for="estAssure">Marchandise
                                        Assurée</label>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Adresse de Facturation (Client)</label>
                                <select v-model="otModel.destinationFacture" class="form-select form-select-sm">
                                    <option :value="null">Sélectionnez le payeur</option>
                                    <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}
                                    </option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <!-- Etape 3: Contrat de transport -->
                    <div v-show="currentStep === 3" class="transition-slide">
                        <h4 class="fw-bold mb-3 text-primary">Étape 3 — Contrat de transport</h4>
                        <p class="text-muted text-xs mb-4">Saisissez les informations logistiques de transport (B/L,
                            Vol, Companie).</p>

                        <div class="row g-4">
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Transporteur / Compagnie maritime ou
                                    aérienne</label>
                                <select v-model="contratModel.compagnie" class="form-select form-select-sm"
                                    @change="notifierCompagnieChange">
                                    <option :value="null">Sélectionnez la compagnie</option>
                                    <option v-for="comp in compagnies" :key="comp.id" :value="comp">{{ comp.nom }} ({{
                                        comp.code }})</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Navire (si maritime)</label>
                                <select v-model="contratModel.navire" class="form-select form-select-sm"
                                    :disabled="!contratModel.compagnie">
                                    <option :value="null">Sélectionnez le navire</option>
                                    <option v-for="nav in navires" :key="nav.id" :value="nav">{{ nav.nom }} (IMO: {{
                                        nav.imo }})</option>
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Numéro B/L ou LTA</label>
                                <input type="text" v-model="contratModel.numero" class="form-control form-control-sm"
                                    placeholder="ex: HBLM4991209">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">N° de Vol (si aérien)</label>
                                <input type="text" v-model="contratModel.numeroVol" class="form-control form-control-sm"
                                    placeholder="ex: MD051">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Lieu d'émission</label>
                                <input type="text" v-model="contratModel.lieuEmission"
                                    class="form-control form-control-sm" placeholder="ex: Antananarivo">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Notify party</label>
                                <input type="text" v-model="contratModel.notify" class="form-control form-control-sm"
                                    placeholder="ex: Auximad Tana">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Date prévue de départ / d'arrivée</label>
                                <input type="datetime-local" v-model="contratModel.datePrevue"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label text-xs fw-semibold">Date d'arrivée effective</label>
                                <input type="datetime-local" v-model="contratModel.dateArrivee"
                                    class="form-control form-control-sm">
                            </div>
                        </div>
                    </div>

                    <!-- Etape 4: Recap & Validation finale -->
                    <div v-show="currentStep === 4" class="transition-slide">
                        <h4 class="fw-bold mb-3 text-primary">Étape 4 — Consultation & Validation</h4>
                        <div class="alert alert-info py-2 text-xs" role="alert">
                            Veuillez vérifier les informations ci-dessous avant d'enregistrer l'ordre de transit.
                        </div>

                        <div class="row g-4 mt-1">
                            <div class="col-md-6">
                                <div class="card border bg-light p-3">
                                    <h6 class="fw-bold border-bottom pb-2">Résumé administratif</h6>
                                    <ul class="list-unstyled text-xs">
                                        <li class="mb-1"><strong>Donneur d'ordre :</strong> {{ otModel.client?.nom }} {{
                                            otModel.client?.prenom }}</li>
                                        <li class="mb-1"><strong>Type de production :</strong> {{
                                            otModel.typeProduction?.nom }}</li>
                                        <li class="mb-1"><strong>Régime douanier :</strong> {{ otModel.typeRegime?.code
                                            }} - {{ otModel.typeRegime?.nom }}</li>
                                        <li class="mb-1"><strong>Numéro de commande :</strong> {{ otModel.numeroCommande
                                            || 'Non fourni' }}</li>
                                    </ul>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card border bg-light p-3">
                                    <h6 class="fw-bold border-bottom pb-2">Résumé logistique</h6>
                                    <ul class="list-unstyled text-xs">
                                        <li class="mb-1"><strong>Expéditeur :</strong> {{ otModel.expediteur?.nom }} {{
                                            otModel.expediteur?.prenom }}</li>
                                        <li class="mb-1"><strong>Destinataire :</strong> {{ otModel.destinataire?.nom }}
                                            {{ otModel.destinataire?.prenom }}</li>
                                        <li class="mb-1"><strong>Bureau dédouanement :</strong> {{
                                            otModel.bureauDedouanement?.nom }}</li>
                                        <li class="mb-1"><strong>Assuré :</strong> {{ otModel.estAssure ? 'Oui' : 'Non'
                                            }}</li>
                                    </ul>
                                </div>
                            </div>
                            <div class="col-md-12">
                                <div class="card border bg-light p-3">
                                    <h6 class="fw-bold border-bottom pb-2">Contrat de transport associé</h6>
                                    <div class="row text-xs">
                                        <div class="col-md-4"><strong>Numéro B/L ou LTA :</strong> {{
                                            contratModel.numero || 'Aucun' }}</div>
                                        <div class="col-md-4"><strong>Compagnie :</strong> {{
                                            contratModel.compagnie?.nom || 'Non spécifiée' }}</div>
                                        <div class="col-md-4"><strong>Navire / Vol :</strong> {{
                                            contratModel.navire?.nom || contratModel.numeroVol || 'Aucun' }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Wizard actions buttons -->
                    <div class="d-flex justify-content-between mt-5 pt-4 border-top">
                        <button type="button" class="btn btn-secondary btn-sm" :disabled="currentStep === 1"
                            @click="etapePrecedente">
                            &larr; Précédent
                        </button>
                        <div>
                            <button v-if="currentStep < 4" type="button" class="btn btn-primary btn-sm"
                                @click="etapeSuivante">
                                Suivant &rarr;
                            </button>
                            <button v-else type="button" class="btn btn-success btn-sm" @click="creerOrdreTransit"
                                :disabled="sauvegardeEnCours">
                                <span v-if="sauvegardeEnCours" class="spinner-border spinner-border-sm me-2"
                                    role="status"></span>
                                Finaliser la création
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import OrdreTransit from '../models/OrdreTransit'
import OrdreTransitService from '../services/OrdreTransitService'
import ClientService from '../services/ClientService'

const router = useRouter()
const currentStep = ref(1)
const sauvegardeEnCours = ref(false)

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

onMounted(async () => {
    try {
        const [cls, productions, regimes, bur, comps] = await Promise.all([
            ClientService.searchClients(0, 1000, 'nom', 'asc', {}),
            OrdreTransitService.getAllTypeProductions(),
            OrdreTransitService.getAllTypeRegimes(),
            OrdreTransitService.getAllBureaux(),
            OrdreTransitService.getCompagnies(1) // par défaut compagnies maritimes
        ])
        clients.value = cls.content || []
        typeProductions.value = productions
        typeRegimes.value = regimes
        bureaux.value = bur
        compagnies.value = comps
    } catch (e) {
        console.error('Erreur chargement wizard', e)
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

function etapeSuivante() {
    if (currentStep.value === 1) {
        if (!otModel.value.client || !otModel.value.typeProduction || !otModel.value.typeRegime) {
            alert('Veuillez renseigner tous les champs obligatoires (*) de l\'étape 1.')
            return
        }
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

        // Sauvegarder le contrat de transport si des informations sont renseignées
        if (contratModel.value.numero || contratModel.value.compagnie) {
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

<style scoped>
.text-xs {
    font-size: 0.75rem;
}

.wizard-steps {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.step-item {
    text-align: center;
    position: relative;
    z-index: 10;
    opacity: 0.5;
}

.step-item.active {
    opacity: 1;
}

.step-badge {
    display: inline-block;
    width: 32px;
    height: 32px;
    line-height: 32px;
    border-radius: 50%;
    background-color: var(--bs-secondary);
    color: white;
    font-weight: bold;
    margin-bottom: 8px;
}

.step-item.active .step-badge {
    background-color: var(--bs-primary);
}

.step-item.completed .step-badge {
    background-color: var(--bs-success);
}

.step-label {
    display: block;
    font-size: 0.75rem;
    font-weight: 600;
}

.step-divider {
    height: 2px;
    background-color: #dee2e6;
    flex-grow: 1;
    margin: 0 16px;
    margin-top: -24px;
}

.relative {
    position: relative;
}

.transition-slide {
    transition: transform 0.4s ease, opacity 0.4s ease;
}
</style>

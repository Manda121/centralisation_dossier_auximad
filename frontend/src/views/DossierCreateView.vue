<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-4">
                    <router-link to="/dossiers" class="text-decoration-none text-muted">&larr; Retour aux
                        dossiers</router-link>
                </div>

                <PageSkeleton v-if="chargement" variant="form" :with-layout="false" />

                <div v-else class="card border-0 shadow-sm p-4 mb-4">
                    <h1 class="h4 fw-bold text-primary mb-4">Créer un nouveau Dossier</h1>

                    <!-- Pré-remplissage depuis un OT validé -->
                    <div class="alert alert-info text-xs py-2 mb-4">
                        Vous pouvez pré-remplir ce formulaire à partir d'un <strong>Ordre de Transit validé</strong>.
                        Saisissez son numéro ci-dessous.
                    </div>
                    <div class="row g-3 mb-4 align-items-end">
                        <div class="col-md-4">
                            <label class="form-label text-xs fw-semibold">N° Ordre de Transit (pour pré-remplir)</label>
                            <div class="input-group input-group-sm">
                                <input type="number" v-model="rechercheOt" class="form-control" placeholder="ex: 10001">
                                <button class="btn btn-outline-secondary" :disabled="chargementOt" @click="preRemplirDepuisOt">
                                    <span v-if="chargementOt" class="spinner-border spinner-border-sm me-1"></span>
                                    Charger OT
                                </button>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <span v-if="otCharge"
                                class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2">
                                OT-{{ otCharge.numeroOrdreTransit }} chargé — Client: {{ otCharge.client?.nom }}
                            </span>
                        </div>
                    </div>

                    <hr>

                    <form @submit.prevent="creerDossier" class="text-xs">
                        <div class="row g-4">
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Client <span class="text-danger">*</span></label>
                                <select v-model="dossier.client" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="c in clients" :key="c.id" :value="c">{{ c.nom }} {{ c.prenom }}
                                    </option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Type de Production <span
                                        class="text-danger">*</span></label>
                                <select v-model="dossier.typeProduction" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="p in typeProductions" :key="p.id" :value="p">{{ p.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Phase dossier <span
                                        class="text-danger">*</span></label>
                                <select v-model="dossier.phaseDossier" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="ph in phases" :key="ph.id" :value="ph">{{ ph.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Statut initial</label>
                                <select v-model="dossier.statutDossier" class="form-select form-select-sm">
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="s in statuts" :key="s.id" :value="s">{{ s.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Pays d'origine</label>
                                <select v-model="dossier.paysOrigine" class="form-select form-select-sm">
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="p in pays" :key="p.id" :value="p">{{ p.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Pays de destination</label>
                                <select v-model="dossier.paysDestination" class="form-select form-select-sm">
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="p in pays" :key="p.id" :value="p">{{ p.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Incoterm</label>
                                <select v-model="dossier.inconterme" class="form-select form-select-sm">
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="i in incontermes" :key="i.id" :value="i">{{ i.abreviation }} — {{
                                        i.nom }}</option>
                                </select>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Lieu de départ</label>
                                <input type="text" v-model="dossier.depart" class="form-control form-control-sm">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Lieu paiement fret</label>
                                <input type="text" v-model="dossier.lieuPaiementFret"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Pour compte de</label>
                                <input type="text" v-model="dossier.pourCompte" class="form-control form-control-sm"
                                    placeholder="ex: Société X">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Provision demandée (MGA)</label>
                                <input type="number" step="0.01" v-model="dossier.provisionDemandee"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Échéance</label>
                                <input type="datetime-local" v-model="dossier.echeance"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-4">
                                <label class="form-label fw-semibold">Date de livraison</label>
                                <input type="datetime-local" v-model="dossier.dateLivraison"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-8">
                                <label class="form-label fw-semibold">Adresse de livraison</label>
                                <input type="text" v-model="dossier.adresseLivraison"
                                    class="form-control form-control-sm">
                            </div>
                            <div class="col-md-4 d-flex flex-column gap-2 mt-3">
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="estGroupage"
                                        v-model="dossier.estGroupage">
                                    <label class="form-check-label" for="estGroupage">Groupage (LCL)</label>
                                </div>
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="estPrepaye"
                                        v-model="dossier.estPrepaye">
                                    <label class="form-check-label" for="estPrepaye">Fret prépayé</label>
                                </div>
                                <div class="form-check form-switch">
                                    <input class="form-check-input" type="checkbox" id="estProvisionne"
                                        v-model="dossier.estProvisionne">
                                    <label class="form-check-label" for="estProvisionne">Provisionné</label>
                                </div>
                            </div>
                        </div>

                        <div class="d-flex justify-content-end gap-3 mt-5 pt-3 border-top">
                            <router-link to="/dossiers" class="btn btn-secondary btn-sm">Annuler</router-link>
                            <button type="submit" class="btn btn-primary btn-sm" :disabled="enCours">
                                <span v-if="enCours" class="spinner-border spinner-border-sm me-2"></span>
                                Créer le Dossier
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
import Dossier from '../models/Dossier'
import DossierService from '../services/DossierService'
import ClientService from '../services/ClientService'
import OrdreTransitService from '../services/OrdreTransitService'
import UtilisateurService from '@/services/UtilisateurService.js'

const router = useRouter()
const enCours = ref(false)
const chargement = ref(true)
const chargementOt = ref(false)
const rechercheOt = ref('')
const otCharge = ref(null)

const dossier = ref(new Dossier())
const clients = ref([])
const typeProductions = ref([])
const phases = ref([])
const statuts = ref([])
const pays = ref([])
const incontermes = ref([])
const utilisateur = ref();

onMounted(async () => {
    try {
        utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
        // console.log('user: ', utilisateur.value)
        const [cls, prods, ph, st, inc, ps] = await Promise.all([
            ClientService.searchClients(0, 1000, 'nom', 'asc', {}),
            OrdreTransitService.getAllTypeProductions(),
            DossierService.getAllPhases(),
            DossierService.getAllStatuts(),
            DossierService.getAllIncontermes(),
            ClientService.getAllPays()
        ])
        clients.value = cls.content || []
        typeProductions.value = prods
        phases.value = ph
        statuts.value = st
        incontermes.value = inc
        pays.value = ps || []
    } catch (e) {
        console.error('Erreur chargement formulaire dossier', e)
    } finally {
        chargement.value = false
    }
})

async function preRemplirDepuisOt() {
    if (!rechercheOt.value) return
    chargementOt.value = true
    try {
        const result = await OrdreTransitService.searchOrdreTransits(0, 1, 'date_creation', 'desc', {
            numero: rechercheOt.value
        })
        if (result.content && result.content.length > 0) {
            const ot = result.content[0]

            const dossiersOt = await DossierService.searchDossiers(0, 1, 'date_creation', 'desc', {
                otId: ot.id
            })
            if (dossiersOt.content && dossiersOt.content.length > 0) {
                const existant = dossiersOt.content[0]
                alert(`Un dossier existe déjà pour cet OT (${existant.numeroDossier || existant.id}).`)
                otCharge.value = null
                dossier.value.ordreTransit = null
                return
            }

            otCharge.value = ot
            dossier.value.client = ot.client
            dossier.value.typeProduction = ot.typeProduction
            dossier.value.ordreTransit = ot
        } else {
            alert("Aucun OT trouvé pour ce numéro.")
        }
    } catch (e) {
        alert("Erreur technique lors de la recherche de l'OT.")
    } finally {
        chargementOt.value = false
    }
}

async function creerDossier() {
    const validation = dossier.value.valider()
    if (!validation.valide) {
        alert(validation.erreurs.join('\n'))
        return
    }
    enCours.value = true
        try {
        // Assurer que `dossier.agence` est un objet { id: ... } attendu par toJSON()
        if (utilisateur.value) {
            if (utilisateur.value.agence && typeof utilisateur.value.agence === 'object') {
                dossier.value.agence = utilisateur.value.agence
            } else {
                const agenceId = utilisateur.value.idAgence || utilisateur.value.agence
                if (agenceId) {
                    dossier.value.agence = { id: agenceId }
                }
            }
        }
        // Assurer que `dossier.utilisateur` contient l'id de l'utilisateur connecté
        if (utilisateur.value) {
            if (utilisateur.value.id) {
                dossier.value.utilisateur = { id: utilisateur.value.id }
            } else if (utilisateur.value.idUtilisateur) {
                dossier.value.utilisateur = { id: utilisateur.value.idUtilisateur }
            }
        }
        console.log('agence dossier: ', dossier.value.agence)
        console.log('agence utilisateur: ', utilisateur.value.agence)
        const result = await DossierService.createDossier(dossier.value.toJSON())
        alert(`Dossier ${result.numeroDossier} créé avec succès.`)
        router.push(`/dossiers/${result.id}`)
    } catch (e) {
        const msg = e?.response?.data
        alert(typeof msg === 'string' && msg ? msg : "Erreur lors de la création du dossier.")
    } finally {
        enCours.value = false
    }
}
</script>

<style scoped>
.text-xs {
    font-size: 0.75rem;
}
</style>

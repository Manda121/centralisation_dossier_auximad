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
                        Vous pouvez pré-remplir ce formulaire à partir d'un <strong>Ordre de Transit</strong>.
                        Tapez quelques chiffres : la liste des OT correspondants s'affiche si vous ne vous souvenez
                        plus du numéro exact. Un OT non validé devra être validé après confirmation. Sans OT sélectionné, un OT validé sera automatiquement créé et lié au dossier.
                    </div>
                    <div class="row g-3 mb-4 align-items-start">
                        <div class="col-md-4">
                            <label class="form-label text-xs fw-semibold">N° Ordre de Transit (pour pré-remplir)</label>
                            <AutocompleteRecherche v-model="rechercheOt" :fetch-results="suggestionsOt"
                                :label-fn="ot => ot.numeroOrdreTransit"
                                :sublabel-fn="ot => libelleClient(ot.client)"
                                :value-fn="ot => ot.numeroOrdreTransit"
                                placeholder="ex: 10001 (quelques chiffres suffisent)"
                                message-vide="Aucun OT ne correspond à cette recherche"
                                numerique @selection="utiliserOt" />
                            <button class="btn btn-outline-secondary btn-sm mt-2" :disabled="chargementOt"
                                @click="preRemplirDepuisOt">
                                <span v-if="chargementOt" class="spinner-border spinner-border-sm me-1"></span>
                                Charger OT
                            </button>
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
                            <div v-if="!otCharge" class="col-md-4">
                                <label class="form-label fw-semibold">Nature de la marchandise <span class="text-danger">*</span></label>
                                <select v-model="dossier.natureMarchandiseCreation" class="form-select form-select-sm" required>
                                    <option :value="null">Sélectionnez</option>
                                    <option v-for="nature in naturesMarchandise" :key="nature.id" :value="nature">{{ nature.nom }}</option>
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

                        <!-- Section Documents (lors de la création) -->
                        <div v-if="dossier.typeProduction && (dossier.ordreTransit?.natureMarchandise?.id || dossier.natureMarchandiseCreation?.id)"
                            class="mt-4 pt-3 border-top">
                            <h6 class="fw-semibold mb-3">Documents nécessaires</h6>
                            <div class="alert alert-info py-2 text-xs mb-3">
                                Cochez les documents déjà disponibles. Les documents obligatoires <span
                                    class="badge bg-danger-subtle text-danger border border-danger-subtle">obligatoire</span>
                                sont requis pour effectuer des mouvements. Vous pouvez créer le dossier sans eux.
                            </div>
                            <DocumentsSection :dossier-id="null" :type-production-id="dossier.typeProduction?.id"
                                :nature-marchandise-id="dossier.ordreTransit?.natureMarchandise?.id || dossier.natureMarchandiseCreation?.id"
                                :read-only="false" @docs-obligatoires-complets="docsObligatoiresComplets = $event"
                                @documents-valides="docsCreation = $event" ref="docsSectionRef" />
                            <div v-if="!docsObligatoiresComplets" class="alert alert-warning py-2 text-xs mt-2">
                                Le dossier peut être créé. Les mouvements seront bloqués tant que des documents obligatoires manquent.
                            </div>
                        </div>

                        <div class="d-flex justify-content-end gap-3 mt-5 pt-3 border-top">
                            <router-link to="/dossiers" class="btn btn-secondary btn-sm">Annuler</router-link>
                            <button type="submit" class="btn btn-primary btn-sm"
                                :disabled="enCours || chargementOt">
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
import DocumentsSection from '../components/DocumentsSection.vue'
import AutocompleteRecherche from '../components/inc/AutocompleteRecherche.vue'
import DocumentService from '../services/DocumentService'
import Dossier from '../models/Dossier'
import DossierService from '../services/DossierService'
import ClientService from '../services/ClientService'
import OrdreTransitService from '../services/OrdreTransitService'
import MarchandiseService from '../services/MarchandiseService'
import UtilisateurService from '@/services/UtilisateurService.js'
import { useNotification } from '../composables/useNotification'

const router = useRouter()
const route = useRoute()
const { notifySuccess, notifyError, notifyWarning, confirm } = useNotification()
const enCours = ref(false)
const chargement = ref(true)
const chargementOt = ref(false)
const rechercheOt = ref('')
const otCharge = ref(null)
const docsObligatoiresComplets = ref(true) // Par défaut true (pas de type de production sélectionné)
const docsCreation = ref({ typeIdsCoches: [], fichiersParTypeId: {} })
const docsSectionRef = ref(null)
const maintenantLocal = () => {
    const date = new Date()
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
    return date.toISOString().slice(0, 16)
}

const dossier = ref(new Dossier())
dossier.value.echeance = maintenantLocal()
dossier.value.dateLivraison = maintenantLocal()
const clients = ref([])
const typeProductions = ref([])
const naturesMarchandise = ref([])
const phases = ref([])
const statuts = ref([])
const pays = ref([])
const incontermes = ref([])
const utilisateur = ref();

onMounted(async () => {
    try {
        utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
        // console.log('user: ', utilisateur.value)
        const [cls, prods, ph, st, inc, ps, natures] = await Promise.all([
            ClientService.searchClients(0, 1000, 'nom', 'asc', {}),
            OrdreTransitService.getAllTypeProductions(),
            DossierService.getAllPhases(),
            DossierService.getAllStatuts(),
            DossierService.getAllIncontermes(),
            ClientService.getAllPays(),
            MarchandiseService.getNatureMarchandises()
        ])
        clients.value = cls.content || []
        typeProductions.value = prods
        phases.value = ph
        statuts.value = st
        incontermes.value = inc
        pays.value = ps || []
        naturesMarchandise.value = natures
        const otId = Number(route.query.otId)
        if (Number.isSafeInteger(otId) && otId > 0) {
            try {
                const ot = await OrdreTransitService.getOrdreTransitById(otId)
                rechercheOt.value = String(ot.numeroOrdreTransit)
                await utiliserOt(ot)
            } catch (e) {
                notifyError("Impossible de charger l'ordre de transit sélectionné.")
            }
        }
    } catch (e) {
        console.error('Erreur chargement formulaire dossier', e)
    } finally {
        chargement.value = false
    }
})

/** Suggestions d'OT pour l'autocomplétion (recherche partielle sur le numéro) */
async function suggestionsOt(q) {
    try {
        return await OrdreTransitService.suggestions(q)
    } catch (e) {
        console.error('Erreur recherche suggestions OT', e)
        return []
    }
}

function libelleClient(client) {
    if (!client) return ''
    return `${client.nom || ''} ${client.prenom || ''}`.trim()
}

/** Applique un OT (choisi dans les suggestions ou trouvé par recherche exacte) au formulaire */
async function utiliserOt(ot) {
    if (!ot) return
    chargementOt.value = true
    try {
        const dossiersOt = await DossierService.searchDossiers(0, 1, 'date_creation', 'desc', {
            otId: ot.id
        })
        if (dossiersOt.content && dossiersOt.content.length > 0) {
            const existant = dossiersOt.content[0]
            notifyWarning(`Un dossier existe déjà pour cet OT (${existant.numeroDossier || existant.id}).`)
            otCharge.value = null
            dossier.value.ordreTransit = null
            return
        }

        otCharge.value = ot
        dossier.value.client = ot.client
        dossier.value.typeProduction = ot.typeProduction
        dossier.value.natureMarchandiseCreation = null
        dossier.value.ordreTransit = ot
    } catch (e) {
        notifyError("Erreur technique lors du chargement de l'OT.")
    } finally {
        chargementOt.value = false
    }
}

/** Recherche exacte par numéro (bouton « Charger OT ») */
async function preRemplirDepuisOt() {
    const numero = String(rechercheOt.value || '').trim()
    if (!numero) return
    if (!/^\d+$/.test(numero)) {
        notifyWarning('Le numéro d’OT ne doit contenir que des chiffres.')
        return
    }
    chargementOt.value = true
    try {
        const result = await OrdreTransitService.searchOrdreTransits(0, 1, 'date_creation', 'desc', { numero })
        if (result.content && result.content.length > 0) {
            await utiliserOt(result.content[0])
        } else {
            notifyWarning("Aucun OT trouvé pour ce numéro.")
        }
    } catch (e) {
        notifyError("Erreur technique lors de la recherche de l'OT.")
    } finally {
        chargementOt.value = false
    }
}

async function creerDossier() {
    const validation = dossier.value.valider()
    if (!validation.valide) {
        notifyWarning(validation.erreurs.join('\n'))
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
        if (dossier.value.ordreTransit) {
            const ot = await OrdreTransitService.getOrdreTransitById(dossier.value.ordreTransit.id)
            if (ot.statutOt?.id !== 3) {
                const ok = await confirm(`L'OT ${ot.numeroOrdreTransit} n'est pas validé. Confirmez-vous sa validation pour créer le dossier ?`)
                if (!ok) { enCours.value = false; return }
                const valide = await OrdreTransitService.validateOrdreTransit(ot.id)
                dossier.value.ordreTransit = valide
                otCharge.value = valide
            }
        }
        const result = await DossierService.createDossier(dossier.value.toJSON())

        // Traiter les documents sélectionnés lors de la création.
        // On lit l'état courant du composant (l'utilisateur n'est pas obligé de
        // passer par « Valider les documents » pour que sa sélection soit prise en compte).
        const section = docsSectionRef.value
        const typeIdsCoches = section ? Array.from(section.coches) : (docsCreation.value.typeIdsCoches || [])
        const fichiersParTypeId = section ? { ...section.fichiersTemp } : (docsCreation.value.fichiersParTypeId || {})
        const dateRemise = section?.dateRemiseBatch || docsCreation.value.dateRemise || null
        const datesRemiseParType = section ? { ...section.datesRemiseSaisies } : (docsCreation.value.datesRemiseParType || {})

        let docsEnregistres = 0
        if (typeIdsCoches.length > 0) {
            try {
                await DocumentService.processerBatch(result.id, typeIdsCoches, [], fichiersParTypeId, dateRemise)
                // Dates de remise saisies document par document (elles priment sur la date commune)
                for (const [typeId, date] of Object.entries(datesRemiseParType)) {
                    if (!date) continue
                    await DocumentService.assignerDocument(result.id, Number(typeId), date)
                }
                docsEnregistres = typeIdsCoches.length
            } catch (e) {
                console.error('Documents non sauvegardés :', e)
                notifyWarning(`Dossier ${result.numeroDossier} créé, mais les documents n'ont pas pu être enregistrés.`)
            }
        }

        notifySuccess(`Dossier ${result.numeroDossier} créé avec succès.` +
            (docsEnregistres ? ` ${docsEnregistres} document(s) rattaché(s).` : ''))
        router.push(`/dossiers/${result.id}`)
    } catch (e) {
        const msg = e?.response?.data
        notifyError(typeof msg === 'string' && msg ? msg : "Erreur lors de la création du dossier.")
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

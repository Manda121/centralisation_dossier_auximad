<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import DemandeInteragenceService from '../services/DemandeInteragenceService'
import UtilisateurService from '../services/UtilisateurService'
import AgenceService from '../services/AgenceService'

const router = useRouter()
const datatableRef = ref(null)
const agences = ref([])

onMounted(async () => {
    try {
        agences.value = await AgenceService.getAllAgences()
    } catch (e) {
        console.error("Impossible de charger les agences", e)
    }
})

// Obtenir la date du jour au format YYYY-MM-DD
const aujourdhui = new Date().toISOString().split('T')[0]

const filters = reactive({
    query: '',
    statut: '',
    dateMin: aujourdhui,
    dateMax: aujourdhui,
    agenceDemandeuseId: '',
    agenceDestinataireId: ''
})

const selectedDemande = ref(null)
const showDetailModal = ref(false)
const reponseMotif = ref('')
const submittingReponse = ref(false)
const messageAction = ref(null)
const erreurAction = ref(null)

const modeEdition = ref(false)
const editForm = reactive({
    idAgenceDestinataire: '',
    motif: ''
})
const submittingEdit = ref(false)

const userConnecte = computed(() => UtilisateurService.obtenirUtilisateurConnecte())

const columns = [
    { key: 'dossier.numeroDossier', label: 'N° Dossier', sortable: true },
    { key: 'direction', label: 'Rôle', sortable: false },
    { key: 'agenceDemandeuse.nom', label: 'Agence Émettrice', sortable: false },
    { key: 'agenceDestinataire.nom', label: 'Agence Cible', sortable: false },
    { key: 'motif', label: 'Motif', sortable: false },
    { key: 'statut', label: 'Statut', sortable: true },
    { key: 'dateDemande', label: 'Date Demande', sortable: true }
]

async function chargerDemandes(page, size, sortBy, sortDir, filters) {
    return await DemandeInteragenceService.searchDemandes(page, size, sortBy, sortDir, filters)
}

function reinitialiserFiltres() {
    filters.query = ''
    filters.statut = ''
    filters.dateMin = ''
    filters.dateMax = ''
    filters.agenceDemandeuseId = ''
    filters.agenceDestinataireId = ''
    datatableRef.value?.refresh(true)
}

function appliquerAujourdhui() {
    filters.dateMin = aujourdhui
    filters.dateMax = aujourdhui
    datatableRef.value?.refresh(true)
}

async function ouvrirDetail(row) {
    selectedDemande.value = await DemandeInteragenceService.getDemandeById(row.id)
    reponseMotif.value = ''
    erreurAction.value = null
    messageAction.value = null
    modeEdition.value = false
    showDetailModal.value = true
}

async function traiterDemande(acceptee) {
    if (!selectedDemande.value) return 
    submittingReponse.value = true
    erreurAction.value = null
    messageAction.value = null

    try {
        const payload = {
            acceptee: acceptee,
            motifReponse: reponseMotif.value || null
        }
        const updated = await DemandeInteragenceService.repondreDemande(selectedDemande.value.id, payload)
        selectedDemande.value = updated
        messageAction.value = acceptee ? "Demande acceptée avec succès !" : "Demande refusée."
        datatableRef.value?.refresh()
    } catch (e) {
        erreurAction.value = e.response?.data || "Erreur lors du traitement de la demande."
    } finally {
        submittingReponse.value = false
    }
}

function estAgenceDemandeuse(demande) {
    if (!userConnecte.value || !userConnecte.value.idAgence) return false
    return userConnecte.value.idAgence === demande?.agenceDemandeuse?.id
}

function estAgenceDestinataire(demande) {
    console.log('test: ', userConnecte.value);
    if (!userConnecte.value || !userConnecte.value.idAgence) return false
    return userConnecte.value.idAgence === demande?.agenceDestinataire?.id
}

function peutModifier(demande) {
    return estAgenceDemandeuse(demande) && demande?.statut === 'EN_ATTENTE'
}

function meFormaterAgencesEdition() {
    if (!selectedDemande.value) return []
    return agences.value.filter(a => a.id !== selectedDemande.value.agenceDemandeuse?.id)
}

function activerEdition() {
    if (!selectedDemande.value) return
    editForm.idAgenceDestinataire = selectedDemande.value.agenceDestinataire?.id || ''
    editForm.motif = selectedDemande.value.motif || ''
    modeEdition.value = true
    erreurAction.value = null
    messageAction.value = null
}

async function enregistrerModification() {
    if (!selectedDemande.value) return
    submittingEdit.value = true
    erreurAction.value = null
    messageAction.value = null
    try {
        const payload = {
            idAgenceDestinataire: parseInt(editForm.idAgenceDestinataire),
            motif: editForm.motif || null
        }
        const updated = await DemandeInteragenceService.modifierDemande(selectedDemande.value.id, payload)
        selectedDemande.value = updated
        modeEdition.value = false
        messageAction.value = "Demande modifiée avec succès !"
        datatableRef.value?.refresh()
    } catch (e) {
        erreurAction.value = e.response?.data || "Erreur lors de la modification de la demande."
    } finally {
        submittingEdit.value = false
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
                <!-- En-tête de page -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Demandes Inter-Agences</h1>
                        <p class="text-secondary mb-0 small">Suivi et traitement des demandes de délégation / partage
                            inter-agences.</p>
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-outline-secondary btn-sm" @click="reinitialiserFiltres">
                            🔄 Tous les filtres
                        </button>
                        <button class="btn btn-primary btn-sm" @click="appliquerAujourdhui">
                            📅 Demandes du jour
                        </button>
                    </div>
                </div>

                <!-- Carte principale -->
                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerDemandes"
                            :filters="filters" default-sort="dateDemande" default-sort-dir="desc" :pageSize="15"
                            row-clickable @row-click="ouvrirDetail">

                            <template #filters="{ filters: slotFilters }">
                                <div class="row g-2">
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Date début</label>
                                        <input type="date" v-model="slotFilters.dateMin"
                                            class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Date fin</label>
                                        <input type="date" v-model="slotFilters.dateMax"
                                            class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Statut</label>
                                        <select v-model="slotFilters.statut" class="form-select form-select-sm">
                                            <option value="">Tous les statuts</option>
                                            <option value="EN_ATTENTE">En attente</option>
                                            <option value="ACCEPTEE">Acceptée</option>
                                            <option value="REFUSEE">Refusée</option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Agence
                                            Émettrice</label>
                                        <select v-model="slotFilters.agenceDemandeuseId"
                                            class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="a in agences" :key="a.id" :value="a.id">{{ a.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Agence Cible</label>
                                        <select v-model="slotFilters.agenceDestinataireId"
                                            class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="a in agences" :key="a.id" :value="a.id">{{ a.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Recherche</label>
                                        <input type="text" v-model="slotFilters.query"
                                            class="form-control form-control-sm"
                                            placeholder="N° dossier, client, motif…">
                                    </div>
                                </div>
                            </template>

                            <template #cell-dossier.numeroDossier="{ row }">
                                <div class="d-flex align-items-center gap-2">
                                    <span v-if="!row.estVue && estAgenceDestinataire(row)"
                                        class="badge bg-danger rounded-circle p-1" title="Nouvelle demande non vue">
                                    </span>
                                    <span class="fw-semibold text-primary">
                                        {{ row.dossier?.numeroDossier }}
                                    </span>
                                </div>
                            </template>

                            <template #cell-direction="{ row }">
                                <span v-if="estAgenceDemandeuse(row)"
                                    class="badge bg-primary-subtle text-primary border">
                                    📤 Notre demande
                                </span>
                                <span v-else-if="estAgenceDestinataire(row)"
                                    class="badge bg-info-subtle text-dark border">
                                    📥 Adressée à nous
                                </span>
                                <span v-else class="badge bg-light text-muted border">
                                    🌐 Autre agence
                                </span>
                            </template>

                            <template #cell-motif="{ row }">
                                <span class="text-truncate d-inline-block" style="max-width: 220px;" :title="row.motif">
                                    {{ row.motif || '—' }}
                                </span>
                            </template>

                            <template #cell-statut="{ row }">
                                <span v-if="row.statut === 'EN_ATTENTE'" class="badge bg-warning text-dark">
                                    ⏳ En attente
                                </span>
                                <span v-else-if="row.statut === 'ACCEPTEE'" class="badge bg-success text-white">
                                    ✓ Acceptée
                                </span>
                                <span v-else-if="row.statut === 'REFUSEE'" class="badge bg-danger text-white">
                                    ✕ Refusée
                                </span>
                                <span v-else class="badge bg-secondary">{{ row.statut }}</span>
                            </template>

                            <template #cell-dateDemande="{ row }">
                                {{ formatarDate(row.dateDemande) }}
                            </template>
                        </DataTable>
                    </div>
                </div>
            </div>
        </main>

        <!-- Modal de détail / édition / traitement -->
        <div v-if="showDetailModal" class="modal fade show d-block tab-modal-backdrop" tabindex="-1"
            style="background: rgba(0,0,0,0.5)">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow">
                    <div class="modal-header bg-light py-3">
                        <h5 class="modal-title h6 fw-bold text-dark mb-0">
                            Détails de la demande d'inter-agence
                        </h5>
                        <button type="button" class="btn-close" @click="showDetailModal = false"></button>
                    </div>
                    <div class="modal-body p-4" v-if="selectedDemande">
                        <!-- Alertes -->
                        <div v-if="messageAction" class="alert alert-success py-2 mb-3">{{ messageAction }}</div>
                        <div v-if="erreurAction" class="alert alert-danger py-2 mb-3">{{ erreurAction }}</div>

                        <!-- Card Dossier & Agences -->
                        <div class="row g-3 small mb-4">
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded border">
                                    <div class="text-secondary mb-1">DOSSIER MÈRE CONCERNÉ</div>
                                    <router-link :to="`/dossiers/${selectedDemande.dossier?.id}`"
                                        class="h6 fw-bold text-primary text-decoration-none">
                                        {{ selectedDemande.dossier?.numeroDossier }}
                                    </router-link>
                                    <div class="text-muted mt-1">Client : {{ selectedDemande.dossier?.client?.nom }} {{
                                        selectedDemande.dossier?.client?.prenom || '' }}</div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded border">
                                    <div class="text-secondary mb-1">STATUT DE LA DEMANDE</div>
                                    <span v-if="selectedDemande.statut === 'EN_ATTENTE'"
                                        class="badge bg-warning text-dark fs-6">
                                        ⏳ En attente
                                    </span>
                                    <span v-else-if="selectedDemande.statut === 'ACCEPTEE'"
                                        class="badge bg-success text-white fs-6">
                                        ✓ Acceptée
                                    </span>
                                    <span v-else-if="selectedDemande.statut === 'REFUSEE'"
                                        class="badge bg-danger text-white fs-6">
                                        ✕ Refusée
                                    </span>
                                    <div class="text-muted mt-2">Demandé le : {{
                                        formatarDate(selectedDemande.dateDemande) }}
                                    </div>
                                </div>
                            </div>

                            <div class="col-md-6">
                                <div class="text-secondary">AGENCE ÉMETTRICE (DEMANDEUSE)</div>
                                <div class="fw-semibold text-dark">{{ selectedDemande.agenceDemandeuse?.nom }} ({{
                                    selectedDemande.agenceDemandeuse?.ville }})</div>
                                <div class="text-muted">Par : {{ selectedDemande.utilisateurDemande?.nomComplet ||
                                    selectedDemande.utilisateurDemande?.nomUtilisateur }}</div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">AGENCE CIBLE (DESTINATAIRE)</div>
                                <div class="fw-semibold text-dark">{{ selectedDemande.agenceDestinataire?.nom }} ({{
                                    selectedDemande.agenceDestinataire?.ville }})</div>
                            </div>

                            <div class="col-12" v-if="selectedDemande.motif && !modeEdition">
                                <div class="text-secondary">MOTIF / DESCRIPTION DE LA DEMANDE</div>
                                <div class="p-2 bg-white border rounded text-dark mt-1">{{ selectedDemande.motif }}
                                </div>
                            </div>

                            <!-- Réponse de validation si traitée -->
                            <div class="col-12" v-if="selectedDemande.statut !== 'EN_ATTENTE'">
                                <div class="p-3 bg-light rounded border border-secondary-subtle">
                                    <div class="text-secondary mb-1">TRAITEMENT DE LA DEMANDE</div>
                                    <div><strong>Traité par :</strong> {{ selectedDemande.utilisateurReponse?.nomComplet
                                        ||
                                        selectedDemande.utilisateurReponse?.nomUtilisateur || 'Système' }}</div>
                                    <div><strong>Date réponse :</strong> {{ formatarDate(selectedDemande.dateReponse) }}
                                    </div>
                                    <div v-if="selectedDemande.reponseMotif" class="mt-2">
                                        <strong>Commentaire :</strong> {{ selectedDemande.reponseMotif }}
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Formulaire de modification pour l'agence émettrice -->
                        <div v-if="peutModifier(selectedDemande)" class="p-3 border rounded bg-light mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <h6 class="fw-bold text-dark mb-0">Gestion de votre demande</h6>
                                <button v-if="!modeEdition" type="button" class="btn btn-outline-primary btn-sm"
                                    @click="activerEdition">
                                    ✏️ Modifier la demande
                                </button>
                            </div>

                            <div v-if="modeEdition" class="mt-3">
                                <div class="mb-3">
                                    <label class="form-label small text-secondary fw-semibold">Changer l'agence
                                        cible</label>
                                    <select v-model="editForm.idAgenceDestinataire" class="form-select form-select-sm">
                                        <option v-for="a in meFormaterAgencesEdition()" :key="a.id" :value="a.id">
                                            {{ a.nom }} ({{ a.ville }})
                                        </option>
                                    </select>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label small text-secondary fw-semibold">Motif de la
                                        demande</label>
                                    <textarea v-model="editForm.motif" class="form-control form-control-sm" rows="3"
                                        placeholder="Explication ou motif…"></textarea>
                                </div>
                                <div class="d-flex gap-2 justify-content-end">
                                    <button type="button" class="btn btn-outline-secondary btn-sm"
                                        @click="modeEdition = false">
                                        Annuler
                                    </button>
                                    <button type="button" class="btn btn-primary btn-sm" :disabled="submittingEdit"
                                        @click="enregistrerModification">
                                        💾 Enregistrer les modifications
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- Formulaire de réponse pour l'agence destinataire -->
                        <div v-if="selectedDemande.statut === 'EN_ATTENTE' && estAgenceDestinataire(selectedDemande)"
                            class="p-3 border rounded bg-warning-subtle mb-3">
                            <h6 class="fw-bold text-dark mb-2">Décision pour votre agence</h6>
                            <div class="mb-3">
                                <label class="form-label small text-secondary">Commentaire / Remarque
                                    (optionnel)</label>
                                <textarea v-model="reponseMotif" class="form-control form-control-sm" rows="2"
                                    placeholder="Ajouter une remarque…"></textarea>
                            </div>
                            <div class="d-flex gap-2 justify-content-end">
                                <button type="button" class="btn btn-outline-danger btn-sm"
                                    :disabled="submittingReponse" @click="traiterDemande(false)">
                                    ✕ Refuser la demande
                                </button>
                                <button type="button" class="btn btn-success btn-sm text-white"
                                    :disabled="submittingReponse" @click="traiterDemande(true)">
                                    ✓ Accepter &amp; Créer Sous-dossier
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer bg-light py-2">
                        <button type="button" class="btn btn-secondary btn-sm"
                            @click="showDetailModal = false">Fermer</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.bg-warning-subtle {
    background-color: #fff8e6;
}

.bg-primary-subtle {
    background-color: #e7f1ff;
}

.bg-info-subtle {
    background-color: #e0f7fa;
}
</style>

<template>
    <!-- ======================== Section Documents ======================== -->
    <div class="documents-section">
        <div v-if="chargement" class="d-flex align-items-center gap-2 py-3 text-secondary small">
            <span class="spinner-border spinner-border-sm"></span>
            Chargement des documents…
        </div>

        <div v-else>
            <!-- ═══ Date de remise commune pour l'assignation ═══ -->
            <div v-if="!readOnly" class="d-flex flex-wrap align-items-center gap-2 mb-3 p-2 rounded batch-bar">
                <label class="fw-semibold small mb-0">Date de remise commune :</label>
                <input type="datetime-local" v-model="dateRemiseBatch" class="form-control form-control-sm"
                    style="max-width:220px;" />
                <span v-if="!dateRemiseBatch" class="text-secondary" style="font-size:0.72rem;">
                    (date actuelle par défaut)
                </span>
            </div>

            <!-- Liste des types de documents avec checkboxes -->
            <div v-for="typeDoc in typeDocuments" :key="typeDoc.id" class="doc-item mb-2"
                :class="{ 'doc-item--assigned': estCoche(typeDoc.id) }">
                <div class="d-flex align-items-start gap-3">
                    <!-- Checkbox -->
                    <div class="form-check mt-1">
                        <input class="form-check-input doc-checkbox" type="checkbox" :id="`doc-${typeDoc.id}`"
                            :checked="estCoche(typeDoc.id)" @change="onCheckChange(typeDoc.id, $event)"
                            :disabled="readOnly" />
                    </div>

                    <!-- Infos document -->
                    <div class="flex-grow-1">
                        <label :for="`doc-${typeDoc.id}`"
                            class="form-check-label fw-semibold d-flex align-items-center gap-2 cursor-pointer">
                            {{ typeDoc.nom }}
                            <span v-if="typeDoc.estObligatoire"
                                class="badge bg-danger-subtle text-danger border border-danger-subtle"
                                style="font-size:0.65rem;">obligatoire</span>
                        </label>
                        <div v-if="typeDoc.description" class="text-secondary" style="font-size:0.75rem;">{{
                            typeDoc.description }}</div>

                        <!-- Date de remise (table remise) : saisie pour un doc non remis, badge sinon -->
                        <div v-if="!readOnly && !estCoche(typeDoc.id)" class="mt-1 d-flex align-items-center gap-2">
                            <label class="text-secondary" style="font-size:0.72rem;"
                                :for="`remise-${typeDoc.id}`">Remise :</label>
                            <input type="datetime-local" :id="`remise-${typeDoc.id}`"
                                v-model="datesRemiseSaisies[typeDoc.id]" class="form-control form-control-sm"
                                style="max-width:200px;font-size:0.75rem;" />
                        </div>
                        <div v-else-if="getDateRemise(typeDoc.id)" class="mt-1 d-flex align-items-center gap-2">
                            <span class="badge bg-primary-subtle text-primary" style="font-size:0.68rem;">
                                📌 Remise : {{ formatarDate(getDateRemise(typeDoc.id)) }}
                            </span>
                            <!-- Modifier la date de remise (hors lecture seule) -->
                            <span v-if="!readOnly" class="dropdown">
                                <button class="btn btn-link btn-sm p-0 text-secondary dropdown-toggle"
                                    style="font-size:0.72rem;" type="button" data-bs-toggle="dropdown"
                                    aria-expanded="false">modifier</button>
                                <div class="dropdown-menu p-2" style="min-width:230px;">
                                    <label class="form-label mb-1" style="font-size:0.72rem;">Nouvelle date de
                                        remise</label>
                                    <input type="datetime-local" class="form-control form-control-sm mb-2"
                                        v-model="nouvelleDateRemise" />
                                    <button class="btn btn-primary btn-sm w-100"
                                        @click="changerDateRemise(typeDoc.id)">Enregistrer</button>
                                    <button v-if="!readOnly" class="btn btn-link btn-sm w-100 text-danger mt-1"
                                        @click="retirerDateRemise(typeDoc.id)">Retirer la date de remise</button>
                                </div>
                            </span>
                        </div>

                        <!-- Aperçu du fichier existant (mode lecture ou remplissage) -->
                        <div v-if="getDocumentActif(typeDoc.id)" class="mt-1 d-flex align-items-center gap-2">
                            <span class="text-success" style="font-size:0.75rem;">
                                <svg viewBox="0 0 16 16" fill="currentColor" width="12" height="12">
                                    <path
                                        d="M10.97 4.97a.75.75 0 0 1 1.07 1.05l-3.99 4.99a.75.75 0 0 1-1.08.02L4.324 8.384a.75.75 0 0 1-1.06-1.06l2.094 2.093 3.473-4.425z" />
                                </svg>
                                Remis
                            </span>
                            <!-- Aperçu miniature fichier -->
                            <button v-if="getDocumentActif(typeDoc.id).nomFichier" type="button"
                                class="btn btn-link btn-sm p-0 doc-preview-btn"
                                @click="ouvrirApercu(getDocumentActif(typeDoc.id))"
                                :title="getDocumentActif(typeDoc.id).nomFichier">
                                <div class="doc-thumbnail">
                                    <img v-if="estImage(getDocumentActif(typeDoc.id).nomFichier)"
                                        :src="getFichierUrl(getDocumentActif(typeDoc.id).id)" class="doc-thumbnail__img"
                                        :alt="getDocumentActif(typeDoc.id).nomFichier" />
                                    <div v-else class="doc-thumbnail__icon">
                                        <svg viewBox="0 0 16 16" fill="currentColor" width="20" height="20">
                                            <path
                                                d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z" />
                                        </svg>
                                    </div>
                                    <span class="doc-thumbnail__name">{{ getDocumentActif(typeDoc.id).nomFichier
                                        }}</span>
                                </div>
                            </button>
                        </div>
                    </div>

                    <!-- Actions par document : Upload fichier (si case cochée) -->
                    <div v-if="!readOnly && estCoche(typeDoc.id)" class="ms-auto d-flex align-items-center gap-2">
                        <label class="btn btn-outline-secondary btn-sm" :for="`upload-${typeDoc.id}`"
                            style="cursor:pointer;white-space:nowrap;">
                            <svg viewBox="0 0 16 16" fill="currentColor" width="12" height="12" class="me-1">
                                <path
                                    d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5" />
                                <path
                                    d="M7.646 1.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 2.707V11.5a.5.5 0 0 1-1 0V2.707L5.354 4.854a.5.5 0 1 1-.708-.708z" />
                            </svg>
                            {{ fichiersTemp[typeDoc.id] ? fichiersTemp[typeDoc.id].name : 'Fichier (opt.)' }}
                        </label>
                        <input type="file" :id="`upload-${typeDoc.id}`" class="d-none"
                            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.gif"
                            @change="onFichierChange(typeDoc.id, $event)" />
                    </div>
                </div>
            </div>

            <div v-if="typeDocuments.length === 0" class="text-secondary small fst-italic">
                Aucun type de document configuré pour ce type de production.
            </div>

            <!-- Bouton unique d'assignation/validation (en bas) -->
            <div v-if="!readOnly" class="mt-4 d-flex justify-content-end gap-2">
                <button type="button" class="btn btn-primary" @click="valider" :disabled="enCoursValidation">
                    <span v-if="enCoursValidation" class="spinner-border spinner-border-sm me-1"></span>
                    <span v-if="dossierId">Assigner tout</span>
                    <span v-else>Valider les documents</span>
                </button>
            </div>
        </div>
    </div>

    <!-- Modal aperçu fichier -->
    <div v-if="apercuDoc" class="modal fade show d-block" tabindex="-1"
        style="background: rgba(0,0,0,0.7); z-index:2000;">
        <div class="modal-dialog modal-lg modal-dialog-centered">
            <div class="modal-content border-0 shadow" style="max-height:90vh;overflow:hidden;">
                <div class="modal-header bg-dark text-white py-2">
                    <h6 class="modal-title mb-0">{{ apercuDoc.nomFichier }}</h6>
                    <button type="button" class="btn-close btn-close-white" @click="apercuDoc = null"></button>
                </div>
                <div class="modal-body p-0" style="overflow:auto;max-height:80vh;">
                    <img v-if="estImage(apercuDoc.nomFichier)" :src="getFichierUrl(apercuDoc.id)" class="w-100"
                        :alt="apercuDoc.nomFichier" />
                    <iframe v-else-if="estPdf(apercuDoc.nomFichier)" :src="getFichierUrl(apercuDoc.id)" width="100%"
                        height="600" frameborder="0"></iframe>
                    <div v-else class="p-4 text-center">
                        <a :href="getFichierUrl(apercuDoc.id)" class="btn btn-primary" download>
                            <svg viewBox="0 0 16 16" fill="currentColor" width="16" height="16" class="me-1">
                                <path
                                    d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5" />
                                <path
                                    d="M7.646 11.854a.5.5 0 0 0 .708 0l3-3a.5.5 0 0 0-.708-.708L8.5 10.293V1.5a.5.5 0 0 0-1 0v8.793L5.354 8.146a.5.5 0 1 1-.708.708z" />
                            </svg>
                            Télécharger le fichier
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Popup confirmation retrait -->
    <div v-if="popupRetrait.visible" class="modal fade show d-block" tabindex="-1"
        style="background: rgba(0,0,0,0.5); z-index:2100;">
        <div class="modal-dialog modal-dialog-centered modal-sm">
            <div class="modal-content border-0 shadow">
                <div class="modal-header bg-warning-subtle py-3">
                    <h6 class="modal-title fw-bold mb-0">Confirmer le retrait</h6>
                </div>
                <div class="modal-body small">
                    <p>Vous avez retiré <strong>{{ popupRetrait.nomDocument }}</strong>.</p>
                    <p class="mb-0">Voulez-vous vraiment le supprimer du dossier ?</p>
                </div>
                <div class="modal-footer gap-2 py-2">
                    <button class="btn btn-outline-secondary btn-sm" @click="annulerRetrait">Non, annuler</button>
                    <button class="btn btn-danger btn-sm" @click="confirmerRetrait">Oui, retirer</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import DocumentService from '@/services/DocumentService'

const props = defineProps({
    dossierId: { type: Number, default: null },
    typeProductionId: { type: Number, default: null },
    readOnly: { type: Boolean, default: false },
    /** Pour le mode création : on passe les docs initiaux via le parent */
    documentsInitiaux: { type: Array, default: () => [] }
})

const emit = defineEmits(['documents-valides', 'docs-obligatoires-complets'])

const chargement = ref(true)
const enCoursValidation = ref(false)
const typeDocuments = ref([])
const documentsActifs = ref([]) // Documents déjà remis
const coches = ref(new Set())     // IDs des typeDocument cochés
const fichiersTemp = ref({})      // { [typeDocumentId]: File }
const apercuDoc = ref(null)
const remises = ref({})           // { [typeDocumentId]: dateRemise } (table remise)

// Assignation par document
const datesRemiseSaisies = ref({})   // dates saisies pour les docs non remis
const dateRemiseBatch = ref('')
const nouvelleDateRemise = ref('')

// Popup confirmation retrait
const popupRetrait = ref({ visible: false, typeDocId: null, nomDocument: '', pendingUncheck: null })

const cochesSelectionnes = computed(() => {
    const remis = new Set(documentsActifs.value.map(d => d.typeDocument?.id))
    return Array.from(coches.value).filter(id => !remis.has(id))
})

onMounted(async () => {
    await chargerDonnees()
})

watch(() => props.dossierId, async () => {
    await chargerDonnees()
})

watch(() => props.typeProductionId, async () => {
    await chargerDonnees()
})

async function chargerDonnees() {
    chargement.value = true
    try {
        // Charger les types de documents
        typeDocuments.value = await DocumentService.getTypeDocuments(props.typeProductionId)

        // Charger les documents déjà remis (si dossier existant)
        if (props.dossierId) {
            documentsActifs.value = await DocumentService.getDocumentsByDossier(props.dossierId)
            // Charger les remises (table remise)
            try {
                const remisesList = await DocumentService.getRemises(props.dossierId)
                const map = {}
                remisesList.forEach(r => {
                    if (r.typeDocument?.id != null) map[r.typeDocument.id] = r.dateRemise
                })
                remises.value = map
            } catch (e) {
                console.warn('Erreur chargement remises', e)
                remises.value = {}
            }
        } else {
            documentsActifs.value = [...props.documentsInitiaux]
            remises.value = {}
        }

        // Initialiser les cases cochées
        const cochesSet = new Set()
        documentsActifs.value.forEach(doc => {
            cochesSet.add(doc.typeDocument?.id)
        })
        coches.value = cochesSet

        emitObligatoiresComplets()
    } catch (e) {
        console.error('Erreur chargement documents', e)
    } finally {
        chargement.value = false
    }
}

function estCoche(typeDocId) {
    return coches.value.has(typeDocId)
}

function getDocumentActif(typeDocId) {
    // Retourne le dernier document correspondant au type demandé.
    for (let i = documentsActifs.value.length - 1; i >= 0; i -= 1) {
        const doc = documentsActifs.value[i]
        if (doc?.typeDocument?.id === typeDocId) {
            return doc
        }
    }
    return null
}

function getDateRemise(typeDocId) {
    return remises.value[typeDocId] || null
}

function formatarDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    })
}

function getFichierUrl(docId) {
    return `http://localhost:8080/api/documents/${docId}/fichier`
}

function estImage(nomFichier) {
    if (!nomFichier) return false
    return /\.(jpg|jpeg|png|gif|webp)$/i.test(nomFichier)
}

function estPdf(nomFichier) {
    if (!nomFichier) return false
    return /\.pdf$/i.test(nomFichier)
}

function ouvrirApercu(doc) {
    apercuDoc.value = doc
}

function onFichierChange(typeDocId, event) {
    const file = event.target.files[0]
    if (file) {
        fichiersTemp.value = { ...fichiersTemp.value, [typeDocId]: file }
    }
}

function onCheckChange(typeDocId, event) {
    const checked = event.target.checked
    if (!checked) {
        // On décoche : demander confirmation si document déjà remis
        const docExistant = getDocumentActif(typeDocId)
        if (docExistant) {
            const typeDoc = typeDocuments.value.find(t => t.id === typeDocId)
            popupRetrait.value = {
                visible: true,
                typeDocId,
                nomDocument: typeDoc?.nom || 'ce document',
                pendingUncheck: () => {
                    const newSet = new Set(coches.value)
                    newSet.delete(typeDocId)
                    coches.value = newSet
                    retirer(typeDocId)
                }
            }
            // Remettre temporairement coché (le popup gère la décision)
            event.target.checked = true
            return
        }
        const newSet = new Set(coches.value)
        newSet.delete(typeDocId)
        coches.value = newSet
    } else {
        const newSet = new Set(coches.value)
        newSet.add(typeDocId)
        coches.value = newSet
    }
    emitObligatoiresComplets()
}

/** Assigner un seul document avec sa date de remise (table remise) */
async function assignerUn(typeDocId) {
    if (!props.dossierId) {
        // Mode création : ajouter simplement à la sélection locale
        const newSet = new Set(coches.value)
        newSet.add(typeDocId)
        coches.value = newSet
        emitObligatoiresComplets()
        return
    }

    assignationEnCours.value = { ...assignationEnCours.value, [typeDocId]: true }
    try {
        const dateRemise = datesRemiseSaisies.value[typeDocId] || dateRemiseBatch.value || null
        await DocumentService.assignerDocument(props.dossierId, typeDocId, dateRemise || null)
        // Uploader le fichier choisi pour ce document, s'il y en a un
        const fichier = fichiersTemp.value[typeDocId]
        if (fichier) {
            await DocumentService.saveDocument(props.dossierId, typeDocId, fichier)
            const restants = { ...fichiersTemp.value }
            delete restants[typeDocId]
            fichiersTemp.value = restants
        }
        await chargerDonnees()
        emit('documents-valides', { succes: true })
    } catch (e) {
        console.error('Erreur assignation document', e)
        alert(e?.response?.data || 'Erreur lors de l\'assignation du document.')
    } finally {
        assignationEnCours.value = { ...assignationEnCours.value, [typeDocId]: false }
        delete datesRemiseSaisies.value[typeDocId]
    }
}

/** Modifier la date de remise d'un document assigné (table remise) */
async function changerDateRemise(typeDocId) {
    try {
        await DocumentService.updateDateRemise(props.dossierId, typeDocId, nouvelleDateRemise.value || null)
        nouvelleDateRemise.value = ''
        await chargerDonnees()
    } catch (e) {
        console.error('Erreur modification date de remise', e)
        alert(e?.response?.data || 'Erreur lors de la modification de la date de remise.')
    }
}

/** Retirer uniquement la date de remise (garde le document actif) */
async function retirerDateRemise(typeDocId) {
    try {
        await DocumentService.updateDateRemise(props.dossierId, typeDocId, null)
        await chargerDonnees()
    } catch (e) {
        console.error('Erreur retrait date de remise', e)
        alert(e?.response?.data || 'Erreur lors du retrait de la date de remise.')
    }
}

/** Retirer immédiatement un document (désactive le document + supprime la remise) */
async function retirer(typeDocId) {
    if (!props.dossierId) return
    try {
        await DocumentService.retirerDocument(props.dossierId, typeDocId)
        await chargerDonnees()
        emit('documents-valides', { succes: true })
    } catch (e) {
        console.error('Erreur retrait document', e)
        alert(e?.response?.data || 'Erreur lors du retrait du document.')
    }
}

function annulerRetrait() {
    popupRetrait.value = { visible: false, typeDocId: null, nomDocument: '', pendingUncheck: null }
}

function confirmerRetrait() {
    if (popupRetrait.value.pendingUncheck) {
        popupRetrait.value.pendingUncheck()
        // Remettre à false la checkbox correspondante
        const el = document.getElementById(`doc-${popupRetrait.value.typeDocId}`)
        if (el) el.checked = false
    }
    popupRetrait.value = { visible: false, typeDocId: null, nomDocument: '', pendingUncheck: null }
}

function emitObligatoiresComplets() {
    const obligatoires = typeDocuments.value.filter(t => t.estObligatoire)
    const tousCoches = obligatoires.every(t => coches.value.has(t.id))
    emit('docs-obligatoires-complets', tousCoches)
}

async function valider() {
    if (!props.dossierId) {
        console.log('iddossier: ', props)
        // Mode création : émettre les données pour que le parent les gère
        emit('documents-valides', {
            typeIdsCoches: Array.from(coches.value),
            fichiersParTypeId: { ...fichiersTemp.value }
        })
        return
    }

    enCoursValidation.value = true
    try {
        // Calculer les ajouts et retraits
        const cochesInitiales = new Set(documentsActifs.value.map(d => d.typeDocument?.id))
        const typeIdsAjoutes = Array.from(coches.value).filter(id => !cochesInitiales.has(id))
        const typeIdsRetires = Array.from(cochesInitiales).filter(id => !coches.value.has(id))

        await DocumentService.processerBatch(
            props.dossierId,
            typeIdsAjoutes,
            typeIdsRetires,
            fichiersTemp.value,
            dateRemiseBatch.value || null
        )

        // Recharger
        fichiersTemp.value = {}
        dateRemiseBatch.value = ''
        await chargerDonnees()
        emit('documents-valides', { succes: true })
    } catch (e) {
        console.error('Erreur validation documents', e)
        alert(e?.response?.data || 'Erreur lors de la validation des documents.')
    } finally {
        enCoursValidation.value = false
    }
}

// Exposer pour le mode création
defineExpose({ coches, fichiersTemp, typeDocuments })
</script>

<style scoped>
.documents-section {
    font-size: 0.85rem;
}

.doc-item {
    padding: 10px 12px;
    border-radius: 8px;
    transition: background 0.15s;
}

.doc-item:hover,
.doc-item--assigned {
    background: #f8f9fa;
}

.doc-checkbox {
    width: 1rem;
    height: 1rem;
    cursor: pointer;
}

.cursor-pointer {
    cursor: pointer;
}

.batch-bar {
    background: #eef4ff;
    border: 1px solid #dbe7ff;
}

.doc-thumbnail {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 8px;
    border: 1px solid #dee2e6;
    border-radius: 6px;
    background: #f8f9fa;
    text-decoration: none;
    color: #495057;
    transition: border-color 0.15s, background 0.15s;
    max-width: 200px;
}

.doc-thumbnail:hover {
    border-color: #0d6efd;
    background: #e8f0fe;
}

.doc-thumbnail__img {
    width: 32px;
    height: 32px;
    object-fit: cover;
    border-radius: 4px;
}

.doc-thumbnail__icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #6c757d;
}

.doc-thumbnail__name {
    font-size: 0.72rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 130px;
}

.doc-preview-btn {
    text-decoration: none;
}
</style>

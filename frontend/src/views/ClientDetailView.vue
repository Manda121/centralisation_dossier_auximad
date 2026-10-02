<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import PageSkeleton from '../components/inc/PageSkeleton.vue'
import Client from '../models/Client'
import ClientService from '../services/ClientService'
import PermissionService, { PORTEE_PROPRE_ECRITURE, PORTEE_PROPRE_SUPPRESSION, ACTION_OT } from '../utils/permission.service'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const erreur = ref(null)
const editionMode = ref(false)
const estNouveau = ref(false)

const clientModel = ref(new Client())
const originalData = ref(null)

const typeClients = ref([])
const listePays = ref([])

const selectedTypeClientId = ref(null)
const selectedPaysId = ref(null)

const peutModifier = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)
})

const peutSupprimer = computed(() => {
    return PermissionService.verifierPermission(PORTEE_PROPRE_SUPPRESSION, ACTION_OT)
})

onMounted(async () => {
    try {
        const [types, pays] = await Promise.all([
            ClientService.getAllTypeClients(),
            ClientService.getAllPays()
        ])
        typeClients.value = types
        listePays.value = pays

        const idParam = route.params.id
        if (idParam === 'nouveau') {
            estNouveau.value = true
            editionMode.value = true
            clientModel.value = new Client({
                typeClient: types[0] || null,
                pays: pays.find(p => p.nom === 'Madagascar') || pays[0] || null
            })
            selectedTypeClientId.value = clientModel.value.typeClient?.id
            selectedPaysId.value = clientModel.value.pays?.id
            loading.value = false
        } else {
            chargerClient(idParam)
        }
    } catch (e) {
        erreur.value = "Impossible de récupérer les données référentielles."
        loading.value = false
    }
})

async function chargerClient(id) {
    loading.value = true
    try {
        const data = await ClientService.getClientById(id)
        clientModel.value = new Client(data)
        originalData.value = { ...data }
        selectedTypeClientId.value = clientModel.value.typeClient?.id
        selectedPaysId.value = clientModel.value.pays?.id
        loading.value = false
    } catch (e) {
        erreur.value = "Le client spécifié n'existe pas ou vous n'avez pas l'autorisation d'y accéder."
        loading.value = false
    }
}

function notifierTypeChange() {
    const selected = typeClients.value.find(t => t.id === selectedTypeClientId.value)
    clientModel.value.typeClient = selected || null
}

async function enregistrerClient() {
    clientModel.value.pays = listePays.value.find(p => p.id === selectedPaysId.value) || null
    clientModel.value.typeClient = typeClients.value.find(t => t.id === selectedTypeClientId.value) || null

    const validation = clientModel.value.valider()
    if (!validation.valide) {
        alert(validation.erreurs.join('\n'))
        return
    }

    loading.value = true
    try {
        if (estNouveau.value) {
            const data = await ClientService.createClient(clientModel.value.toJSON())
            alert('Client créé avec succès.')
            router.push(`/clients/${data.id}`)
        } else {
            const data = await ClientService.updateClient(clientModel.value.id, clientModel.value.toJSON())
            alert('Client mis à jour avec succès.')
            clientModel.value = new Client(data)
            originalData.value = { ...data }
            editionMode.value = false
            loading.value = false
        }
    } catch (e) {
        alert("Erreur lors de l'enregistrement : données erronées ou doublon de NIF/STAT/Email.")
        loading.value = false
    }
}

function annulerModifications() {
    if (estNouveau.value) {
        router.push('/clients')
    } else {
        clientModel.value = new Client(originalData.value)
        selectedTypeClientId.value = clientModel.value.typeClient?.id
        selectedPaysId.value = clientModel.value.pays?.id
        editionMode.value = false
    }
}

async function supprimerClient() {
    loading.value = true
    try {
        await ClientService.deleteClient(clientModel.value.id)
        alert('Client supprimé avec succès.')
        router.push('/clients')
    } catch (e) {
        if (e?.code === 'ERR_CANCELED') return
        alert(e.response?.data?.message || 'Erreur lors de la suppression du client.')
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="mb-3">
                    <router-link to="/clients" class="text-decoration-none text-secondary">
                        &larr; Retour à la liste des clients
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
                                {{ estNouveau ? 'Nouveau Client' : clientModel.getNomComplet() }}
                            </h1>
                            <p class="text-secondary mb-0 small">
                                {{ estNouveau ? 'Créer une nouvelle entité client dans le système.' : `Enregistrement CLI-${String(clientModel.id).padStart(4, '0')}` }}
                            </p>
                        </div>
                        <div class="d-flex gap-2">
                            <template v-if="editionMode">
                                <button type="button" class="btn btn-secondary btn-sm" @click="annulerModifications">Annuler</button>
                                <button type="button" class="btn btn-primary btn-sm" @click="enregistrerClient">Enregistrer</button>
                            </template>
                            <template v-else>
                                <button v-if="peutModifier" type="button" class="btn btn-outline-primary btn-sm" @click="editionMode = true">Modifier</button>
                                <button v-if="peutSupprimer" type="button" class="btn btn-outline-danger btn-sm" @click="supprimerClient">Supprimer</button>
                            </template>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-lg-8">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Informations d'identité</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3">
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Nom / Raison Sociale <span class="text-danger">*</span></label>
                                            <input type="text" v-model="clientModel.nom" class="form-control form-control-sm" :disabled="!editionMode" required>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Prénom (facultatif)</label>
                                            <input type="text" v-model="clientModel.prenom" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Type de client</label>
                                            <select v-model="selectedTypeClientId" class="form-select form-select-sm" :disabled="!editionMode" @change="notifierTypeChange">
                                                <option v-for="t in typeClients" :key="t.id" :value="t.id">{{ t.nom }}</option>
                                            </select>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Téléphone</label>
                                            <input type="text" v-model="clientModel.telephone" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Email</label>
                                            <input type="email" v-model="clientModel.email" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Fax</label>
                                            <input type="text" v-model="clientModel.fax" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Informations Douanières & Fiscales</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3">
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">NIF (Numéro d'Identification Fiscale)</label>
                                            <input type="text" v-model="clientModel.nif" class="form-control form-control-sm" :disabled="!editionMode" placeholder="ex: 10 caractères si particulier, 13 si entreprise">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">STAT (Numéro Statistique)</label>
                                            <input type="text" v-model="clientModel.stat" class="form-control form-control-sm" :disabled="!editionMode" placeholder="ex: 17 caractères">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-lg-4">
                            <div class="card border shadow-sm">
                                <div class="card-header bg-white border-bottom py-2">
                                    <h5 class="mb-0 fw-semibold text-dark">Adresse & Localisation</h5>
                                </div>
                                <div class="card-body">
                                    <div class="row g-3">
                                        <div class="col-12">
                                            <label class="form-label small fw-semibold text-secondary">Adresse postale / physique</label>
                                            <textarea v-model="clientModel.adresse" class="form-control form-control-sm" rows="3" :disabled="!editionMode"></textarea>
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">Ville</label>
                                            <input type="text" v-model="clientModel.ville" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                        <div class="col-md-6">
                                            <label class="form-label small fw-semibold text-secondary">CP-client</label>
                                            <input type="text" v-model="clientModel.cpClient" class="form-control form-control-sm" :disabled="!editionMode">
                                        </div>
                                        <div class="col-12">
                                            <label class="form-label small fw-semibold text-secondary">Pays</label>
                                            <select v-model="selectedPaysId" class="form-select form-select-sm" :disabled="!editionMode">
                                                <option v-for="p in listePays" :key="p.id" :value="p.id">{{ p.nom }}</option>
                                            </select>
                                        </div>
                                    </div>
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
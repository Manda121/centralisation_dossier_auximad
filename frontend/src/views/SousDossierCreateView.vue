<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '../components/inc/Header.vue'
import DossierService from '../services/DossierService'
import InterAgenceService from '../services/InterAgenceService'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const idDossierMere = route.params.id
const dossierMere = ref(null)
const agences = ref([])
const loading = ref(true)
const submitting = ref(false)
const erreur = ref(null)
const succes = ref(null)

const form = ref({
    idAgence: '',
    motif: '',
    idTypeProduction: ''
})

function authHeader() {
    const token = localStorage.getItem('auximad_token')
    return token ? { Authorization: `Bearer ${token}` } : {}
}

onMounted(async () => {
    try {
        const [dossier, agencesList] = await Promise.all([
            DossierService.getDossierById(idDossierMere),
            axios.get('http://localhost:8080/api/agences', { headers: authHeader() }).then(r => r.data)
        ])
        dossierMere.value = dossier
        agences.value = agencesList
        // Exclure l'agence du dossier mère de la liste
        agences.value = agencesList.filter(a => a.id !== dossier.agence?.id)
    } catch (e) {
        erreur.value = "Impossible de charger les données nécessaires."
    } finally {
        loading.value = false
    }
})

async function soumettre() {
    if (!form.value.idAgence) {
        erreur.value = "Veuillez sélectionner une agence."
        return
    }
    erreur.value = null
    submitting.value = true
    try {
        const payload = {
            idAgence: parseInt(form.value.idAgence),
            motif: form.value.motif || null,
            idTypeProduction: form.value.idTypeProduction ? parseInt(form.value.idTypeProduction) : null
        }
        const resultat = await InterAgenceService.creerSousDossier(idDossierMere, payload)
        succes.value = `Sous-dossier créé : ${resultat.sousDossier.numeroDossier}`
        // Rediriger vers le sous-dossier créé après 1.5s
        setTimeout(() => {
            router.push(`/dossiers/${resultat.sousDossier.id}`)
        }, 1500)
    } catch (e) {
        erreur.value = e.response?.data || "Erreur lors de la création du sous-dossier."
    } finally {
        submitting.value = false
    }
}

function formatDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid" style="max-width: 760px;">

                <!-- Breadcrumb -->
                <div class="mb-3 small">
                    <router-link to="/dossiers" class="text-decoration-none text-secondary">Dossiers</router-link>
                    <span class="text-secondary mx-1">/</span>
                    <router-link :to="`/dossiers/${idDossierMere}`" class="text-decoration-none text-secondary">
                        {{ dossierMere?.numeroDossier || '...' }}
                    </router-link>
                    <span class="text-secondary mx-1">/</span>
                    <span class="text-dark fw-semibold">Créer un sous-dossier</span>
                </div>

                <!-- Titre -->
                <div class="mb-4">
                    <h1 class="h4 fw-bold text-dark mb-1">Créer un Sous-Dossier Inter-Agences</h1>
                    <p class="text-secondary small mb-0">
                        Le sous-dossier héritera automatiquement du contenu du dossier mère.
                        Seule l'agence gestionnaire sera différente.
                    </p>
                </div>

                <!-- Alerts -->
                <div v-if="erreur" class="alert alert-danger alert-dismissible mb-3" role="alert">
                    {{ erreur }}
                    <button type="button" class="btn-close" @click="erreur = null"></button>
                </div>
                <div v-if="succes" class="alert alert-success mb-3" role="alert">
                    <strong>✓ {{ succes }}</strong> — Redirection en cours…
                </div>

                <!-- Carte Dossier Mère (lecture seule) -->
                <div v-if="loading" class="text-center py-5 text-secondary">Chargement…</div>
                <template v-else>
                    <div class="card border shadow-sm mb-4">
                        <div class="card-header bg-white border-bottom py-2 d-flex align-items-center gap-2">
                            <span class="badge bg-primary-subtle text-primary-emphasis small">Dossier mère</span>
                            <h5 class="mb-0 fw-semibold text-dark">{{ dossierMere?.numeroDossier }}</h5>
                        </div>
                        <div class="card-body">
                            <div class="row g-3 small">
                                <div class="col-md-4">
                                    <div class="text-secondary">AGENCE</div>
                                    <div class="fw-semibold">{{ dossierMere?.agence?.nom || '—' }}</div>
                                </div>
                                <div class="col-md-4">
                                    <div class="text-secondary">CLIENT</div>
                                    <div class="fw-semibold">{{ dossierMere?.client?.nom }} {{ dossierMere?.client?.prenom }}</div>
                                </div>
                                <div class="col-md-4">
                                    <div class="text-secondary">OT LIÉ</div>
                                    <div class="fw-semibold">{{ dossierMere?.ordreTransit?.numeroOrdreTransit || '—' }}</div>
                                </div>
                                <div class="col-md-4">
                                    <div class="text-secondary">PHASE</div>
                                    <div class="fw-semibold">{{ dossierMere?.phaseDossier?.nom || '—' }}</div>
                                </div>
                                <div class="col-md-4">
                                    <div class="text-secondary">DATE CRÉATION</div>
                                    <div class="fw-semibold">{{ formatDate(dossierMere?.dateCreation) }}</div>
                                </div>
                                <div class="col-md-4">
                                    <div class="text-secondary">GROUPAGE</div>
                                    <div class="fw-semibold">{{ dossierMere?.estGroupage ? 'Oui' : 'Non' }}</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Formulaire sous-dossier -->
                    <div class="card border shadow-sm">
                        <div class="card-header bg-white border-bottom py-2">
                            <h5 class="mb-0 fw-semibold text-dark">Paramètres du Sous-Dossier</h5>
                        </div>
                        <div class="card-body">
                            <form @submit.prevent="soumettre">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold text-dark">
                                        Agence chargée du suivi <span class="text-danger">*</span>
                                    </label>
                                    <select v-model="form.idAgence" class="form-select" required>
                                        <option value="">— Sélectionner une agence —</option>
                                        <option v-for="a in agences" :key="a.id" :value="a.id">
                                            {{ a.nom }} ({{ a.ville }})
                                        </option>
                                    </select>
                                    <div class="form-text text-muted">
                                        L'agence du dossier mère ({{ dossierMere?.agence?.nom }}) est exclue de la liste.
                                    </div>
                                </div>

                                <div class="mb-4">
                                    <label class="form-label fw-semibold text-dark">Motif de la délégation</label>
                                    <textarea
                                        v-model="form.motif"
                                        class="form-control"
                                        rows="3"
                                        placeholder="Ex: Opérations de dédouanement à Tamatave — agence locale compétente…"
                                    ></textarea>
                                </div>

                                <div class="d-flex gap-2 justify-content-end">
                                    <router-link :to="`/dossiers/${idDossierMere}`" class="btn btn-outline-secondary">
                                        Annuler
                                    </router-link>
                                    <button type="submit" class="btn btn-primary" :disabled="submitting">
                                        <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
                                        {{ submitting ? 'Création…' : 'Créer le sous-dossier' }}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </template>
            </div>
        </main>
    </div>
</template>

<style scoped>
.bg-primary-subtle { background-color: #e6edf6; }
.text-primary-emphasis { color: #2c5f8a; }
</style>

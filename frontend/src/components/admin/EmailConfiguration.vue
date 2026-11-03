<template>
    <div class="email-configuration">
        <header class="mb-4">
            <h2>Adresses emails</h2>
            <p class="text-secondary">Configurez les destinataires des demandes interagences et consultez l’historique des emails.</p>
        </header>

        <p v-if="erreur" class="alert alert-danger" role="alert">{{ erreur }}</p>
        <p v-if="succes" class="alert alert-success" role="status">{{ succes }}</p>
        <p v-if="smtpConfigure === false" class="alert alert-warning" role="status">
            Le service d’envoi n’est pas configuré sur le serveur. Vous pouvez préparer les adresses ; les emails non envoyés seront visibles dans l’historique.
        </p>

        <section class="card border-0 shadow-sm mb-4" aria-labelledby="email-destinataires-title">
            <div class="card-body">
                <h3 id="email-destinataires-title" class="h5">Destinataires par agence</h3>
                <p class="text-secondary small">Chaque adresse active de l’agence sollicitée reçoit un email. Désactivez une adresse pour suspendre ses notifications.</p>
                <form class="row g-3 align-items-end mb-4" @submit.prevent="enregistrer">
                    <div class="col-md-4">
                        <label for="email-agence" class="form-label">Agence</label>
                        <select id="email-agence" v-model="form.idAgence" class="form-select" required :disabled="enregistrement">
                            <option value="" disabled>Sélectionner une agence</option>
                            <option v-for="agence in agences" :key="agence.id" :value="agence.id">{{ agence.nom }}</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label for="email-adresse" class="form-label">Adresse email</label>
                        <input id="email-adresse" v-model="form.email" type="email" maxlength="254" class="form-control" required :disabled="enregistrement" placeholder="service@agence.mg" />
                    </div>
                    <div class="col-md-4 d-flex gap-2 align-items-center flex-wrap">
                        <div class="form-check me-2">
                            <input id="email-actif" v-model="form.actif" class="form-check-input" type="checkbox" :disabled="enregistrement" />
                            <label for="email-actif" class="form-check-label">Active</label>
                        </div>
                        <button class="btn btn-primary" :disabled="enregistrement || chargement">{{ enregistrement ? 'Enregistrement…' : form.id ? 'Enregistrer' : 'Ajouter' }}</button>
                        <button v-if="form.id" type="button" class="btn btn-outline-secondary" :disabled="enregistrement" @click="reinitialiser">Annuler</button>
                    </div>
                </form>
                <p v-if="chargement" role="status">Chargement des adresses…</p>
                <div v-else class="table-responsive">
                    <table class="table align-middle">
                        <thead><tr><th scope="col">Agence</th><th scope="col">Adresse email</th><th scope="col">État</th><th scope="col">Action</th></tr></thead>
                        <tbody>
                            <tr v-for="adresse in adresses" :key="adresse.id">
                                <td>{{ adresse.agence.nom }}</td><td>{{ adresse.email }}</td>
                                <td><span class="badge" :class="adresse.actif ? 'text-bg-success' : 'text-bg-secondary'">{{ adresse.actif ? 'Active' : 'Inactive' }}</span></td>
                                <td><button type="button" class="btn btn-sm btn-outline-primary" :disabled="enregistrement" @click="modifier(adresse)">Modifier<span class="visually-hidden"> {{ adresse.email }}</span></button></td>
                            </tr>
                            <tr v-if="!adresses.length"><td colspan="4" class="text-secondary">Aucune adresse configurée.</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>

        <section class="card border-0 shadow-sm" aria-labelledby="email-historique-title">
            <div class="card-body">
                <div class="d-flex justify-content-between gap-3 flex-wrap mb-3">
                    <div>
                        <h3 id="email-historique-title" class="h5">Historique des emails</h3>
                        <p class="small text-secondary mb-0">Les envois en attente sont traités automatiquement. « Envoyé » indique l’acceptation par le serveur de messagerie.</p>
                    </div>
                    <button type="button" class="btn btn-outline-secondary" :disabled="chargementHistorique" @click="chargerHistorique(page)">Actualiser</button>
                </div>
                <div class="row g-3 mb-3">
                    <div class="col-md-6">
                        <label for="email-filtre-agence" class="form-label">Agence destinataire</label>
                        <select id="email-filtre-agence" v-model="filtreAgence" class="form-select" @change="chargerHistorique(0)">
                            <option value="">Toutes les agences</option>
                            <option v-for="agence in agences" :key="agence.id" :value="agence.id">{{ agence.nom }}</option>
                        </select>
                    </div>
                    <div class="col-md-6">
                        <label for="email-filtre-statut" class="form-label">Statut</label>
                        <select id="email-filtre-statut" v-model="filtreStatut" class="form-select" @change="chargerHistorique(0)">
                            <option value="">Tous les statuts</option>
                            <option v-for="(libelle, statut) in statuts" :key="statut" :value="statut">{{ libelle }}</option>
                        </select>
                    </div>
                </div>
                <p v-if="chargementHistorique" role="status">Chargement de l’historique…</p>
                <div v-else class="table-responsive">
                    <table class="table align-middle email-history">
                        <thead><tr><th scope="col">Date</th><th scope="col">Agence / Dossier</th><th scope="col">Destinataire</th><th scope="col">Email</th><th scope="col">Statut</th><th scope="col">Action</th></tr></thead>
                        <tbody>
                            <tr v-for="mail in historique.content" :key="mail.id">
                                <td>{{ date(mail.dateCreation) }}<small v-if="mail.dateEnvoi" class="d-block text-secondary">Envoyé le {{ date(mail.dateEnvoi) }}</small></td>
                                <td>{{ mail.nomAgence }}<small class="d-block text-secondary">{{ mail.numeroDossier }} · Demande n°{{ mail.idDemandeInteragence }}</small></td>
                                <td class="email-address">{{ mail.destinataire || 'Aucune adresse active' }}</td>
                                <td>
                                    <details>
                                        <summary>{{ mail.sujet }}</summary>
                                        <p v-if="mail.expediteur" class="small mt-2">De : {{ mail.expediteur }}</p>
                                        <pre class="email-body">{{ mail.corps }}</pre>
                                        <p v-if="mail.idMailOrigine" class="small text-secondary">Relance de l’email n°{{ mail.idMailOrigine }}</p>
                                        <p v-if="mail.dateTentative" class="small text-secondary">Tentative le {{ date(mail.dateTentative) }}</p>
                                    </details>
                                    <p v-if="mail.erreur" class="text-danger small mb-0 mt-2">{{ mail.erreur }}</p>
                                </td>
                                <td><span class="badge" :class="couleurStatut(mail.statut)">{{ statuts[mail.statut] || mail.statut }}</span></td>
                                <td><button v-if="['ECHEC', 'NON_CONFIGURE'].includes(mail.statut)" type="button" class="btn btn-sm btn-outline-primary" :disabled="relanceId !== null" @click="relancer(mail)">{{ relanceId === mail.id ? 'Relance…' : 'Relancer' }}<span class="visually-hidden"> l’email n°{{ mail.id }}</span></button></td>
                            </tr>
                            <tr v-if="!historique.content.length"><td colspan="6" class="text-secondary">Aucun email pour ces filtres.</td></tr>
                        </tbody>
                    </table>
                </div>
                <nav class="d-flex justify-content-between align-items-center gap-2 mt-3" aria-label="Pagination de l’historique des emails">
                    <span class="small text-secondary">{{ historique.totalElements }} email(s) · Page {{ historique.totalPages ? page + 1 : 0 }} / {{ historique.totalPages }}</span>
                    <div class="d-flex gap-2">
                        <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="chargementHistorique || page === 0" @click="chargerHistorique(page - 1)">Précédent</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" :disabled="chargementHistorique || page + 1 >= historique.totalPages" @click="chargerHistorique(page + 1)">Suivant</button>
                    </div>
                </nav>
            </div>
        </section>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AdminService from '../../services/AdminService'

const agences = ref([])
const adresses = ref([])
const smtpConfigure = ref(null)
const chargement = ref(true)
const chargementHistorique = ref(false)
const enregistrement = ref(false)
const relanceId = ref(null)
const erreur = ref('')
const succes = ref('')
const form = ref({ id: null, idAgence: '', email: '', actif: true })
const filtreAgence = ref('')
const filtreStatut = ref('')
const page = ref(0)
const historique = ref({ content: [], totalElements: 0, totalPages: 0 })
const statuts = { EN_ATTENTE: 'En attente', ENVOYE: 'Envoyé', ECHEC: 'Échec', NON_CONFIGURE: 'Non configuré', ANNULE: 'Annulé' }
let historiqueRequete = 0

const messageErreur = e => e.response?.data?.erreur || 'Une erreur est survenue. Veuillez réessayer.'
const date = valeur => valeur ? new Date(valeur).toLocaleString('fr-FR') : '—'
const couleurStatut = statut => ({ ENVOYE: 'text-bg-success', ECHEC: 'text-bg-danger', NON_CONFIGURE: 'text-bg-warning', EN_ATTENTE: 'text-bg-secondary', ANNULE: 'text-bg-secondary' })[statut]
const reinitialiser = () => { form.value = { id: null, idAgence: '', email: '', actif: true } }
const modifier = adresse => {
    form.value = { id: adresse.id, idAgence: adresse.agence.id, email: adresse.email, actif: adresse.actif }
    document.getElementById('email-adresse')?.focus()
}

async function chargerHistorique(nouvellePage = 0) {
    const requete = ++historiqueRequete
    chargementHistorique.value = true
    try {
        const resultat = await AdminService.emails.historique({ page: nouvellePage, size: 15,
            agenceId: filtreAgence.value || undefined, statut: filtreStatut.value || undefined })
        if (requete !== historiqueRequete) return
        historique.value = resultat
        page.value = nouvellePage
    } catch (e) {
        if (requete === historiqueRequete) erreur.value = messageErreur(e)
    } finally {
        if (requete === historiqueRequete) chargementHistorique.value = false
    }
}

async function enregistrer() {
    enregistrement.value = true
    erreur.value = ''; succes.value = ''
    try {
        const { id, ...donnees } = form.value
        if (id) await AdminService.emails.modifier(id, donnees)
        else await AdminService.emails.creer(donnees)
        reinitialiser()
        succes.value = 'Adresse email enregistrée.'
        adresses.value = await AdminService.emails.lister()
    } catch (e) { erreur.value = messageErreur(e) }
    finally { enregistrement.value = false }
}

async function relancer(mail) {
    relanceId.value = mail.id
    erreur.value = ''; succes.value = ''
    try {
        await AdminService.emails.relancer(mail.id)
        succes.value = 'Relance enregistrée. Une nouvelle ligne conserve le résultat de cette tentative.'
        await chargerHistorique(0)
    } catch (e) { erreur.value = messageErreur(e) }
    finally { relanceId.value = null }
}

onMounted(async () => {
    const resultats = await Promise.allSettled([
        AdminService.agences.lister(), AdminService.emails.lister(), AdminService.emails.configuration(), chargerHistorique(0),
    ])
    if (resultats[0].status === 'fulfilled') agences.value = resultats[0].value
    if (resultats[1].status === 'fulfilled') adresses.value = resultats[1].value
    if (resultats[2].status === 'fulfilled') smtpConfigure.value = resultats[2].value.smtpConfigure
    const echec = resultats.find(r => r.status === 'rejected')
    if (echec) erreur.value = messageErreur(echec.reason)
    chargement.value = false
})
</script>

<style scoped>
.email-configuration { min-width: 0; }
.email-history { font-size: .875rem; }
.email-history td { min-width: 110px; vertical-align: top; }
.email-history details { min-width: 220px; max-width: 420px; }
.email-history summary { cursor: pointer; }
.email-body { white-space: pre-wrap; overflow-wrap: anywhere; font-family: inherit; margin-top: 1rem; }
.email-address { overflow-wrap: anywhere; }
</style>

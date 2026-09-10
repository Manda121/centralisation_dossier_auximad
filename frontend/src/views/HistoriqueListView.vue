<script setup>
import { ref, reactive, onMounted } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import HistoriqueService from '../services/HistoriqueService'

const datatableRef = ref(null)
const actions = ref([])
const utilisateurs = ref([])
const filtresRef = ref({ adressesIp: [], hostnames: [], logiciels: [] })

function getFiveMinutesAgoFormatted() {
    const d = new Date(Date.now() - 5 * 60 * 1000)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
}

function getNowFormatted() {
    const d = new Date()
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
}

const filters = reactive({
    query: '',
    utilisateurId: '',
    actionId: '',
    adresseIp: '',
    hostname: '',
    logiciel: '',
    methodeHttp: '',
    statutHttp: '',
    dateMin: getFiveMinutesAgoFormatted(),
    dateMax: getNowFormatted()
})

const selected = ref(null)
const showDetailModal = ref(false)

const columns = [
    { key: 'dateAction', label: 'Date', sortable: true },
    { key: 'utilisateur.nomUtilisateur', label: 'Utilisateur', sortable: false },
    { key: 'action.nom', label: 'Action', sortable: true },
    { key: 'entite', label: 'Entité', sortable: true },
    { key: 'methodeHttp', label: 'Méthode', sortable: true },
    { key: 'url', label: 'URL', sortable: true },
    { key: 'statutHttp', label: 'Statut', sortable: true },
    { key: 'adresseIp', label: 'Adresse IP', sortable: true },
    { key: 'hostname', label: 'Hostname', sortable: false },
    { key: 'logiciel', label: 'Logiciel', sortable: false }
]

const methodesHttp = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']

onMounted(async () => {
    try {
        const [a, u, f] = await Promise.all([
            HistoriqueService.getActions(),
            HistoriqueService.getUtilisateurs(),
            HistoriqueService.getFiltres()
        ])
        actions.value = a
        utilisateurs.value = u
        filtresRef.value = f
    } catch (e) {
        console.error('Impossible de charger les données de filtre', e)
    }
})

function formatDateKey(dt) {
    if (!dt) return 'inconnu'
    const d = new Date(dt)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

async function chargerHistoriques(page, size, sortBy, sortDir, filters) {
    const res = await HistoriqueService.searchHistoriques(page, size, sortBy, sortDir, filters)
    if (!res || !res.content) return res

    // Regroupement par seconde (hh:mm:ss)
    const groupedMap = new Map()
    for (const item of res.content) {
        const secondKey = formatDateKey(item.dateAction)
        if (!groupedMap.has(secondKey)) {
            groupedMap.set(secondKey, [])
        }
        groupedMap.get(secondKey).push(item)
    }

    const groupedContent = []
    for (const [secondKey, items] of groupedMap.entries()) {
        const premier = items[0]
        groupedContent.push({
            ...premier,
            groupItems: items,
            groupCount: items.length,
            secondKey: secondKey
        })
    }

    return {
        ...res,
        content: groupedContent
    }
}

function reinitialiserFiltres() {
    Object.keys(filters).forEach(k => { filters[k] = '' })
    filters.dateMin = getFiveMinutesAgoFormatted()
    filters.dateMax = getNowFormatted()
    datatableRef.value?.refresh(true)
}

function formatDate(dt) {
    if (!dt) return '—'
    return new Date(dt).toLocaleString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
}

function ouvrirDetail(row) {
    selected.value = row
    showDetailModal.value = true
}

function actionBadgeClass(nom) {
    switch (nom) {
        case 'creation': return 'badge bg-success'
        case 'modification': return 'badge bg-primary'
        case 'suppression': return 'badge bg-danger'
        case 'validation': return 'badge bg-info text-dark'
        case 'connexion': return 'badge bg-dark'
        case 'deconnexion': return 'badge bg-secondary'
        case 'connexion echouee': return 'badge bg-warning text-dark'
        default: return 'badge bg-light text-dark border'
    }
}

function statutClass(statut) {
    if (statut >= 500) return 'text-danger fw-bold'
    if (statut >= 400) return 'text-warning fw-bold'
    return 'text-success'
}

function nomUtilisateur(row) {
    if (!row.utilisateur) return 'Anonyme'
    const complet = [row.utilisateur.prenom, row.utilisateur.nom].filter(Boolean).join(' ')
    return complet || row.utilisateur.nomUtilisateur || '—'
}
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <!-- En-tête -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h1 class="h4 fw-bold text-dark mb-1">Historique des actions</h1>
                        <p class="text-secondary mb-0 small">
                            Journal d'audit : connexions, créations, modifications, suppressions…
                        </p>
                    </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-outline-secondary btn-sm" @click="reinitialiserFiltres">
                            🔄 Réinitialiser les filtres
                        </button>
                        <button class="btn btn-primary btn-sm" @click="datatableRef?.refresh()">
                            ↻ Rafraîchir
                        </button>
                    </div>
                </div>

                <!-- Tableau -->
                <div class="card border shadow-sm">
                    <div class="card-body p-3">
                        <DataTable ref="datatableRef" :columns="columns" :fetch-function="chargerHistoriques"
                            :filters="filters" default-sort="dateAction" default-sort-dir="desc" :pageSize="50"
                            row-clickable @row-click="ouvrirDetail">

                            <template #filters="{ filters: f }">
                                <div class="row g-2">
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Recherche</label>
                                        <input type="text" v-model="f.query" class="form-control form-control-sm"
                                            placeholder="URL, détails, utilisateur…">
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Utilisateur</label>
                                        <select v-model="f.utilisateurId" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="u in utilisateurs" :key="u.id" :value="u.id">
                                                {{ u.nomComplet }} ({{ u.nomUtilisateur }})
                                            </option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Type d'action</label>
                                        <select v-model="f.actionId" class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="a in actions" :key="a.id" :value="a.id">{{ a.nom }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Adresse IP</label>
                                        <select v-model="f.adresseIp" class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="ip in filtresRef.adressesIp" :key="ip" :value="ip">{{ ip }}
                                            </option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Hostname</label>
                                        <select v-model="f.hostname" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="h in filtresRef.hostnames" :key="h" :value="h">{{ h }}
                                            </option>
                                        </select>
                                    </div>
                                    <div class="col-md-3">
                                        <label class="form-label small fw-semibold text-secondary">Logiciel
                                            (User-Agent)</label>
                                        <select v-model="f.logiciel" class="form-select form-select-sm">
                                            <option value="">Tous</option>
                                            <option v-for="l in filtresRef.logiciels" :key="l" :value="l">{{ l }}
                                            </option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Méthode HTTP</label>
                                        <select v-model="f.methodeHttp" class="form-select form-select-sm">
                                            <option value="">Toutes</option>
                                            <option v-for="m in methodesHttp" :key="m" :value="m">{{ m }}</option>
                                        </select>
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Statut HTTP</label>
                                        <input type="number" v-model="f.statutHttp" class="form-control form-control-sm"
                                            placeholder="ex: 200">
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Date début</label>
                                        <input type="datetime-local" v-model="f.dateMin"
                                            class="form-control form-control-sm">
                                    </div>
                                    <div class="col-md-2">
                                        <label class="form-label small fw-semibold text-secondary">Date fin</label>
                                        <input type="datetime-local" v-model="f.dateMax"
                                            class="form-control form-control-sm">
                                    </div>
                                </div>
                            </template>

                            <template #cell-utilisateur.nomUtilisateur="{ row }">
                                {{ nomUtilisateur(row) }}
                            </template>

                            <template #cell-action.nom="{ row }">
                                <span :class="actionBadgeClass(row.action?.nom)">
                                    {{ row.action?.nom || '—' }}
                                </span>
                            </template>

                            <template #cell-methodeHttp="{ row }">
                                <span class="fw-semibold font-monospace small">{{ row.methodeHttp || '—' }}</span>
                            </template>

                            <template #cell-url="{ row }">
                                <span class="text-truncate d-inline-block font-monospace small"
                                    style="max-width: 240px;" :title="row.url">
                                    {{ row.url || '—' }}
                                </span>
                            </template>

                            <template #cell-statutHttp="{ row }">
                                <span :class="statutClass(row.statutHttp)">{{ row.statutHttp }}</span>
                            </template>

                            <template #cell-dateAction="{ row }">
                                <div class="d-flex align-items-center gap-2">
                                    <span>{{ formatDate(row.dateAction) }}</span>
                                    <!-- <span v-if="row.groupCount > 1"
                                        class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill font-monospace"
                                        style="font-size: 0.72rem;"
                                        :title="`${row.groupCount} actions enregistrées dans la même seconde`">
                                        +{{ row.groupCount - 1 }} action{{ row.groupCount > 2 ? 's' : '' }}
                                    </span> -->
                                    <span v-if="row.groupCount > 1">.</span>

                                </div>
                            </template>

                            <template #cell-logiciel="{ row }">
                                <span class="text-truncate d-inline-block small" style="max-width: 160px;"
                                    :title="row.logiciel">
                                    {{ row.logiciel || '—' }}
                                </span>
                            </template>
                        </DataTable>
                    </div>
                </div>
            </div>
        </main>

        <!-- Modal détail (Individuel ou Groupé) -->
        <div v-if="showDetailModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5)">
            <div class="modal-dialog modal-dialog-centered" :class="selected?.groupCount > 1 ? 'modal-xl' : 'modal-lg'">
                <div class="modal-content border-0 shadow">
                    <div class="modal-header bg-light py-3">
                        <div>
                            <h5 class="modal-title h6 fw-bold text-dark mb-0">
                                <template v-if="selected?.groupCount > 1">
                                    Groupe d'actions à {{ formatDate(selected?.dateAction) }}
                                    <span class="badge bg-primary ms-2">{{ selected.groupCount }} actions
                                        regroupées</span>
                                </template>
                                <template v-else>
                                    Détail de l'action #{{ selected?.id }}
                                </template>
                            </h5>
                            <small class="text-muted" v-if="selected?.groupCount > 1">
                                Actions exécutées dans la même seconde ({{ selected?.secondKey }})
                            </small>
                        </div>
                        <button type="button" class="btn-close" @click="showDetailModal = false"></button>
                    </div>

                    <div class="modal-body p-4" v-if="selected">
                        <!-- VUE GROUPÉE (Plusieurs actions dans la même seconde) -->
                        <div v-if="selected.groupCount > 1">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0 border">
                                    <thead class="table-light small text-uppercase">
                                        <tr>
                                            <th>#ID</th>
                                            <th>Action</th>
                                            <th>Entité</th>
                                            <th>Méthode &amp; URL</th>
                                            <th>Statut</th>
                                            <th>Utilisateur</th>
                                            <th>IP / Hostname</th>
                                            <th>Logiciel</th>
                                            <th>Détails</th>
                                        </tr>
                                    </thead>
                                    <tbody class="small">
                                        <tr v-for="item in selected.groupItems" :key="item.id">
                                            <td class="fw-bold font-monospace text-secondary">#{{ item.id }}</td>
                                            <td>
                                                <span :class="actionBadgeClass(item.action?.nom)">
                                                    {{ item.action?.nom || '—' }}
                                                </span>
                                            </td>
                                            <td class="fw-semibold">
                                                {{ item.entite || '—' }}
                                                <span v-if="item.identifiantEntite" class="text-muted small">({{
                                                    item.identifiantEntite }})</span>
                                            </td>
                                            <td>
                                                <span class="badge bg-light text-dark font-monospace border me-1">{{
                                                    item.methodeHttp }}</span>
                                                <span class="font-monospace text-truncate d-inline-block"
                                                    style="max-width: 220px;" :title="item.url">{{ item.url }}</span>
                                            </td>
                                            <td>
                                                <span :class="statutClass(item.statutHttp)">{{ item.statutHttp }}</span>
                                            </td>
                                            <td>{{ nomUtilisateur(item) }}</td>
                                            <td class="font-monospace text-muted small">
                                                {{ item.adresseIp }}
                                                <div class="text-secondary small" style="font-size: 0.75rem;">{{
                                                    item.hostname
                                                }}</div>
                                            </td>
                                            <td>
                                                <span class="text-truncate d-inline-block" style="max-width: 120px;"
                                                    :title="item.logiciel">
                                                    {{ item.logiciel || '—' }}
                                                </span>
                                            </td>
                                            <td>
                                                <span class="text-muted text-truncate d-inline-block"
                                                    style="max-width: 180px;" :title="item.details">
                                                    {{ item.details || '—' }}
                                                </span>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <!-- VUE SINGLE (Une seule action) -->
                        <div v-else class="row g-3 small">
                            <div class="col-md-6">
                                <div class="text-secondary">DATE &amp; HEURE</div>
                                <div class="fw-semibold text-dark">{{ formatDate(selected.dateAction) }}</div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">UTILISATEUR</div>
                                <div class="fw-semibold text-dark">
                                    {{ selected.utilisateur ? nomUtilisateur(selected) : 'Anonyme / non résolu' }}
                                    <span v-if="selected.utilisateur" class="text-muted">
                                        ({{ selected.utilisateur.nomUtilisateur }})
                                    </span>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">TYPE D'ACTION</div>
                                <div><span :class="actionBadgeClass(selected.action?.nom)">
                                        {{ selected.action?.nom || '—' }}
                                    </span></div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">ENTITÉ CONCERNÉE</div>
                                <div class="fw-semibold text-dark">
                                    {{ selected.entite || '—' }}
                                    <span v-if="selected.identifiantEntite" class="text-muted">
                                        ({{ selected.identifiantEntite }})
                                    </span>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">REQUÊTE HTTP</div>
                                <div class="fw-semibold text-dark font-monospace">
                                    {{ selected.methodeHttp || '—' }} — statut {{ selected.statutHttp }}
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">ADRESSE IP</div>
                                <div class="fw-semibold text-dark font-monospace">{{ selected.adresseIp || '—' }}</div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">HOSTNAME</div>
                                <div class="fw-semibold text-dark">{{ selected.hostname || '—' }}</div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-secondary">LOGICIEL (USER-AGENT)</div>
                                <div class="fw-semibold text-dark" style="word-break: break-all;">
                                    {{ selected.logiciel || '—' }}
                                </div>
                            </div>
                            <div class="col-12">
                                <div class="text-secondary">URL COMPLÈTE</div>
                                <div class="p-2 bg-white border rounded font-monospace text-dark"
                                    style="word-break: break-all;">
                                    {{ selected.url || '—' }}
                                </div>
                            </div>
                            <div class="col-12" v-if="selected.details">
                                <div class="text-secondary">DÉTAILS</div>
                                <div class="p-2 bg-white border rounded text-dark">{{ selected.details }}</div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer bg-light py-2">
                        <button type="button" class="btn btn-secondary btn-sm" @click="showDetailModal = false">
                            Fermer
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.font-monospace {
    font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.modal.show {
    z-index: 1055;
}
</style>

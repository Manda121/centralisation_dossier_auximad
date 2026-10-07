<script setup>
import { ref, reactive, onMounted } from 'vue'
import Header from '../components/inc/Header.vue'
import DataTable from '../components/inc/DataTable.vue'
import MvtDossierService from '../services/MvtDossierService'
import AdminService from '../services/AdminService'
import DossierService from '../services/DossierService'
import { useNotification } from '../composables/useNotification'
import PermissionService, { PORTEE_PROPRE_ECRITURE, ACTION_OT } from '../utils/permission.service'

const { notifySuccess, notifyError, confirm } = useNotification()
const peutAnnuler = PermissionService.verifierPermission(PORTEE_PROPRE_ECRITURE, ACTION_OT)

const datatableRef = ref(null)
const dateLocale = new Date()
const aujourdhui = `${dateLocale.getFullYear()}-${String(dateLocale.getMonth() + 1).padStart(2, '0')}-${String(dateLocale.getDate()).padStart(2, '0')}`

// ── Référentiels ──────────────────────────────────────────────
const agences = ref([])
const typesProduction = ref([])
const phases = ref([])
const statuts = ref([])

onMounted(async () => {
  try {
    const [agRes, tpRes, phRes, stRes] = await Promise.all([
      AdminService.agences.lister(),
      AdminService.typesProduction.lister(),
      DossierService.getAllPhases(),
      DossierService.getAllStatuts()
    ])
    agences.value = agRes || []
    typesProduction.value = tpRes || []
    phases.value = phRes || []
    statuts.value = stRes || []
  } catch (e) {
    console.error('Erreur chargement référentiels mvt:', e)
  }
})

// ── Filtres ───────────────────────────────────────────────────
const filters = reactive({
  query: '',
  cpClient: '',
  numeroDossier: '',
  dateMin: aujourdhui,
  dateMax: aujourdhui,
  agenceId: '',
  typeProductionId: '',
  phaseId: '',
  statutId: '',
  operationId: '',
  utilisateurId: ''
})

// ── Colonnes ─────────────────────────────────────────────────
const columns = [
  { key: 'dateMvt',                label: 'Date Mouvement',   sortable: true,  format: 'datetime' },
  { key: 'dossier.numeroDossier',  label: 'Dossier',          sortable: false },
  { key: 'client',                 label: 'Client',           sortable: false },
  { key: 'operation.description',  label: 'Opération',        sortable: false },
  { key: 'phases',                 label: 'Phase',            sortable: false },
  { key: 'statuts',                label: 'Statut',           sortable: false },
  { key: 'utilisateur',            label: 'Utilisateur',      sortable: false },
  { key: 'document',               label: 'Document',         sortable: false },
  { key: 'etat',                   label: 'État',             sortable: false },
  { key: 'actions',                label: 'Actions',          sortable: false }
]

async function annulerMouvement(row) {
  const ok = await confirm('Annuler ce mouvement ? Tous les mouvements suivants du dossier seront aussi annulés.')
  if (!ok) return
  try {
    await MvtDossierService.annulerMouvement(row.id)
    datatableRef.value?.refresh()
    notifySuccess('Mouvement annulé.')
  } catch (e) {
    notifyError(e.response?.data?.erreur || 'Impossible d’annuler le mouvement.')
  }
}

// ── Fetch ─────────────────────────────────────────────────────
async function chargerMouvements(page, size, sortBy, sortDir, f) {
  return await MvtDossierService.searchMouvements(page, size, sortBy || 'dateMvt', sortDir || 'desc', f)
}

// ── Formatage ─────────────────────────────────────────────────
function formatDate(dt) {
  if (!dt) return '—'
  return new Date(dt).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

// ── Aperçu document (inline modal) ────────────────────────────
const apercuDoc = ref(null)

function ouvrirApercu(doc) {
  apercuDoc.value = doc
}

function getFichierUrl(doc) {
  if (!doc) return '#'
  if (doc.cheminFichier) return `http://localhost:8080/uploads/documents/${doc.cheminFichier}`
  return `http://localhost:8080/api/documents/${doc.id}/fichier`
}

function estImage(nom) {
  if (!nom) return false
  return /\.(jpg|jpeg|png|gif|webp)$/i.test(nom)
}

function estPdf(nom) {
  if (!nom) return false
  return /\.pdf$/i.test(nom)
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
            <h1 class="h4 fw-bold text-dark mb-1">Mouvements de Dossier</h1>
            <p class="text-secondary mb-0 small">Suivi en temps réel des opérations et avancements.</p>
          </div>
          <router-link to="/mouvements-dossier/creer" class="btn btn-primary btn-sm d-flex align-items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4"/>
            </svg>
            Créer un mouvement
          </router-link>
        </div>

        <!-- DataTable -->
        <div class="card border shadow-sm">
          <div class="card-body p-3">
            <DataTable
              ref="datatableRef"
              :columns="columns"
              :fetch-function="chargerMouvements"
              :filters="filters"
              default-sort="dateMvt"
              default-sort-dir="desc"
              :pageSize="15"
            >
              <!-- ── Slot filtres ── -->
              <template #filters="{ filters: sf }">
                <div class="row g-2">
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Recherche libre</label>
                    <input type="text" v-model="sf.query" class="form-control form-control-sm"
                           placeholder="N° dossier, client…" />
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Code / Nom Client</label>
                    <input type="text" v-model="sf.cpClient" class="form-control form-control-sm"
                           placeholder="Ex: CLI-001" />
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">N° Dossier</label>
                    <input type="text" v-model="sf.numeroDossier" class="form-control form-control-sm"
                           placeholder="Ex: DOS-2026-001" />
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Agence</label>
                    <select v-model="sf.agenceId" class="form-select form-select-sm">
                      <option value="">Toutes les agences</option>
                      <option v-for="a in agences" :key="a.id" :value="a.id">{{ a.nom }}</option>
                    </select>
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Type de Production</label>
                    <select v-model="sf.typeProductionId" class="form-select form-select-sm">
                      <option value="">Tous les types</option>
                      <option v-for="tp in typesProduction" :key="tp.id" :value="tp.id">{{ tp.nom }}</option>
                    </select>
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Phase Dossier</label>
                    <select v-model="sf.phaseId" class="form-select form-select-sm">
                      <option value="">Toutes les phases</option>
                      <option v-for="p in phases" :key="p.id" :value="p.id">{{ p.nom }}</option>
                    </select>
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Statut Dossier</label>
                    <select v-model="sf.statutId" class="form-select form-select-sm">
                      <option value="">Tous les statuts</option>
                      <option v-for="s in statuts" :key="s.id" :value="s.id">{{ s.nom }}</option>
                    </select>
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Date Min</label>
                    <input type="date" v-model="sf.dateMin" class="form-control form-control-sm" />
                  </div>
                  <div class="col-md-3">
                    <label class="form-label small fw-semibold text-secondary">Date Max</label>
                    <input type="date" v-model="sf.dateMax" class="form-control form-control-sm" />
                  </div>
                </div>
              </template>

              <!-- ── Cellules personnalisées ── -->
              <template #cell-dateMvt="{ row }">
                <span class="fw-semibold small">{{ formatDate(row.dateMvt) }}</span>
              </template>

              <template #cell-dossier.numeroDossier="{ row }">
                <router-link v-if="row.dossier" :to="`/dossiers/${row.dossier.id}`"
                             class="fw-bold text-primary text-decoration-none">
                  {{ row.dossier.numeroDossier }}
                </router-link>
                <span v-else class="text-muted">—</span>
              </template>

              <template #cell-client="{ row }">
                <span v-if="row.dossier?.client" class="small">
                  <span class="fw-semibold">{{ row.dossier.client.cpClient }}</span>
                  — {{ row.dossier.client.nom }}
                </span>
                <span v-else class="text-muted">—</span>
              </template>

              <template #cell-operation.description="{ row }">
                <span class="badge bg-info-subtle text-info-emphasis border border-info-subtle px-2 py-1">
                  {{ row.operation ? row.operation.description : 'Mouvement manuel' }}
                </span>
              </template>

              <template #cell-phases="{ row }">
                <div class="d-flex align-items-center gap-1">
                  <span v-if="row.anciennePhase" class="badge bg-light text-dark border">{{ row.anciennePhase.nom }}</span>
                  <span v-if="row.anciennePhase && row.nouvellePhase" class="text-muted small">➔</span>
                  <span v-if="row.nouvellePhase" class="badge bg-primary-subtle text-primary border border-primary-subtle">{{ row.nouvellePhase.nom }}</span>
                  <span v-if="!row.anciennePhase && !row.nouvellePhase" class="text-muted">—</span>
                </div>
              </template>

              <template #cell-statuts="{ row }">
                <div class="d-flex align-items-center gap-1">
                  <span v-if="row.ancienStatut" class="badge bg-light text-dark border">{{ row.ancienStatut.nom }}</span>
                  <span v-if="row.ancienStatut && row.nouveauStatut" class="text-muted small">➔</span>
                  <span v-if="row.nouveauStatut" class="badge bg-success-subtle text-success border border-success-subtle">{{ row.nouveauStatut.nom }}</span>
                  <span v-if="!row.ancienStatut && !row.nouveauStatut" class="text-muted">—</span>
                </div>
              </template>

              <template #cell-utilisateur="{ row }">
                <span class="small text-muted">
                  {{ row.utilisateur ? (row.utilisateur.nomComplet || row.utilisateur.nomUtilisateur) : 'Système' }}
                </span>
              </template>

              <template #cell-document="{ row }">
                <button v-if="row.document" type="button"
                        class="btn btn-outline-primary btn-sm px-2 py-0 d-inline-flex align-items-center gap-1"
                        @click="ouvrirApercu(row.document)"
                        :title="row.document.nomFichier">
                  📎 <span class="small" style="max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
                    {{ row.document.nomFichier || 'Fichier' }}
                  </span>
                </button>
                <span v-else class="text-muted small">—</span>
              </template>
              <template #cell-etat="{ row }">
                <span v-if="row.annulation" class="badge bg-danger-subtle text-danger">Annulé</span>
                <span v-else-if="row.origine === 'SAISIE' || !row.origine" class="badge bg-success-subtle text-success">Effectué</span>
                <span v-else class="badge bg-info-subtle text-info-emphasis">Automatique</span>
              </template>

              <template #cell-actions="{ row }">
                <button v-if="peutAnnuler && !row.annulation" type="button"
                  class="btn btn-outline-danger btn-sm" @click="annulerMouvement(row)">Annuler</button>
                <span v-else-if="row.annulation" class="small text-muted" :title="row.annulation.motifAnnulation">
                  {{ row.annulation.motifAnnulation || 'Annulé' }}
                </span>
              </template>
            </DataTable>
          </div>
        </div>

      </div>
    </main>

    <!-- ── Modal aperçu document inline ── -->
    <div v-if="apercuDoc" class="modal fade show d-block" tabindex="-1"
         style="background: rgba(0,0,0,0.72); z-index: 2000;"
         @click.self="apercuDoc = null">
      <div class="modal-dialog modal-xl modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg" style="max-height: 92vh; overflow: hidden;">
          <div class="modal-header bg-dark text-white py-2 d-flex justify-content-between align-items-center">
            <h6 class="modal-title mb-0 text-truncate" style="max-width: 70%;">
              📎 {{ apercuDoc.nomFichier || 'Document' }}
            </h6>
            <div class="d-flex align-items-center gap-2">
              <a :href="getFichierUrl(apercuDoc)" target="_blank"
                 class="btn btn-sm btn-outline-light py-0 px-2" style="font-size:0.75rem;">
                🔗 Ouvrir dans un onglet
              </a>
              <button type="button" class="btn-close btn-close-white" @click="apercuDoc = null"></button>
            </div>
          </div>
          <div class="modal-body p-0" style="overflow: auto; max-height: 82vh;">
            <!-- Image -->
            <img v-if="estImage(apercuDoc.nomFichier)"
                 :src="getFichierUrl(apercuDoc)"
                 class="w-100 d-block"
                 :alt="apercuDoc.nomFichier" />
            <!-- PDF -->
            <iframe v-else-if="estPdf(apercuDoc.nomFichier)"
                    :src="getFichierUrl(apercuDoc)"
                    width="100%" height="700" frameborder="0"
                    style="display:block;"></iframe>
            <!-- Autres formats -->
            <div v-else class="p-5 text-center">
              <p class="text-muted mb-3">Aperçu non disponible pour ce type de fichier.</p>
              <a :href="getFichierUrl(apercuDoc)" class="btn btn-primary" download>
                ⬇ Télécharger {{ apercuDoc.nomFichier }}
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
</style>

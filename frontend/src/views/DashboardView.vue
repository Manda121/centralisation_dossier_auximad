<script setup>
import { ref, onMounted, computed } from 'vue'
import Header from '../components/inc/Header.vue'
import DashboardService from '../services/DashboardService'
import DossierEvolutionChart from '../components/dashboard/DossierEvolutionChart.vue'

// ──────────────────────────────────────────────
// STATE
// ──────────────────────────────────────────────
const loading = ref(true)
const error = ref(null)
const data = ref(null)
const moduleActif = ref('global')

const modules = [
  { key: 'global',     label: 'Vue globale' },
  { key: 'dossiers',   label: 'Dossiers' },
  { key: 'ot',         label: 'Ordres de Transit' },
  { key: 'clients',    label: 'Clients' },
  { key: 'mouvements', label: 'Mouvements' },
  { key: 'conteneurs', label: 'Conteneurs' },
  { key: 'documents',  label: 'Documents' },
]

// ──────────────────────────────────────────────
// CHARGEMENT
// ──────────────────────────────────────────────
const charger = async () => {
  loading.value = true
  error.value = null
  try {
    data.value = await DashboardService.getDashboardComplet()
  } catch (e) {
    error.value = 'Impossible de charger les données du tableau de bord.'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(charger)

// ──────────────────────────────────────────────
// KPI CARDS
// ──────────────────────────────────────────────
const kpiGlobal = computed(() => {
  const g = data.value?.global || {}
  return [
    { label: 'Dossiers',      valeur: g.totalDossiers    || 0 },
    { label: 'Ordres Transit',valeur: g.totalOT          || 0 },
    { label: 'Clients',       valeur: g.totalClients     || 0 },
    { label: 'Mouvements',    valeur: g.totalMouvements  || 0 },
    { label: 'Conteneurs',    valeur: g.totalConteneurs  || 0 },
    { label: 'Déclarations',  valeur: g.totalDeclarations|| 0 },
    { label: 'Documents',     valeur: g.totalDocuments   || 0 },
    { label: 'Utilisateurs',  valeur: g.totalUtilisateurs|| 0 },
  ]
})

// ──────────────────────────────────────────────
// DONNÉES DU MODULE ACTIF
// ──────────────────────────────────────────────
const donneeModule = computed(() => {
  if (!data.value) return null
  return data.value[moduleActif.value] || null
})

// ──────────────────────────────────────────────
// HELPERS GRAPHIQUES (SVG internes, pas Chart.js)
// Calcul de barres horizontales simples
// ──────────────────────────────────────────────
const maxVal = (arr) => {
  if (!arr || !arr.length) return 1
  return Math.max(...arr.map(x => Number(x.valeur)), 1)
}

const pourcentage = (val, max) => Math.max(0, Math.min(100, (Number(val) / max) * 100))

const formatDate = (isoStr) => {
  if (!isoStr) return ''
  try {
    return new Date(isoStr).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
  } catch { return isoStr }
}
</script>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />

    <main class="db-main flex-grow-1 p-4">
      <div class="container-fluid">
      <!-- ───── EN-TÊTE ───── -->
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
        <div>
          <h1 class="h4 fw-bold text-dark mb-1">Tableau de bord</h1>
          <p class="text-secondary mb-0 small">Vue d'ensemble de l'activité AUXIMAD</p>
        </div>
        <button type="button" class="btn btn-primary btn-sm" @click="charger" :disabled="loading">
          <span v-if="loading" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
          Actualiser
        </button>
      </div>

      <!-- ───── ERREUR ───── -->
      <div v-if="error" class="alert alert-danger" role="alert">
         {{ error }}
      </div>

      <!-- ───── SQUELETTES ───── -->
      <template v-else-if="loading">
        <div class="db-kpis">
          <div v-for="i in 8" :key="i" class="card border shadow-sm kpi-card--skeleton"></div>
        </div>
      </template>

      <template v-else-if="data">
        <!-- ───── KPI GLOBAUX ───── -->
        <div class="db-kpis">
          <div
            v-for="kpi in kpiGlobal"
            :key="kpi.label"
            class="card border shadow-sm"
          >
            <div class="card-body p-3">
              <div class="h4 fw-bold text-dark mb-1">{{ kpi.valeur.toLocaleString('fr-FR') }}</div>
              <div class="small text-secondary">{{ kpi.label }}</div>
            </div>
          </div>
        </div>

        <!-- ───── FILTRES DE MODULE ───── -->
        <div class="nav nav-tabs mb-4 gap-1" aria-label="Statistiques par module">
          <button
            v-for="m in modules"
            :key="m.key"
            type="button" class="nav-link small"
            :class="{ active: moduleActif === m.key }" :aria-current="moduleActif === m.key ? 'page' : undefined"
            @click="moduleActif = m.key"
          >
            {{ m.label }}
          </button>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- VUE GLOBALE -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-if="moduleActif === 'global'" class="db-grid">
          <!-- Évolution mensuelle dossiers -->
          <div class="card border shadow-sm db-card--wide">
            <div class="card-header bg-white border-bottom py-3">
              <h2 class="h6 fw-semibold mb-1">Évolution mensuelle des dossiers</h2>
              <p class="small text-secondary mb-0">Dossiers créés par mois sur les 12 derniers mois, mois en cours inclus.</p>
            </div>
            <DossierEvolutionChart class="card-body p-3" :evolution="data.dossiers?.evolutionMensuelle || []" />
          </div>

          <!-- Répartition par type de production (dossiers) -->
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3">
              <span class="h6 fw-semibold mb-0"> Dossiers par type</span>
            </div>
            <div class="db-bars">
              <template v-for="(item, i) in (data.dossiers?.parTypeProduction || [])" :key="i">
                <div class="db-bar-row">
                  <span class="db-bar-label small text-secondary" :title="item.label + ' ' + item.mode">
                    {{ item.label }} <small>{{ item.mode }}</small>
                  </span>
                  <div class="progress db-bar-track">
                    <div class="progress-bar"
                      :style="{ width: pourcentage(item.valeur, maxVal(data.dossiers?.parTypeProduction)) + '%' }">
                    </div>
                  </div>
                  <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
                </div>
              </template>
            </div>
          </div>

          <!-- Dossiers par agence -->
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3">
              <span class="h6 fw-semibold mb-0"> Dossiers par agence (top 10)</span>
            </div>
            <div class="db-bars">
              <template v-for="(item, i) in (data.dossiers?.parAgence || [])" :key="i">
                <div class="db-bar-row">
                  <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                  <div class="progress db-bar-track">
                    <div class="progress-bar"
                      :style="{ width: pourcentage(item.valeur, maxVal(data.dossiers?.parAgence)) + '%' }">
                    </div>
                  </div>
                  <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
                </div>
              </template>
            </div>
          </div>

          <!-- Activité récente -->
          <div class="card border shadow-sm db-card--wide">
            <div class="card-header bg-white border-bottom py-3">
              <span class="h6 fw-semibold mb-0"> Activité récente</span>
            </div>
            <div class="db-activite">
              <div v-for="mvt in (data.activite || [])" :key="mvt.id" class="db-activite-row">
                <div class="db-activite-body">
                  <span class="db-activite-op">{{ mvt.operation }}</span>
                  <span class="db-activite-meta">
                    {{ mvt.numero_dossier }} · {{ mvt.nom_utilisateur }} · {{ formatDate(mvt.date_mouvement) }}
                  </span>
                </div>
                <span class="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">{{ mvt.phase }}</span>
              </div>
              <p v-if="!(data.activite?.length)" class="text-secondary small text-center py-4 mb-0">Aucune activité récente</p>
            </div>
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE DOSSIERS -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'dossiers'" class="db-grid">
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par statut</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parStatut || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parStatut)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par phase</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parPhase || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parPhase)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par type de production</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parTypeProduction || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }} — {{ item.mode }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parTypeProduction)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par agence</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parAgence || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parAgence)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm db-card--wide">
            <div class="card-header bg-white border-bottom py-3"><h2 class="h6 fw-semibold mb-1">Évolution mensuelle des dossiers</h2>
              <p class="small text-secondary mb-0">Dossiers créés par mois sur les 12 derniers mois, mois en cours inclus.</p></div>
            <DossierEvolutionChart class="card-body p-3" :evolution="data.dossiers?.evolutionMensuelle || []" />
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE ORDRES DE TRANSIT -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'ot'" class="db-grid">
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par statut OT</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parStatut || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parStatut)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par type de production</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parTypeProduction || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }} — {{ item.mode }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parTypeProduction)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par régime douanier</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parRegime || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parRegime)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par agence</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parAgence || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parAgence)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE CLIENTS -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'clients'" class="db-grid">
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par type de client</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parType || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parType)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par pays (top 10)</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parPays || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parPays)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE MOUVEMENTS -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'mouvements'" class="db-grid">
          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par opération (top 10)</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parOperation || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parOperation)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Par phase</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parPhase || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parPhase)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE CONTENEURS -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'conteneurs'" class="db-grid">
          <div class="card border shadow-sm db-card--wide">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Conteneurs par type</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parType || [])" :key="i" class="db-bar-row db-bar-row--lg">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parType)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ════════════════════════════════════════════════════ -->
        <!-- MODULE DOCUMENTS -->
        <!-- ════════════════════════════════════════════════════ -->
        <div v-else-if="moduleActif === 'documents'" class="db-grid">
          <div class="card border shadow-sm db-card--wide">
            <div class="card-header bg-white border-bottom py-3"><span class="h6 fw-semibold mb-0"> Documents par type (top 12)</span></div>
            <div class="db-bars">
              <div v-for="(item, i) in (donneeModule?.parType || [])" :key="i" class="db-bar-row">
                <span class="db-bar-label small text-secondary">{{ item.label }}</span>
                <div class="progress db-bar-track">
                  <div class="progress-bar" :style="{ width: pourcentage(item.valeur, maxVal(donneeModule?.parType)) + '%' }"></div>
                </div>
                <span class="small fw-semibold text-dark text-end">{{ item.valeur }}</span>
              </div>
            </div>
          </div>
        </div>

      </template>
      </div>
    </main>
  </div>
</template>

<style scoped>
.db-main { margin-left: 280px; min-width: 0; width: calc(100% - 280px); }
.db-kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
.kpi-card--skeleton { min-height: 90px; background: var(--bs-secondary-bg); }
.db-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1.5rem; }
.db-grid > .card { min-width: 0; }
.db-card--wide { grid-column: 1 / -1; }
.db-bars, .db-activite { padding: 1rem; }
.db-bars { display: flex; flex-direction: column; gap: 0.875rem; }
.db-bar-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(3rem, auto); align-items: center; gap: 0.75rem; }
.db-bar-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.db-bar-label small { color: var(--bs-secondary-color); margin-left: 0.25rem; }
.db-bar-track { height: 0.5rem; }
.db-activite-row { display: flex; align-items: center; gap: 0.75rem; padding: 0.875rem 0; border-bottom: 1px solid var(--bs-border-color); }
.db-activite-row:first-child { padding-top: 0; }
.db-activite-row:last-child { border-bottom: 0; padding-bottom: 0; }
.db-activite-body { flex: 1; min-width: 0; }
.db-activite-op { display: block; font-size: 0.875rem; font-weight: 600; }
.db-activite-meta { font-size: 0.8rem; color: var(--bs-secondary-color); overflow-wrap: anywhere; }
@media (max-width: 1100px) {
  .db-kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .db-grid { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 640px) {
  .db-kpis { grid-template-columns: minmax(0, 1fr); }
  .db-bar-row { grid-template-columns: minmax(0, 1fr) auto; }
  .db-bar-track { grid-column: 1 / -1; grid-row: 2; }
  .db-activite-row { flex-wrap: wrap; }
}
</style>

<template>
  <aside class="sidebar">
    <!-- Brand -->
    <router-link to="/" class="sidebar__brand">
      <span class="sidebar__brand-name">Auximad</span>
      <span v-if="userInfo && userInfo.agence" class="sidebar__badge">
        {{ userInfo.agence }}
      </span>
    </router-link>

    <hr class="sidebar__divider" />

    <!-- Navigation -->
    <nav v-if="isLoggedIn" class="sidebar__nav">
      <router-link to="/" class="sidebar__nav-link" active-class="sidebar__nav-link--active">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path
            d="M8.707 1.5a1 1 0 0 0-1.414 0L.646 8.146a.5.5 0 0 0 .708.708L2 8.207V13.5A1.5 1.5 0 0 0 3.5 15h9a1.5 1.5 0 0 0 1.5-1.5V8.207l.646.647a.5.5 0 0 0 .708-.708L13 5.793V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293zM13 7.207V13.5a.5.5 0 0 1-.5.5h-9a.5.5 0 0 1-.5-.5V7.207l5-5z" />
        </svg>
        Tableau de bord
      </router-link>

      <router-link to="/clients" class="sidebar__nav-link" active-class="sidebar__nav-link--active">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path
            d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1zm-7.978-1L7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002-.014.002zM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4m3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0M6.936 9.28a6 6 0 0 0-1.23-.247A7 7 0 0 0 5 9c-4 0-5 3-5 4q0 1 1 1h4.216A2.24 2.24 0 0 1 5 13c0-1.01.377-2.047 1.09-2.904.243-.294.526-.569.846-.816M4.92 7.01a4 4 0 1 0 0-6 4 4 0 0 0 0 6" />
        </svg>
        Clients
      </router-link>

      <router-link to="/ordres-transit" class="sidebar__nav-link" active-class="sidebar__nav-link--active">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path
            d="M5.5 7a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1zM5 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0 2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5" />
          <path
            d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z" />
        </svg>
        Ordres de Transit
      </router-link>

      <router-link to="/marchandises" class="sidebar__nav-link" active-class="sidebar__nav-link--active">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path
            d="M8.186 1.113a.5.5 0 0 0-.372 0L1.846 3.5l2.404.961L10.404 2zm3.564 1.426L5.596 5 8 5.961 14.154 3.5zm.325 1.283L6 6.136V13.5l6-2.4zm-7.075 2.3L1 3.824v6.865l5 2zm-1.5-.724v5.44L1.8 9.528zM8 7.062 14 4.662v5.438l-6 2.4z" />
        </svg>
        Marchandises
      </router-link>

      <div class="sidebar__nav-group">
        <div class="sidebar__nav-link" @click="toggleDossiers"
          :class="{ 'sidebar__nav-link--active': isDossierActive }">
          <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
            <path
              d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31M2.19 4a1 1 0 0 0-.996 1.09l.637 7a1 1 0 0 0 .995.91h10.348a1 1 0 0 0 .995-.91l.637-7A1 1 0 0 0 13.81 4zm4.69-1.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139h5.17l-.828-.827z" />
          </svg>
          <span style="flex-grow: 1;">Dossiers</span>
          <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__chevron"
            :class="{ 'sidebar__chevron--rotated': dossiersExpanded }">
            <path fill-rule="evenodd"
              d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z" />
          </svg>
        </div>
        <div v-show="dossiersExpanded" class="sidebar__submenu">
          <router-link to="/dossiers" class="sidebar__submenu-link"
            :class="{ 'sidebar__submenu-link--active': isListeDossiersActive }">
            Liste des dossiers
          </router-link>
          <router-link to="/dossiers-interagence" class="sidebar__submenu-link"
            :class="{ 'sidebar__submenu-link--active': isInteragenceActive }">
            Inter-agences
          </router-link>
          <router-link to="/demandes-interagence"
            class="sidebar__submenu-link d-flex justify-content-between align-items-center"
            :class="{ 'sidebar__submenu-link--active': isDemandeInteragenceActive }">
            <span>Demandes interagences</span>
            <span v-if="demandesBadge > 0" class="badge bg-danger rounded-pill ms-1" style="font-size: 0.7rem;">
              {{ demandesBadge }}
            </span>
          </router-link>
          <router-link to="/documents/dossiers"
            class="sidebar__submenu-link d-flex justify-content-between align-items-center"
            :class="{ 'sidebar__submenu-link--active': isDocumentsActive }">
            <span>📄 Documents</span>
            <span v-if="documentsBadge > 0" class="badge bg-warning text-dark rounded-pill ms-1"
              style="font-size: 0.7rem;">
              {{ documentsBadge }}
            </span>
          </router-link>
        </div>
      </div>

      <router-link to="/historiques" class="sidebar__nav-link" active-class="sidebar__nav-link--active">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path
            d="M8.515 1.019A7 7 0 0 0 8 4c-.335 0-.66.026-.98.075a6.993 6.993 0 0 1-.497-3.077A7 7 0 0 0 2.49 4.743a7 7 0 0 0-.497 3.34c-.02-.02-.038-.04-.056-.06A7 7 0 1 0 8.515 1.02zM8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14m0-14.5A7 7 0 0 0 1.5 8c0 .42.037.83.108 1.229.107-.466.27-.91.483-1.325A6.49 6.49 0 0 1 8 1.5z" />
          <path d="M8.5 4.5a.5.5 0 0 0-1 0v3.25a.5.5 0 0 0 .354.354l2.25.75a.5.5 0 0 0 .292-.958L8.5 7.42z" />
        </svg>
        Historiques
      </router-link>
    </nav>

    <hr v-if="isLoggedIn" class="sidebar__divider" />

    <!-- User card -->
    <div v-if="isLoggedIn" class="sidebar__user">
      <span class="sidebar__user-name">
        {{ userInfo?.nomComplet || userInfo?.nomUtilisateur || 'Utilisateur' }}
      </span>
      <span v-if="userInfo?.role" class="sidebar__user-role">
        {{ userInfo?.role }}
      </span>
    </div>

    <!-- Logout -->
    <div v-if="isLoggedIn" class="sidebar__footer">
      <hr class="sidebar__divider" />
      <button @click="handleLogout" class="sidebar__logout-btn">
        <svg viewBox="0 0 16 16" fill="currentColor" class="sidebar__icon">
          <path fill-rule="evenodd"
            d="M10 12.5a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v2a.5.5 0 0 0 1 0v-2A1.5 1.5 0 0 0 9.5 2h-8A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-2a.5.5 0 0 0-1 0z" />
          <path fill-rule="evenodd"
            d="M15.854 8.354a.5.5 0 0 0 0-.708l-3-3a.5.5 0 0 0-.708.708L14.293 7.5H5.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708z" />
        </svg>
        <span>Déconnexion</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import UtilisateurService from '../../services/UtilisateurService'
import DemandeInteragenceService from '../../services/DemandeInteragenceService'
import HistoriqueService from '../../services/HistoriqueService'
import DocumentService from '../../services/DocumentService'

const router = useRouter()
const route = useRoute()
const utilisateur = ref(null)
const dossiersExpanded = ref(false)
const demandesBadge = ref(0)
const documentsBadge = ref(0)
let timer = null

const isLoggedIn = computed(() => UtilisateurService.estConnecte())

const userInfo = computed(() => {
  if (!utilisateur.value) return null
  return UtilisateurService.formaterPourAffichage(utilisateur.value)
})

const chargerUtilisateur = () => {
  utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
}

const chargerBadges = async () => {
  if (isLoggedIn.value) {
    demandesBadge.value = await DemandeInteragenceService.getNonVuesCount()
    // Badge documents : dossiers avec docs incomplets
    try {
      const sansDocs = await DocumentService.getDossiersSansDocuments()
      const remiseUrgente = await DocumentService.getDossiersRemiseUrgente(7)
      // Dédupliquer
      const tous = new Set([...sansDocs.map(d => d.id), ...remiseUrgente.map(d => d.id)])
      documentsBadge.value = tous.size
    } catch (e) {
      documentsBadge.value = 0
    }
  }
}

const toggleDossiers = () => {
  dossiersExpanded.value = !dossiersExpanded.value
}

const isDossierActive = computed(() => {
  return route.path.startsWith('/dossiers') || route.path.startsWith('/demandes-interagence')
})

const isListeDossiersActive = computed(() => {
  return route.path === '/dossiers' && !route.query.typeDossier
})

const isInteragenceActive = computed(() => {
  return route.path === '/dossiers-interagence' || (route.path === '/dossiers' && route.query.typeDossier === 'INTERAGENCE')
})

const isDemandeInteragenceActive = computed(() => {
  return route.path === '/demandes-interagence'
})

const isDocumentsActive = computed(() => {
  return route.path === '/documents/dossiers'
})

onMounted(() => {
  chargerUtilisateur()
  chargerBadges()
  if (route.path.startsWith('/dossiers') || route.path.startsWith('/demandes-interagence')) {
    dossiersExpanded.value = true
  }
  timer = setInterval(chargerBadges, 15000) // Rafraîchir les notifications toutes les 15s
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const handleLogout = async () => {
  // Notifier le serveur (audit de déconnexion) AVANT de supprimer le token
  await HistoriqueService.notifierDeconnexion()
  UtilisateurService.deconnecter()
  router.push('/login')
}
</script>

<style scoped>
/* ── Variables ── */
.sidebar {
  --sb-bg: #ffffff;
  --sb-border: #e2e6ec;
  --sb-text: #1e2a3a;
  --sb-text-muted: #5e6f88;
  --sb-text-light: #8a9aa8;
  --sb-hover-bg: #f2f4f8;
  --sb-active-bg: #e6edf6;
  --sb-active-text: #2c5f8a;
  --sb-primary: #3b6a9c;
  --sb-radius: 8px;
  --sb-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);

  width: 260px;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  background: var(--sb-bg);
  border-right: 1px solid var(--sb-border);
  box-shadow: var(--sb-shadow);
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: var(--sb-text);
  font-size: 14px;
  overflow-y: auto;
  z-index: 1000;
}

/* ── Brand ── */
.sidebar__brand {
  display: flex;
  align-items: center;
  text-decoration: none;
  margin-bottom: 8px;
  gap: 10px;
  flex-wrap: wrap;
}

.sidebar__brand-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--sb-text);
  letter-spacing: -0.3px;
}

.sidebar__badge {
  background: var(--sb-active-bg);
  color: var(--sb-active-text);
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid rgba(59, 106, 156, 0.15);
}

.sidebar__divider {
  margin: 12px 0;
  border: none;
  border-top: 1px solid var(--sb-border);
  opacity: 0.6;
}

/* ── Navigation ── */
.sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1 0 auto;
}

.sidebar__nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--sb-radius);
  color: var(--sb-text-muted);
  text-decoration: none;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
  cursor: pointer;
}

.sidebar__nav-link:hover {
  background: var(--sb-hover-bg);
  color: var(--sb-text);
}

.sidebar__nav-link--active {
  background: var(--sb-active-bg);
  color: var(--sb-active-text);
  font-weight: 600;
}

.sidebar__nav-link--active:hover {
  background: var(--sb-active-bg);
}

.sidebar__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  opacity: 0.7;
}

.sidebar__chevron {
  width: 14px;
  height: 14px;
  opacity: 0.7;
  transition: transform 0.2s ease;
}

.sidebar__chevron--rotated {
  transform: rotate(90deg);
}

.sidebar__submenu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 28px;
  margin-top: 4px;
  margin-bottom: 6px;
}

.sidebar__submenu-link {
  display: block;
  padding: 6px 12px;
  border-radius: var(--sb-radius);
  color: var(--sb-text-muted);
  text-decoration: none;
  font-weight: 500;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
  cursor: pointer;
}

.sidebar__submenu-link:hover {
  background: var(--sb-hover-bg);
  color: var(--sb-text);
}

.sidebar__submenu-link--active {
  background: var(--sb-active-bg);
  color: var(--sb-active-text);
  font-weight: 600;
}

.sidebar__nav-link--active .sidebar__icon {
  opacity: 1;
}

/* ── User card ── */
.sidebar__user {
  background: #f5f7fa;
  border-radius: var(--sb-radius);
  padding: 12px 14px;
  margin-top: 6px;
  border: 1px solid var(--sb-border);
}

.sidebar__user-name {
  display: block;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--sb-text);
}

.sidebar__user-role {
  display: inline-block;
  margin-top: 4px;
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--sb-text-muted);
  background: var(--sb-bg);
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--sb-border);
  line-height: 1.6;
}

/* ── Logout ── */
.sidebar__footer {
  margin-top: auto;
  padding-top: 4px;
}

.sidebar__logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2d6d6;
  border-radius: var(--sb-radius);
  background: transparent;
  color: #8a4b4b;
  font-weight: 500;
  font-size: 0.9rem;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
  cursor: pointer;
  font-family: inherit;
}

.sidebar__logout-btn:hover {
  background: #f8f0f0;
  border-color: #c9a8a8;
  color: #6d3a3a;
}

.sidebar__logout-btn .sidebar__icon {
  opacity: 0.8;
}

/* ── Scroll ── */
.sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #ccd2db;
  border-radius: 4px;
}

.sidebar::-webkit-scrollbar-track {
  background: transparent;
}
</style>
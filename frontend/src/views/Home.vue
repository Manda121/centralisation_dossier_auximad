<script setup>
import Header from '../components/inc/Header.vue'
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UtilisateurService from '../services/UtilisateurService'

const router = useRouter()
const utilisateur = ref(null)

const isLoggedIn = computed(() => UtilisateurService.estConnecte())

const userInfo = computed(() => {
  if (!utilisateur.value) return null
  return UtilisateurService.formaterPourAffichage(utilisateur.value)
})

const chargerUtilisateur = () => {
  utilisateur.value = UtilisateurService.obtenirUtilisateurConnecte()
}

onMounted(chargerUtilisateur)

</script>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />

    <main class="flex-grow-1 p-4" style="margin-left: 280px;">
      <div class="container-fluid">
        <!-- Carte de bienvenue -->
        <div class="card border shadow-sm mb-4">
          <div class="card-body p-5 text-center">
            <h1 class="h4 fw-bold text-dark mb-3">Bienvenue {{ utilisateur }}</h1>
            <p class="text-secondary mb-0">
              Vous êtes connecté au système de centralisation des dossiers. Utilisez la navigation pour gérer vos activités.
            </p>
          </div>
        </div>

        <!-- Grille d'accès rapide -->
        <div class="row g-4">
          <div class="col-md-3 col-sm-6">
            <router-link to="/clients" class="quick-card">
              <div class="quick-card__icon">
                <svg viewBox="0 0 16 16" fill="currentColor">
                  <path d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1zm-7.978-1L7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002-.014.002zM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4m3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0M6.936 9.28a6 6 0 0 0-1.23-.247A7 7 0 0 0 5 9c-4 0-5 3-5 4q0 1 1 1h4.216A2.24 2.24 0 0 1 5 13c0-1.01.377-2.047 1.09-2.904.243-.294.526-.569.846-.816M4.92 7.01a4 4 0 1 0 0-6 4 4 0 0 0 0 6"/>
                </svg>
              </div>
              <span class="quick-card__label">Clients</span>
            </router-link>
          </div>

          <div class="col-md-3 col-sm-6">
            <router-link to="/ordres-transit" class="quick-card">
              <div class="quick-card__icon">
                <svg viewBox="0 0 16 16" fill="currentColor">
                  <path d="M5.5 7a.5.5 0 0 0 0 1h5a.5.5 0 0 0 0-1zM5 9.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0 2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5"/>
                  <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5z"/>
                </svg>
              </div>
              <span class="quick-card__label">Ordres de Transit</span>
            </router-link>
          </div>

          <div class="col-md-3 col-sm-6">
            <router-link to="/marchandises" class="quick-card">
              <div class="quick-card__icon">
                <svg viewBox="0 0 16 16" fill="currentColor">
                  <path d="M8.186 1.113a.5.5 0 0 0-.372 0L1.846 3.5l2.404.961L10.404 2zm3.564 1.426L5.596 5 8 5.961 14.154 3.5zm.325 1.283L6 6.136V13.5l6-2.4zm-7.075 2.3L1 3.824v6.865l5 2zm-1.5-.724v5.44L1.8 9.528zM8 7.062 14 4.662v5.438l-6 2.4z"/>
                </svg>
              </div>
              <span class="quick-card__label">Marchandises</span>
            </router-link>
          </div>

          <div class="col-md-3 col-sm-6">
            <router-link to="/dossiers" class="quick-card">
              <div class="quick-card__icon">
                <svg viewBox="0 0 16 16" fill="currentColor">
                  <path d="M.54 3.87.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31M2.19 4a1 1 0 0 0-.996 1.09l.637 7a1 1 0 0 0 .995.91h10.348a1 1 0 0 0 .995-.91l.637-7A1 1 0 0 0 13.81 4zm4.69-1.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139h5.17l-.828-.827z"/>
                </svg>
              </div>
              <span class="quick-card__label">Dossiers</span>
            </router-link>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ── Cartes d'accès rapide ── */
.quick-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 12px;
  background: #ffffff;
  border: 1px solid #e2e6ec;
  border-radius: 10px;
  text-decoration: none;
  color: #1e2a3a;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  height: 100%;
  min-height: 140px;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
}

.quick-card:hover {
  background: #f5f7fa;
  border-color: #b8c4d0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  color: #1e2a3a;
}

.quick-card__icon {
  width: 48px;
  height: 48px;
  color: #3b6a9c;
  opacity: 0.75;
  margin-bottom: 12px;
}

.quick-card__icon svg {
  width: 100%;
  height: 100%;
}

.quick-card__label {
  font-weight: 600;
  font-size: 1rem;
  text-align: center;
  color: #1e2a3a;
}

/* ── Ajustement responsive ── */
@media (max-width: 576px) {
  .quick-card {
    min-height: 110px;
    padding: 18px 8px;
  }
  .quick-card__icon {
    width: 36px;
    height: 36px;
    margin-bottom: 8px;
  }
  .quick-card__label {
    font-size: 0.9rem;
  }
}
</style>
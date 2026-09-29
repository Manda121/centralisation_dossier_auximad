import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/clients',
      name: 'clients',
      component: () => import('../views/ClientListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/clients/:id',
      name: 'client-detail',
      component: () => import('../views/ClientDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ordres-transit',
      name: 'ordres-transit',
      component: () => import('../views/OrdreTransitListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ordres-transit/creer',
      name: 'ordre-transit-creer',
      component: () => import('../views/OrdreTransitWizardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ordres-transit/:id',
      name: 'ordre-transit-detail',
      component: () => import('../views/OrdreTransitDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/marchandises',
      name: 'marchandises',
      component: () => import('../views/MarchandiseListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/conteneurs',
      name: 'conteneurs',
      component: () => import('../views/ConteneurListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dossiers',
      name: 'dossiers',
      component: () => import('../views/DossierListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dossiers-interagence',
      name: 'dossiers-interagence',
      component: () => import('../views/DossierInteragenceListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dossiers/creer',
      name: 'dossier-creer',
      component: () => import('../views/DossierCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dossiers/:id',
      name: 'dossier-detail',
      component: () => import('../views/DossierDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dossiers/:id/sous-dossier/creer',
      name: 'sous-dossier-creer',
      component: () => import('../views/SousDossierCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/demandes-interagence',
      name: 'demandes-interagence',
      component: () => import('../views/DemandeInteragenceListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/historiques',
      name: 'historiques',
      component: () => import('../views/HistoriqueListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/documents/dossiers',
      name: 'documents-dossiers',
      component: () => import('../views/DocumentDossierView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/mouvements-dossier',
      name: 'mouvements-dossier',
      component: () => import('../views/MvtDossierListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/mouvements-dossier/creer',
      name: 'mouvement-dossier-creer',
      component: () => import('../views/MvtDossierCreateView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true }
    }
  ]
})

// Gardien de route pour sécuriser l'accès
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('auximad_token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)

  if (requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else if (requiresAdmin) {
    // Vérifier le niveau d'autorisation depuis le localStorage
    try {
      const userInfo = JSON.parse(localStorage.getItem('auximad_user_info') || '{}')
      if ((userInfo.niveauAutorisation || 0) < 10000) {
        next('/')  // Rediriger vers home si pas admin
      } else {
        next()
      }
    } catch (e) {
      next('/')
    }
  } else {
    next()
  }
})

export default router

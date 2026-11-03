<script setup>
import { computed } from 'vue'
import Header from '../components/inc/Header.vue'
import authService from '../utils/auth.service'
import PermissionService from '../utils/permission.service'

function lireProfil() {
    try { return authService.getUserInfo() || {} }
    catch { return {} }
}
const profil = lireProfil()
const nom = profil.prenom || profil.nomComplet || profil.nomUtilisateur || ''
const agence = typeof profil.agence === 'object' ? profil.agence?.nom : profil.agence
const peutCreerOt = computed(() => PermissionService.peutEcrireOT())
const estAdmin = computed(() => PermissionService.getNiveauAutorisation() >= 10000)
const dateDuJour = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date())

const modules = [
    { to: '/dossiers', titre: 'Dossiers', description: 'Retrouvez les dossiers et consultez leur avancement.',
        icone: 'M3 5h7l2 2h9v12H3z M3 5V3h7l2 2' },
    { to: '/ordres-transit', titre: 'Ordres de transit', description: 'Consultez les ordres, les clients et les informations de transport.',
        icone: 'M6 3h9l3 3v15H6z M14 3v5h4 M9 12h6 M9 16h6' },
    { to: '/documents/dossiers', titre: 'Documents', description: 'Préparez les pièces nécessaires et enregistrez leurs remises.',
        icone: 'M6 3h9l3 3v15H6z M9 12l2 2 4-4 M9 17h6' },
    { to: '/mouvements-dossier', titre: 'Suivi des opérations', description: 'Consultez les mouvements et mettez à jour le suivi des dossiers.',
        icone: 'M5 5h14 M5 12h14 M5 19h14 M3 5h.01 M3 12h.01 M3 19h.01' },
    { to: '/marchandises', titre: 'Marchandises', description: 'Choisissez un dossier pour gérer les marchandises de son OT.',
        icone: 'M3 7l9-4 9 4v10l-9 4-9-4z M3 7l9 4 9-4 M12 11v10 M8 5l9 4' },
    { to: '/conteneurs', titre: 'Conteneurs', description: 'Consultez et assignez les conteneurs des dossiers maritimes.',
        icone: 'M3 6h18v13H3z M7 9v7 M12 9v7 M17 9v7 M3 4h18' },
]
</script>

<template>
    <div class="home-page">
        <Header />
        <main class="home-main">
            <header class="home-topbar">
                <span class="home-eyebrow">Accueil</span>
                <time class="home-date" :datetime="new Date().toLocaleDateString('en-CA')">{{ dateDuJour }}</time>
            </header>

            <section class="home-welcome" aria-labelledby="home-title">
                <div class="home-welcome__copy">
                    <span class="home-eyebrow">VOTRE ESPACE AUXIMAD</span>
                    <h1 id="home-title">Bienvenue{{ nom ? `, ${nom}` : '' }}.</h1>
                    <p>Retrouvez vos dossiers et choisissez une action pour commencer.</p>
                    <div class="home-welcome__actions">
                        <RouterLink to="/dossiers" class="btn btn-primary">Consulter les dossiers <span aria-hidden="true">→</span></RouterLink>
                        <RouterLink v-if="peutCreerOt" to="/ordres-transit/creer" class="btn btn-outline-primary">Créer un ordre de transit</RouterLink>
                    </div>
                </div>
                <dl v-if="agence || profil.role" class="home-context">
                    <div v-if="agence"><dt>Votre agence</dt><dd>{{ agence }}</dd></div>
                    <div v-if="profil.role"><dt>Votre rôle</dt><dd>{{ profil.role }}</dd></div>
                </dl>
            </section>

            <section aria-labelledby="home-modules-title" class="home-modules">
                <div class="home-section-heading">
                    <h2 id="home-modules-title">Votre espace de travail</h2>
                    <p>Accédez directement aux activités de l’agence.</p>
                </div>
                <div class="home-grid">
                    <RouterLink v-for="module in modules" :key="module.to" :to="module.to" class="home-module">
                        <span class="home-module__icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path :d="module.icone" /></svg>
                        </span>
                        <h3>{{ module.titre }}</h3>
                        <p>{{ module.description }}</p>
                        <span class="home-module__link">Ouvrir <span aria-hidden="true">↗</span></span>
                    </RouterLink>
                </div>
            </section>

            <nav class="home-shortcuts" aria-label="Autres accès rapides">
                <RouterLink to="/demandes-interagence"><span>Demandes interagences</span><span aria-hidden="true">→</span></RouterLink>
                <RouterLink to="/tableau-de-bord"><span>Consulter le tableau de bord</span><span aria-hidden="true">→</span></RouterLink>
                <RouterLink v-if="estAdmin" to="/admin"><span>Configuration système</span><span aria-hidden="true">→</span></RouterLink>
            </nav>
        </main>
    </div>
</template>

<style scoped>
.home-page { min-height: 100vh; background: #f6f8fb; color: #1e2a3a; }
.home-main { margin-left: 260px; min-width: 0; padding: 28px clamp(20px, 3vw, 48px) 40px; }
.home-topbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-bottom: 24px; }
.home-eyebrow { color: #60738d; font-size: .72rem; font-weight: 700; letter-spacing: .1em; text-transform: uppercase; }
.home-date { color: #6a7b91; font-size: .8rem; text-transform: capitalize; }
.home-welcome { display: flex; align-items: center; justify-content: space-between; gap: 2rem; padding: clamp(24px, 3vw, 40px); background: #fff; border: 1px solid #dce5f0; border-top: 3px solid #356493; border-radius: 12px; }
.home-welcome h1 { margin: .6rem 0 .7rem; font-size: clamp(1.65rem, 2.6vw, 2.4rem); font-weight: 700; letter-spacing: -.04em; }
.home-welcome p { max-width: 560px; margin: 0; color: #64748b; line-height: 1.65; font-size: .95rem; }
.home-welcome__actions { display: flex; flex-wrap: wrap; gap: .75rem; margin-top: 1.5rem; }
.home-welcome__actions .btn { font-size: .85rem; padding: .65rem 1rem; }
.home-context { display: grid; gap: 1.1rem; min-width: 140px; margin: 0; padding-left: 2rem; border-left: 1px solid #e3e9f1; }
.home-context dt { margin-bottom: .2rem; font-size: .72rem; color: #6a7b91; font-weight: 400; }
.home-context dd { margin: 0; font-size: .95rem; font-weight: 600; }
.home-modules { margin-top: 32px; }
.home-section-heading { margin-bottom: 18px; }
.home-section-heading h2 { margin: 0 0 .3rem; font-size: 1.1rem; font-weight: 650; }
.home-section-heading p { margin: 0; color: #6a7b91; font-size: .85rem; }
.home-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.home-module { display: flex; flex-direction: column; align-items: flex-start; padding: 24px; border: 1px solid #e0e6ee; border-radius: 10px; background: #fff; color: inherit; text-decoration: none; transition: border-color .15s, box-shadow .15s; }
.home-module:hover { border-color: #9eb6d0; box-shadow: 0 5px 18px #24364a0a; }
.home-module:focus-visible, .home-shortcuts a:focus-visible { outline: 3px solid #6d9dc7; outline-offset: 3px; }
.home-module__icon { display: inline-flex; align-items: center; justify-content: center; width: 42px; height: 42px; margin-bottom: 17px; border-radius: 10px; background: #eef3fa; color: #356493; }
.home-module__icon svg { width: 24px; height: 24px; }
.home-module h3 { margin: 0 0 8px; font-size: 1rem; font-weight: 650; }
.home-module p { margin: 0 0 20px; color: #6a7b91; font-size: .82rem; line-height: 1.6; }
.home-module__link { display: flex; width: 100%; justify-content: space-between; gap: 1rem; margin-top: auto; color: #356493; font-size: .8rem; font-weight: 600; }
.home-shortcuts { display: flex; flex-wrap: wrap; gap: 10px 24px; margin-top: 24px; }
.home-shortcuts a { display: inline-flex; gap: 20px; align-items: center; padding: .5rem 0; color: #526a87; font-size: .82rem; text-decoration: none; }
.home-shortcuts a:hover { color: #244f7c; text-decoration: underline; }
@media (max-width: 1100px) { .home-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .home-welcome { align-items: flex-start; flex-direction: column; gap: 1.5rem; } .home-context { display: flex; gap: 2rem; padding: 0; border: 0; } }
@media (max-width: 700px) { .home-grid { grid-template-columns: minmax(0, 1fr); } .home-module { padding: 20px; } .home-welcome__actions .btn { white-space: normal; } }
</style>

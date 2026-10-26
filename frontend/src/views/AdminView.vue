<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="admin-main flex-grow-1">
            <header class="admin-topbar">
                <div class="admin-topbar__inner">
                    <div class="admin-topbar__title">
                        <span class="admin-topbar__icon" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                                stroke-linecap="round" stroke-linejoin="round">
                                <path d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z" />
                                <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-1.86 1.86-.06-.06A1.7 1.7 0 0 0 16 18.4a1.7 1.7 0 0 0-1 1.57V20h-2.64v-.09a1.7 1.7 0 0 0-1-1.57 1.7 1.7 0 0 0-1.88.34l-.06.06-1.86-1.86.06-.06A1.7 1.7 0 0 0 7.96 15a1.7 1.7 0 0 0-1.57-1H6.3v-2.64h.09a1.7 1.7 0 0 0 1.57-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 1.86-1.86.06.06A1.7 1.7 0 0 0 11.36 7a1.7 1.7 0 0 0 1-1.57V5.3H15v.09A1.7 1.7 0 0 0 16 6.96a1.7 1.7 0 0 0 1.88-.34l.06-.06 1.86 1.86-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.57 1h.09V14h-.09A1.7 1.7 0 0 0 19.4 15Z" transform="translate(-1 -1)" />
                            </svg>
                        </span>
                        <div>
                            <span class="admin-topbar__eyebrow">ADMINISTRATION</span>
                            <h1 class="admin-topbar__heading">Configuration système</h1>
                            <p class="admin-topbar__sub">Gérez les accès, les référentiels et les opérations.</p>
                        </div>
                    </div>
                    <div class="admin-topbar__badge">
                        <span class="badge-admin">Espace administrateur</span>
                    </div>
                </div>
            </header>

            <div class="admin-body">
                <div class="admin-layout">
                    <aside class="admin-sidebar">
                        <div class="admin-sidebar__intro">
                            <span>Paramètres</span>
                            <span class="admin-sidebar__count">{{ totalParametres }}</span>
                        </div>
                        <label for="admin-menu-search" class="visually-hidden">Rechercher un paramètre</label>
                        <input id="admin-menu-search" v-model="menuSearch" type="search"
                            class="admin-sidebar__search" placeholder="Rechercher un paramètre..." />
                        <nav aria-label="Sections de configuration">
                            <div v-for="section in filteredSections" :key="section.id" class="admin-sidebar__section">
                                <div class="admin-sidebar__section-title">{{ section.label }}</div>
                                <button v-for="item in section.items" :key="item.key" type="button"
                                    class="admin-sidebar__item"
                                    :class="{ 'admin-sidebar__item--active': activeKey === item.key }"
                                    :aria-current="activeKey === item.key ? 'page' : undefined"
                                    @click="setActive(item)">
                                    <span class="admin-sidebar__item-icon" aria-hidden="true">{{ item.label.slice(0, 2).toUpperCase() }}</span>
                                    <span class="admin-sidebar__item-label">{{ item.label }}</span>
                                </button>
                            </div>
                            <p v-if="filteredSections.length === 0" class="admin-sidebar__empty">
                                Aucun paramètre correspondant.
                            </p>
                        </nav>
                    </aside>

                    <section class="admin-content">
                        <OperationDossierAdminView v-if="activeKey === 'operationsDossier'" />

                        <div v-else-if="!activeConfig" class="admin-welcome">
                            <div class="admin-welcome__intro">
                                <span class="admin-eyebrow">VUE D’ENSEMBLE</span>
                                <h2>Paramètres de l’application</h2>
                                <p>Sélectionnez une rubrique pour gérer les données de référence et les accès.</p>
                            </div>
                            <div class="admin-welcome__grid">
                                <button v-for="(section, index) in sections" :key="section.id" type="button"
                                    class="admin-welcome__card" @click="setActive(section.items[0])">
                                    <span class="admin-welcome__number">{{ String(index + 1).padStart(2, '0') }}</span>
                                    <span class="admin-welcome__card-title">{{ section.label }}</span>
                                    <span class="admin-welcome__card-meta">{{ section.items.length }} paramètres <span aria-hidden="true">→</span></span>
                                </button>
                            </div>
                        </div>

                        <template v-else>
                            <!-- En-tête de la section active -->
                            <div class="crud-header">
                                <div class="crud-header__copy">
                                    <span class="admin-eyebrow">{{ activeSectionLabel }}</span>
                                    <h2 class="crud-header__title">{{ activeConfig.label }}</h2>
                                    <p class="crud-header__desc">{{ activeConfig.description }}</p>
                                </div>
                                <div class="crud-header__actions">
                                    <ExportActions :title="activeConfig.label" :columns="activeConfig.columns"
                                        :load-rows="lignesExportAdmin" :disabled="loading || !rows.length" />
                                    <button v-if="!activeConfig.readOnly && activeKey !== 'autorisations'"
                                        type="button" class="btn-add" @click="openCreate">
                                        <span aria-hidden="true">＋</span> Nouveau
                                    </button>
                                </div>
                            </div>

                            <!-- Table de données -->
                            <div class="crud-card">
                                <div class="crud-card__summary">
                                    <span>Enregistrements</span>
                                    <strong>{{ rows.length }}</strong>
                                </div>
                                <div v-if="loading" class="crud-loading">
                                    <div class="spinner"></div>
                                    Chargement...
                                </div>
                                <div v-else-if="error" class="crud-error">
                                    {{ error }}
                                </div>
                                <div v-else-if="rows.length === 0" class="crud-empty">
                                    Aucun enregistrement trouvé.
                                </div>
                                <div v-else class="table-wrapper">
                                    <table class="crud-table">
                                        <thead>
                                            <tr>
                                                <th v-for="col in activeConfig.columns" :key="col.key">{{ col.label }}
                                                </th>
                                                <th class="col-actions">Actions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr v-for="row in rows" :key="row.id">
                                                <td v-for="col in activeConfig.columns" :key="col.key">
                                                    <span v-if="col.type === 'badge'" class="td-badge">{{
                                                        getCellValue(row, col) }}</span>
                                                    <span v-else-if="col.type === 'bool'">
                                                        <span :class="getCellValue(row, col) ? 'bool-yes' : 'bool-no'">
                                                            {{ getCellValue(row, col) ? '✓ Oui' : '✗ Non' }}
                                                        </span>
                                                    </span>
                                                    <span v-else>{{ getCellValue(row, col) || '—' }}</span>
                                                </td>
                                                <td class="col-actions">
                                                    <button v-if="!activeConfig.readOnly" type="button" class="btn-edit"
                                                        :aria-label="'Modifier ' + activeConfig.label" @click="openEdit(row)">Modifier</button>
                                                    <button v-if="!activeConfig.readOnly && activeKey !== 'autorisations'"
                                                        type="button" class="btn-delete" :disabled="saving"
                                                        :aria-label="'Supprimer ' + activeConfig.label" @click="confirmerSuppression(row)">Supprimer</button>
                                                    <!-- Actions spéciales utilisateurs -->
                                                    <template v-if="activeKey === 'utilisateurs'">
                                                        <button type="button" class="btn-key" @click="openResetMdp(row)"
                                                            title="Réinitialiser le mot de passe">Mot de passe</button>
                                                    </template>
                                                    <!-- Débloquer pour suspensions -->
                                                    <template v-if="activeKey === 'suspensions' && row.estSuspendu">
                                                        <button type="button" class="btn-unlock" @click="debloquer(row)"
                                                            title="Débloquer le compte">Débloquer</button>
                                                    </template>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </template>
                    </section>
                </div>
            </div>
        </main>

        <!-- Modal Créer / Modifier -->
        <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
            <div class="modal-box">
                <div class="modal-header">
                    <h3>{{ editMode ? 'Modifier' : 'Créer' }} — {{ activeConfig?.label }}</h3>
                    <button class="modal-close" @click="closeModal">✕</button>
                </div>
                <div class="modal-body">
                    <div v-for="field in activeConfig?.fields" :key="field.key" class="form-group">
                        <label>{{ field.label }}<span v-if="field.required" class="req">*</span></label>

                        <!-- Select pour les FK -->
                        <select v-if="field.type === 'select'" v-model="form[field.key]" class="form-control">
                            <option value="">-- Sélectionner --</option>
                            <option v-for="opt in getSelectOptions(field)" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                            </option>
                        </select>

                        <!-- Checkbox -->
                        <div v-else-if="field.type === 'checkbox'" class="form-check">
                            <input type="checkbox" v-model="form[field.key]" :id="'chk-' + field.key"
                                class="form-check-input" />
                            <label :for="'chk-' + field.key" class="form-check-label">{{ field.label }}</label>
                        </div>

                        <!-- Textarea -->
                        <textarea v-else-if="field.type === 'textarea'" v-model="form[field.key]" class="form-control"
                            rows="3" :placeholder="field.placeholder || ''"></textarea>

                        <!-- Number -->
                        <input v-else-if="field.type === 'number'" type="number" v-model.number="form[field.key]"
                            class="form-control" :placeholder="field.placeholder || ''" />

                        <!-- Password -->
                        <input v-else-if="field.type === 'password'" type="password" v-model="form[field.key]"
                            class="form-control" :placeholder="field.placeholder || ''" />

                        <!-- Text par défaut -->
                        <input v-else type="text" v-model="form[field.key]" class="form-control"
                            :placeholder="field.placeholder || ''" />
                    </div>

                    <div v-if="formError" class="form-error">{{ formError }}</div>
                </div>
                <div class="modal-footer">
                    <button class="btn-cancel" @click="closeModal">Annuler</button>
                    <button class="btn-save" @click="sauvegarder" :disabled="saving">
                        <span v-if="saving" class="spinner-sm"></span>
                        {{ saving ? 'Enregistrement...' : 'Enregistrer' }}
                    </button>
                </div>
            </div>
        </div>

        <!-- Modal Reset MDP -->
        <div v-if="showResetMdp" class="modal-overlay" @click.self="showResetMdp = false">
            <div class="modal-box modal-box--sm">
                <div class="modal-header">
                    <h3>Réinitialiser le mot de passe</h3>
                    <button class="modal-close" @click="showResetMdp = false">✕</button>
                </div>
                <div class="modal-body">
                    <p>Utilisateur : <strong>{{ resetTarget?.nomUtilisateur }}</strong></p>
                    <div class="form-group">
                        <label>Nouveau mot de passe <span class="req">*</span></label>
                        <input type="password" v-model="newMdp" class="form-control" placeholder="Min. 6 caractères" />
                    </div>
                    <div v-if="formError" class="form-error">{{ formError }}</div>
                </div>
                <div class="modal-footer">
                    <button class="btn-cancel" @click="showResetMdp = false">Annuler</button>
                    <button class="btn-save" @click="reinitialiserMdp" :disabled="saving">
                        {{ saving ? 'Enregistrement...' : 'Réinitialiser' }}
                    </button>
                </div>
            </div>
        </div>

    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import Header from '../components/inc/Header.vue'
import ExportActions from '../components/inc/ExportActions.vue'
import OperationDossierAdminView from './OperationDossierAdminView.vue'
import AdminService from '../services/AdminService'
import authService from '../utils/auth.service'
import { useNotification } from '../composables/useNotification'

// ── Vérification admin ─────────────────────────────────────────────────────
const userInfo = authService.getUserInfo()
const niveauAutorisation = userInfo?.niveauAutorisation || 0

// ── État global ────────────────────────────────────────────────────────────
const activeKey = ref(null)
const activeConfig = ref(null)
const rows = ref([])
const menuSearch = ref('')
async function lignesExportAdmin() { return rows.value }
const loading = ref(false)
const error = ref(null)
const saving = ref(false)
const { notifySuccess, notifyError } = useNotification()

// Modals
const showModal = ref(false)
const showResetMdp = ref(false)
const editMode = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)
const resetTarget = ref(null)
const form = ref({})
const formError = ref(null)
const newMdp = ref('')

// ── Référentiels locaux (pour les selects FK) ─────────────────────────────
const refRoles = ref([])
const refAgences = ref([])
const refModesTransport = ref([])
const refPays = ref([])
const refCompagnies = ref([])
const refTypesProduction = ref([])
const refNaturesMarchandise = ref([])

// Charger les référentiels nécessaires aux FK
const chargerReferentiels = async () => {
    try {
        const [roles, agences, mt, pays, comp, tp, nm] = await Promise.allSettled([
            AdminService.roles.lister(),
            AdminService.agences.lister(),
            AdminService.modesTransport.lister(),
            AdminService.pays.lister(),
            AdminService.compagnies.lister(),
            AdminService.typesProduction.lister(),
            AdminService.naturesMarchandise.lister(),
        ])
        if (roles.status === 'fulfilled') refRoles.value = roles.value
        if (agences.status === 'fulfilled') refAgences.value = agences.value
        if (mt.status === 'fulfilled') refModesTransport.value = mt.value
        if (pays.status === 'fulfilled') refPays.value = pays.value
        if (comp.status === 'fulfilled') refCompagnies.value = comp.value
        if (tp.status === 'fulfilled') refTypesProduction.value = tp.value
        if (nm.status === 'fulfilled') refNaturesMarchandise.value = nm.value
    } catch (e) { /* silent */ }
}

onMounted(chargerReferentiels)

// ── Configuration de toutes les entités ──────────────────────────────────
const CONFIGS = {
    // ─── 1. Utilisateurs & Sécurité ────────────────────────────────────────
    utilisateurs: {
        key: 'utilisateurs',
        icon: '👤',
        label: 'Utilisateurs',
        description: 'Créer, modifier, désactiver un compte utilisateur',
        service: AdminService.utilisateurs,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nomUtilisateur', label: 'Login' },
            { key: 'prenom', label: 'Prénom' },
            { key: 'nom', label: 'Nom' },
            { key: 'email', label: 'Email' },
            { key: 'role.nom', label: 'Rôle', type: 'badge' },
            { key: 'agence.nom', label: 'Agence', type: 'badge' },
        ],
        fields: [
            { key: 'nomUtilisateur', label: 'Nom d\'utilisateur', required: true },
            { key: 'prenom', label: 'Prénom', required: true },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'email', label: 'Email', required: true },
            { key: 'motDePasse', label: 'Mot de passe', type: 'password', placeholder: 'Laisser vide = inchangé' },
            { key: 'idRole', label: 'Rôle', type: 'select', required: true, options: 'roles' },
            { key: 'idAgence', label: 'Agence', type: 'select', required: true, options: 'agences' },
            { key: 'niveauAutorisation', label: 'Niveau autorisation', type: 'number', placeholder: 'ex: 1000' },
        ],
    },
    roles: {
        key: 'roles',
        icon: '🎭',
        label: 'Rôles',
        description: 'Définir les rôles disponibles dans le système',
        service: AdminService.roles,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Nom du rôle', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    autorisations: {
        key: 'autorisations',
        icon: '🔐',
        label: 'Autorisations',
        description: 'Gérer le niveau d\'autorisation de chaque utilisateur',
        service: AdminService.autorisations,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'utilisateur.nomUtilisateur', label: 'Utilisateur' },
            { key: 'niveauAutorisation', label: 'Niveau', type: 'badge' },
        ],
        fields: [
            { key: 'niveauAutorisation', label: 'Niveau d\'autorisation', type: 'number', required: true, placeholder: 'ex: 10000' },
        ],
        customSave: async (form, editTarget) => {
            const idUtilisateur = editTarget?.utilisateur?.id
            if (!idUtilisateur) throw new Error('Utilisateur introuvable')
            return AdminService.autorisations.modifier(idUtilisateur, form.niveauAutorisation)
        }
    },
    suspensions: {
        key: 'suspensions',
        icon: '🔒',
        label: 'Suspensions',
        description: 'Voir et débloquer les comptes suspendus',
        service: AdminService.suspensions,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'utilisateur.nomUtilisateur', label: 'Utilisateur' },
            { key: 'tentativesEchouees', label: 'Tentatives' },
            { key: 'estSuspendu', label: 'Suspendu ?', type: 'bool' },
        ],
        fields: [],
        readOnly: true,
    },
    actions: {
        key: 'actions',
        icon: '📋',
        label: 'Actions',
        description: 'Liste des actions traçables dans les historiques',
        service: AdminService.actions,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Code action', required: true, placeholder: 'ex: connexion' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },

    // ─── 2. Structure organisationnelle ──────────────────────────────────────
    agences: {
        key: 'agences',
        icon: '🏢',
        label: 'Agences',
        description: 'Gestion des agences (code, nom, adresse)',
        service: AdminService.agences,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'code', label: 'Code' },
            { key: 'nom', label: 'Nom' },
            { key: 'ville', label: 'Ville' },
            { key: 'adresse', label: 'Adresse' },
        ],
        fields: [
            { key: 'code', label: 'Code agence', type: 'number', required: true },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'ville', label: 'Ville', required: true },
            { key: 'adresse', label: 'Adresse', required: true },
            { key: 'cpClient', label: 'CP Client' },
        ],
    },
    bureaux: {
        key: 'bureaux',
        icon: '🏛️',
        label: 'Bureaux de Douane',
        description: 'Gestion des bureaux douaniers (code, nom, adresse)',
        service: AdminService.bureaux,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'codeBureau', label: 'Code' },
            { key: 'nom', label: 'Nom' },
            { key: 'ville', label: 'Ville' },
        ],
        fields: [
            { key: 'codeBureau', label: 'Code bureau', required: true },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'ville', label: 'Ville' },
            { key: 'adresse', label: 'Adresse', type: 'textarea' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },

    // ─── 3. Référentiels métier ───────────────────────────────────────────────
    modesTransport: {
        key: 'modesTransport',
        icon: '🚢',
        label: 'Modes de Transport',
        description: 'Maritime, aérien, terrestre...',
        service: AdminService.modesTransport,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true, placeholder: 'ex: Maritime' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    typesProduction: {
        key: 'typesProduction',
        icon: '🏭',
        label: 'Types de Production',
        description: 'Import, export, transit...',
        service: AdminService.typesProduction,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'code', label: 'Code' },
            { key: 'nom', label: 'Nom' },
            { key: 'modeTransport.nom', label: 'Mode Transport', type: 'badge' },
        ],
        fields: [
            { key: 'code', label: 'Code', type: 'number', required: true },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'idModeTransport', label: 'Mode de Transport', type: 'select', options: 'modesTransport' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    typesRegime: {
        key: 'typesRegime',
        icon: '📜',
        label: 'Types de Régime Douanier',
        description: 'Régimes douaniers applicables',
        service: AdminService.typesRegime,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'code', label: 'Code' },
            { key: 'nom', label: 'Nom' },
        ],
        fields: [
            { key: 'code', label: 'Code', required: true, placeholder: 'ex: 4000' },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    phasesDossier: {
        key: 'phasesDossier',
        icon: '📊',
        label: 'Phases de Dossier',
        description: 'Étapes du cycle de vie d\'un dossier',
        service: AdminService.phasesDossier,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'modeTransport.nom', label: 'Mode Transport', type: 'badge' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true },
            { key: 'idModeTransport', label: 'Mode de Transport', type: 'select', options: 'modesTransport' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    statutsDossier: {
        key: 'statutsDossier',
        icon: '🏷️',
        label: 'Statuts de Dossier',
        description: 'Statuts possibles d\'un dossier',
        service: AdminService.statutsDossier,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    statutsOt: {
        key: 'statutsOt',
        icon: '📌',
        label: 'Statuts d\'OT',
        description: 'Statuts des Ordres de Transit',
        service: AdminService.statutsOt,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    incotermes: {
        key: 'incotermes',
        icon: '🤝',
        label: 'Incotermes',
        description: 'Terms commerciaux internationaux (EXW, FOB, CIF...)',
        service: AdminService.incotermes,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'abreviation', label: 'Abr.', type: 'badge' },
            { key: 'nom', label: 'Nom' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'abreviation', label: 'Abréviation', required: true, placeholder: 'ex: FOB' },
            { key: 'nom', label: 'Nom complet', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    naturesMarchandise: {
        key: 'naturesMarchandise',
        icon: '📦',
        label: 'Natures de Marchandise',
        description: 'Catégories de marchandises transportées',
        service: AdminService.naturesMarchandise,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true },
        ],
    },
    unitesMesure: {
        key: 'unitesMesure',
        icon: '⚖️',
        label: 'Unités de Mesure',
        description: 'Unités utilisées pour les marchandises et conteneurs',
        service: AdminService.unitesMesure,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'rapportMCube', label: 'Rapport m³' },
            { key: 'description', label: 'Description' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true, placeholder: 'ex: Mètre cube' },
            { key: 'rapportMCube', label: 'Rapport en m³', type: 'number', required: true },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    typesContainer: {
        key: 'typesContainer',
        icon: '📫',
        label: 'Types de Conteneur',
        description: 'Types avec dimensions et poids max',
        service: AdminService.typesContainer,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'volume', label: 'Volume (m³)' },
            { key: 'poidsMax', label: 'Poids max (t)' },
        ],
        fields: [
            { key: 'nom', label: 'Nom', required: true, placeholder: 'ex: 20 pieds standard' },
            { key: 'description', label: 'Description', type: 'textarea' },
            { key: 'longueur', label: 'Longueur (m)', type: 'number' },
            { key: 'largeur', label: 'Largeur (m)', type: 'number' },
            { key: 'hauteur', label: 'Hauteur (m)', type: 'number' },
            { key: 'poidsMax', label: 'Poids max (T)', type: 'number' },
            { key: 'volume', label: 'Volume (m³)', type: 'number' },
        ],
    },

    // ─── 4. Documents ─────────────────────────────────────────────────────────
    typesDocument: {
        key: 'typesDocument',
        icon: '📄',
        label: 'Types de Document',
        description: 'Documents requis par type de production / nature',
        service: AdminService.typesDocument,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'estObligatoire', label: 'Obligatoire ?', type: 'bool' },
            { key: 'typeProduction.nom', label: 'Type Production', type: 'badge' },
            { key: 'natureMarchandise.nom', label: 'Nature Marc.', type: 'badge' },
        ],
        fields: [
            { key: 'nom', label: 'Nom du document', required: true },
            { key: 'estObligatoire', label: 'Obligatoire', type: 'checkbox' },
            { key: 'idTypeProduction', label: 'Type de Production', type: 'select', options: 'typesProduction' },
            { key: 'idNatureMarchandise', label: 'Nature de Marchandise', type: 'select', options: 'naturesMarchandise' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },

    // ─── 5. Tiers / annuaires ─────────────────────────────────────────────────
    compagnies: {
        key: 'compagnies',
        icon: '🚀',
        label: 'Compagnies',
        description: 'Compagnies de transport maritime / aérien',
        service: AdminService.compagnies,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'code', label: 'Code' },
            { key: 'nom', label: 'Nom' },
            { key: 'pays.nom', label: 'Pays', type: 'badge' },
            { key: 'modeTransport.nom', label: 'Mode', type: 'badge' },
        ],
        fields: [
            { key: 'code', label: 'Code', required: true, placeholder: 'ex: MSC' },
            { key: 'nom', label: 'Nom', required: true },
            { key: 'idPays', label: 'Pays', type: 'select', options: 'pays' },
            { key: 'idModeTransport', label: 'Mode de Transport', type: 'select', options: 'modesTransport' },
            { key: 'description', label: 'Description', type: 'textarea' },
        ],
    },
    navires: {
        key: 'navires',
        icon: '⛵',
        label: 'Navires',
        description: 'Flotte de navires enregistrés',
        service: AdminService.navires,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'nom', label: 'Nom' },
            { key: 'imo', label: 'IMO' },
            { key: 'compagnie.nom', label: 'Compagnie', type: 'badge' },
        ],
        fields: [
            { key: 'nom', label: 'Nom du navire', required: true },
            { key: 'imo', label: 'Numéro IMO', placeholder: 'ex: IMO1234567' },
            { key: 'idCompagnie', label: 'Compagnie', type: 'select', options: 'compagnies' },
        ],
    },
    pays: {
        key: 'pays',
        icon: '🌍',
        label: 'Pays',
        description: 'Référentiel des pays',
        service: AdminService.pays,
        columns: [
            { key: 'id', label: 'ID' },
            { key: 'code', label: 'Code ISO', type: 'badge' },
            { key: 'nom', label: 'Nom' },
        ],
        fields: [
            { key: 'code', label: 'Code ISO (3 lettres)', required: true, placeholder: 'ex: MDG' },
            { key: 'nom', label: 'Nom du pays', required: true },
        ],
    },
    operationsDossier: {
        key: 'operationsDossier',
        icon: '⚡',
        label: 'Opérations Dossier',
        description: 'Configuration du workflow des opérations et des statuts par type de production',
        service: null,
        columns: [],
        fields: []
    }
}

// ── Structure du menu sidebar ─────────────────────────────────────────────
const sections = [
    {
        id: 'securite',
        label: 'Utilisateurs et sécurité',
        items: [
            CONFIGS.utilisateurs,
            CONFIGS.roles,
            CONFIGS.autorisations,
            CONFIGS.suspensions,
            CONFIGS.actions,
        ],
    },
    {
        id: 'structure',
        label: 'Structure organisationnelle',
        items: [CONFIGS.agences, CONFIGS.bureaux],
    },
    {
        id: 'referentiels',
        label: 'Référentiels métier',
        items: [
            CONFIGS.modesTransport,
            CONFIGS.typesProduction,
            CONFIGS.operationsDossier,
            CONFIGS.typesRegime,
            CONFIGS.phasesDossier,
            CONFIGS.statutsDossier,
            CONFIGS.statutsOt,
            CONFIGS.incotermes,
            CONFIGS.naturesMarchandise,
            CONFIGS.unitesMesure,
            CONFIGS.typesContainer,
        ],
    },
    {
        id: 'documents',
        label: 'Documents',
        items: [CONFIGS.typesDocument],
    },
    {
        id: 'tiers',
        label: 'Tiers et annuaires',
        items: [CONFIGS.compagnies, CONFIGS.navires, CONFIGS.pays],
    },
]

const totalParametres = sections.reduce((total, section) => total + section.items.length, 0)
const normaliserRecherche = valeur => String(valeur ?? '')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const filteredSections = computed(() => {
    const terme = normaliserRecherche(menuSearch.value.trim())
    if (!terme) return sections
    return sections.map(section => ({
        ...section,
        items: section.items.filter(item =>
            normaliserRecherche(item.label).includes(terme)
            || normaliserRecherche(item.description).includes(terme)
        )
    })).filter(section => section.items.length > 0)
})
const activeSectionLabel = computed(() =>
    sections.find(section => section.items.some(item => item.key === activeKey.value))?.label || 'Configuration'
)

// ── Navigation ────────────────────────────────────────────────────────────
const setActive = async (config) => {
    activeKey.value = config.key
    activeConfig.value = config
    rows.value = []
    error.value = null
    await chargerDonnees()
}

const chargerDonnees = async () => {
    if (!activeConfig.value || !activeConfig.value.service) return
    loading.value = true
    try {
        rows.value = await activeConfig.value.service.lister()
    } catch (e) {
        error.value = e.response?.data?.erreur || 'Erreur de chargement'
    } finally {
        loading.value = false
    }
}

// ── Récupération cellule imbriquée ────────────────────────────────────────
const getCellValue = (row, col) => {
    const keys = col.key.split('.')
    let val = row
    for (const k of keys) {
        if (val == null) return null
        val = val[k]
    }
    return val
}

// ── Select options ─────────────────────────────────────────────────────────
const getSelectOptions = (field) => {
    const maps = {
        roles: refRoles.value.map(r => ({ value: r.id, label: r.nom })),
        agences: refAgences.value.map(a => ({ value: a.id, label: `${a.code} — ${a.nom}` })),
        modesTransport: refModesTransport.value.map(m => ({ value: m.id, label: m.nom })),
        pays: refPays.value.map(p => ({ value: p.id, label: `${p.code} — ${p.nom}` })),
        compagnies: refCompagnies.value.map(c => ({ value: c.id, label: c.nom })),
        typesProduction: refTypesProduction.value.map(t => ({ value: t.id, label: t.nom })),
        naturesMarchandise: refNaturesMarchandise.value.map(n => ({ value: n.id, label: n.nom })),
    }
    return maps[field.options] || []
}

// ── Créer ─────────────────────────────────────────────────────────────────
const openCreate = () => {
    editMode.value = false
    editTarget.value = null
    form.value = {}
    formError.value = null
    // Valeurs par défaut
    activeConfig.value.fields.forEach(f => {
        if (f.type === 'checkbox') form.value[f.key] = false
        if (f.type === 'number') form.value[f.key] = null
    })
    showModal.value = true
}

// ── Modifier ──────────────────────────────────────────────────────────────
const openEdit = (row) => {
    editMode.value = true
    editTarget.value = row
    form.value = {}
    formError.value = null
    // Pré-remplir les champs
    activeConfig.value.fields.forEach(f => {
        if (f.type === 'select') {
            let fieldProp = f.key
            if (fieldProp.startsWith('id')) {
                fieldProp = fieldProp.substring(2)
                fieldProp = fieldProp.charAt(0).toLowerCase() + fieldProp.slice(1)
            }
            const obj = row[fieldProp]
            form.value[f.key] = row[f.key] ?? (obj?.id ?? '')
        } else if (f.type === 'password') {
            form.value[f.key] = '' // Ne pas pré-remplir le mot de passe
        } else {
            form.value[f.key] = row[f.key] ?? null
        }
    })
    // Cas spécial: niveauAutorisation dans autorisation
    if (activeKey.value === 'autorisations') {
        form.value['niveauAutorisation'] = row['niveauAutorisation']
    }
    showModal.value = true
}

// ── Sauvegarder ───────────────────────────────────────────────────────────
const sauvegarder = async () => {
    formError.value = null
    // Validation basique
    for (const field of activeConfig.value.fields) {
        if (field.required && !form.value[field.key] && form.value[field.key] !== 0) {
            formError.value = `Le champ "${field.label}" est requis`
            return
        }
    }
    saving.value = true
    try {
        if (activeConfig.value.customSave) {
            await activeConfig.value.customSave(form.value, editTarget.value)
        } else if (editMode.value) {
            await activeConfig.value.service.modifier(editTarget.value.id, form.value)
        } else {
            await activeConfig.value.service.creer(form.value)
        }
        showModal.value = false
        await chargerDonnees()
        afficherToast(editMode.value ? 'Modification enregistrée ✓' : 'Créé avec succès ✓')
    } catch (e) {
        formError.value = e.response?.data?.erreur || e.message || 'Erreur lors de l\'enregistrement'
    } finally {
        saving.value = false
    }
}

const closeModal = () => {
    showModal.value = false
    formError.value = null
}

// ── Suppression ───────────────────────────────────────────────────────────
const confirmerSuppression = async (row) => {
    if (saving.value) return
    deleteTarget.value = row
    await supprimerConfirme()
}

const supprimerConfirme = async () => {
    saving.value = true
    try {
        await activeConfig.value.service.supprimer(deleteTarget.value.id)
        await chargerDonnees()
        afficherToast('Suppression effectuée ✓')
    } catch (e) {
        if (e?.code === 'ERR_CANCELED') return
        afficherToast(e.response?.data || 'Erreur lors de la suppression.', 'error')
    } finally {
        saving.value = false
    }
}

// ── Réinitialiser MDP ─────────────────────────────────────────────────────
const openResetMdp = (row) => {
    resetTarget.value = row
    newMdp.value = ''
    formError.value = null
    showResetMdp.value = true
}

const reinitialiserMdp = async () => {
    if (!newMdp.value || newMdp.value.length < 6) {
        formError.value = 'Le mot de passe doit contenir au moins 6 caractères'
        return
    }
    saving.value = true
    try {
        await AdminService.utilisateurs.reinitialiserMdp(resetTarget.value.id, newMdp.value)
        showResetMdp.value = false
        afficherToast('Mot de passe réinitialisé ✓')
    } catch (e) {
        formError.value = e.response?.data?.erreur || 'Erreur'
    } finally {
        saving.value = false
    }
}

// ── Débloquer compte ──────────────────────────────────────────────────────
const debloquer = async (row) => {
    try {
        await AdminService.suspensions.debloquer(row.utilisateur?.id || row.id)
        await chargerDonnees()
        afficherToast('Compte débloqué ✓')
    } catch (e) {
        afficherToast(e.response?.data || 'Impossible de débloquer ce compte.', 'error')
    }
}

// ── Toast ─────────────────────────────────────────────────────────────────
const afficherToast = (message, type = 'success') => {
    if (type === 'error') notifyError(message)
    else notifySuccess(message)
}
</script>

<style scoped>
.admin-main {
    margin-left: 260px;
    min-height: 100vh;
    background: #f5f7fa;
    color: #1e2a3a;
}

.admin-topbar {
    background: #ffffff;
    border-bottom: 1px solid #e2e6ec;
    padding: 0 28px;
}

.admin-topbar__inner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
    min-height: 96px;
    max-width: 1600px;
    margin: 0 auto;
}

.admin-topbar__title {
    display: flex;
    align-items: center;
    gap: 16px;
}

.admin-topbar__icon {
    display: grid;
    place-items: center;
    width: 46px;
    height: 46px;
    flex: none;
    border-radius: 12px;
    background: #e6edf6;
    color: #2c5f8a;
}

.admin-topbar__icon svg {
    width: 25px;
    height: 25px;
}

.admin-topbar__eyebrow,
.admin-eyebrow {
    display: block;
    margin-bottom: 5px;
    color: #3b6a9c;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.11em;
    text-transform: uppercase;
}

.admin-topbar__heading {
    margin: 0 0 2px;
    color: #1e2a3a;
    font-size: 1.45rem;
    font-weight: 700;
    letter-spacing: -0.025em;
}

.admin-topbar__sub {
    margin: 0;
    color: #5e6f88;
    font-size: 0.86rem;
}

.badge-admin {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: #f3f7fc;
    color: #2c5f8a;
    border: 1px solid #dce6f1;
    padding: 7px 13px;
    border-radius: 999px;
    font-size: 0.78rem;
    font-weight: 600;
}

.badge-admin::before {
    content: "";
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #3b6a9c;
}

.admin-body {
    max-width: 1656px;
    margin: 0 auto;
    padding: 28px;
}

.admin-layout {
    display: grid;
    grid-template-columns: 260px minmax(0, 1fr);
    align-items: start;
    gap: 24px;
}

.admin-sidebar {
    background: white;
    border: 1px solid #e2e6ec;
    border-radius: 12px;
    box-shadow: 0 2px 10px rgba(30, 42, 58, 0.035);
    padding: 18px 10px 12px;
    height: fit-content;
    position: sticky;
    top: 24px;
    overflow-y: auto;
    max-height: calc(100vh - 48px);
}

.admin-sidebar__intro {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 10px 13px;
    color: #1e2a3a;
    font-size: 0.92rem;
    font-weight: 700;
}

.admin-sidebar__count {
    min-width: 26px;
    padding: 2px 7px;
    border-radius: 6px;
    background: #edf2f7;
    color: #5e6f88;
    text-align: center;
    font-size: 0.73rem;
}

.admin-sidebar__search {
    width: calc(100% - 12px);
    margin: 0 6px 13px;
    padding: 9px 11px;
    border: 1px solid #dce3ec;
    border-radius: 8px;
    outline: none;
    color: #1e2a3a;
    background: #f9fafc;
    font-size: 0.82rem;
}

.admin-sidebar__search:focus {
    border-color: #3b6a9c;
    background: white;
    box-shadow: 0 0 0 3px rgba(59, 106, 156, 0.13);
}

.admin-sidebar__empty {
    margin: 10px;
    color: #5e6f88;
    font-size: 0.8rem;
}

.admin-sidebar__section {
    padding: 8px 0 10px;
    border-top: 1px solid #edf0f4;
}

.admin-sidebar__section-title {
    font-size: 0.67rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.09em;
    color: #708197;
    padding: 5px 12px 8px;
}

.admin-sidebar__item {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    padding: 8px 10px;
    border: none;
    background: transparent;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.82rem;
    font-weight: 500;
    color: #5e6f88;
    transition: background 0.15s, color 0.15s, box-shadow 0.15s;
    text-align: left;
}

.admin-sidebar__item:hover,
.admin-sidebar__item:focus-visible {
    background: #f2f4f8;
    color: #1e2a3a;
}

.admin-sidebar__item--active {
    background: #e6edf6;
    color: #244d73;
    font-weight: 700;
    box-shadow: inset 3px 0 #3b6a9c;
}

.admin-sidebar__item-icon {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    flex-shrink: 0;
    border-radius: 7px;
    background: #f0f3f7;
    color: #5e6f88;
    font-size: 0.65rem;
    font-weight: 700;
}

.admin-sidebar__item--active .admin-sidebar__item-icon {
    background: #d4e1ef;
    color: #244d73;
}

.admin-sidebar__item-label {
    flex: 1;
    line-height: 1.3;
}

.admin-content {
    min-width: 0;
}

.admin-welcome {
    background: #fff;
    border: 1px solid #e2e6ec;
    border-radius: 12px;
    padding: 32px;
    box-shadow: 0 2px 10px rgba(30, 42, 58, 0.035);
}

.admin-welcome__intro {
    max-width: 590px;
    margin-bottom: 28px;
}

.admin-welcome h2 {
    color: #1e2a3a;
    font-size: 1.4rem;
    font-weight: 700;
    margin-bottom: 7px;
}

.admin-welcome p {
    color: #5e6f88;
    font-size: 0.9rem;
    margin: 0;
}

.admin-welcome__grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 12px;
}

.admin-welcome__card {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 13px;
    min-height: 142px;
    padding: 18px;
    border: 1px solid #e2e6ec;
    border-radius: 10px;
    background: #fff;
    text-align: left;
    transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}

.admin-welcome__card:hover,
.admin-welcome__card:focus-visible {
    border-color: #aebfd2;
    box-shadow: 0 5px 16px rgba(30, 42, 58, 0.08);
    transform: translateY(-2px);
}

.admin-welcome__number {
    color: #3b6a9c;
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.08em;
}

.admin-welcome__card-title {
    color: #1e2a3a;
    font-size: 0.97rem;
    font-weight: 700;
    line-height: 1.3;
}

.admin-welcome__card-meta {
    display: flex;
    justify-content: space-between;
    width: 100%;
    margin-top: auto;
    color: #5e6f88;
    font-size: 0.78rem;
}

.crud-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 18px;
    margin-bottom: 20px;
}

.crud-header__copy {
    min-width: 0;
}

.crud-header__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    align-items: center;
    gap: 10px;
}

.crud-header__title {
    font-size: 1.45rem;
    font-weight: 700;
    color: #1e2a3a;
    letter-spacing: -0.02em;
    margin: 0 0 5px;
}

.crud-header__desc {
    color: #5e6f88;
    font-size: 0.86rem;
    margin: 0;
}

.btn-add {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    background: #3b6a9c;
    color: white;
    border: none;
    border-radius: 7px;
    font-size: 0.88rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s;
    white-space: nowrap;
}

.btn-add:hover {
    background: #2c5f8a;
}

.crud-card {
    background: white;
    border: 1px solid #e2e6ec;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(30, 42, 58, 0.035);
}

.crud-card__summary {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 18px;
    border-bottom: 1px solid #e2e6ec;
    color: #5e6f88;
    font-size: 0.8rem;
}

.crud-card__summary strong {
    padding: 3px 8px;
    border-radius: 6px;
    background: #edf2f7;
    color: #2c5f8a;
    font-size: 0.76rem;
}

.crud-loading,
.crud-error,
.crud-empty {
    padding: 48px;
    text-align: center;
    color: #8a9aa8;
    font-size: 0.9rem;
}

.crud-error {
    color: #c0392b;
}

.table-wrapper {
    overflow-x: auto;
}

.crud-table {
    width: 100%;
    min-width: 680px;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.crud-table thead {
    background: #f8f9fb;
    border-bottom: 1px solid #e2e6ec;
}

.crud-table th {
    padding: 10px 14px;
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #5e6f88;
    text-transform: uppercase;
    text-align: left;
}

.crud-table td {
    padding: 10px 14px;
    border-bottom: 1px solid #f2f4f8;
    color: #1e2a3a;
    vertical-align: middle;
}

.crud-table tr:last-child td {
    border-bottom: none;
}

.crud-table tr:hover td {
    background: #fafbfc;
}

.td-badge {
    background: #e6edf6;
    color: #2c5f8a;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.78rem;
    font-weight: 600;
}

.bool-yes {
    color: #27ae60;
    font-weight: 600;
}

.bool-no {
    color: #e74c3c;
    font-weight: 600;
}

.col-actions {
    text-align: right;
    white-space: nowrap;
    width: 170px;
}

.btn-edit,
.btn-delete,
.btn-key,
.btn-unlock {
    border: 1px solid #dce3ec;
    background: #fff;
    cursor: pointer;
    padding: 5px 8px;
    border-radius: 6px;
    font-size: 0.74rem;
    font-weight: 600;
    color: #2c5f8a;
    transition: background 0.15s, border-color 0.15s;
}

.col-actions button + button {
    margin-left: 5px;
}

.btn-edit:hover {
    background: #e6edf6;
}

.btn-delete:hover {
    background: #fde8e8;
}

.btn-delete {
    color: #bd3f3f;
}

.btn-key:hover {
    background: #fff3cd;
}

.btn-unlock:hover {
    background: #d4edda;
}

/* ── Modal ──────────────────────────────────────────────────────────────── */
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    backdrop-filter: blur(3px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9000;
    padding: 20px;
}

.modal-box {
    background: white;
    border-radius: 14px;
    width: 100%;
    max-width: 560px;
    max-height: 85vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-box--sm {
    max-width: 420px;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 18px 20px;
    border-bottom: 1px solid #e2e6ec;
}

.modal-header h3 {
    margin: 0;
    font-size: 1rem;
    font-weight: 700;
    color: #1e2a3a;
}

.modal-header--danger {
    background: #fde8e8;
    border-radius: 14px 14px 0 0;
}

.modal-header--danger h3 {
    color: #c0392b;
}

.modal-close {
    border: none;
    background: transparent;
    font-size: 1.1rem;
    cursor: pointer;
    color: #8a9aa8;
    padding: 4px 8px;
    border-radius: 6px;
}

.modal-body {
    padding: 18px 20px;
    overflow-y: auto;
    flex: 1;
}

.modal-footer {
    padding: 12px 20px;
    border-top: 1px solid #e2e6ec;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

/* ── Formulaire ─────────────────────────────────────────────────────────── */
.form-group {
    margin-bottom: 14px;
}

.form-group label {
    display: block;
    font-size: 0.82rem;
    font-weight: 600;
    color: #1e2a3a;
    margin-bottom: 5px;
}

.req {
    color: #e74c3c;
    margin-left: 3px;
}

.form-control {
    width: 100%;
    padding: 8px 12px;
    border: 1px solid #d1d9e0;
    border-radius: 8px;
    font-size: 0.88rem;
    color: #1e2a3a;
    background: #fafbfc;
    transition: border-color 0.15s;
    box-sizing: border-box;
}

.form-control:focus {
    outline: none;
    border-color: #2c5f8a;
    background: white;
}

.form-check {
    display: flex;
    align-items: center;
    gap: 8px;
}

.form-check-input {
    width: 16px;
    height: 16px;
    accent-color: #2c5f8a;
}

.form-error {
    margin-top: 8px;
    padding: 8px 12px;
    background: #fde8e8;
    color: #c0392b;
    border-radius: 6px;
    font-size: 0.82rem;
}

.btn-cancel {
    padding: 8px 18px;
    border: 1px solid #d1d9e0;
    border-radius: 8px;
    background: white;
    color: #5e6f88;
    font-size: 0.88rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.15s;
}

.btn-cancel:hover {
    background: #f2f4f8;
}

.btn-save {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 20px;
    border: none;
    background: #2c5f8a;
    color: white;
    border-radius: 8px;
    font-size: 0.88rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s;
}

.btn-save:hover:not(:disabled) {
    background: #1e3a5f;
}

.btn-save:disabled {
    opacity: 0.65;
    cursor: not-allowed;
}

.btn-delete-confirm {
    padding: 8px 20px;
    border: none;
    background: #e74c3c;
    color: white;
    border-radius: 8px;
    font-size: 0.88rem;
    font-weight: 600;
    cursor: pointer;
}

.btn-delete-confirm:hover:not(:disabled) {
    background: #c0392b;
}

/* ── Spinner ────────────────────────────────────────────────────────────── */
.spinner {
    width: 28px;
    height: 28px;
    border: 3px solid #e2e6ec;
    border-top-color: #2c5f8a;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin: 0 auto 10px;
}

.spinner-sm {
    display: inline-block;
    width: 14px;
    height: 14px;
    border: 2px solid rgba(255, 255, 255, 0.4);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ── Toast ──────────────────────────────────────────────────────────────── */
.text-muted {
    color: #8a9aa8;
    font-size: 0.85rem;
}
@media (max-width: 1250px) {
    .admin-layout { grid-template-columns: minmax(0, 1fr); }
    .admin-sidebar { position: static; max-height: 260px; }
    .admin-sidebar nav { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 0 12px; }
}
@media (max-width: 768px) {
    .admin-body { padding: 20px 16px; }
    .admin-topbar { padding: 0 16px; }
    .admin-topbar__inner { min-height: 84px; }
    .admin-topbar__badge { display: none; }
    .admin-topbar__icon { display: none; }
    .crud-header { align-items: flex-start; flex-direction: column; }
    .crud-header__actions { justify-content: flex-start; }
    .admin-welcome { padding: 22px; }
}
</style>

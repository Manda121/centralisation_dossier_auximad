/**
 * AdminService.js
 * Service pour les API d'administration (niveau autorisation = 10000)
 */
import authService from '../utils/auth.service'
import axios from 'axios'

const API_URL = 'http://localhost:8080/api/admin'

const api = axios.create({ baseURL: API_URL })

api.interceptors.request.use((config) => {
    const token = authService.getToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// ── Helper générique ──────────────────────────────────────────────────────

async function lister(endpoint) {
    const r = await api.get(`/${endpoint}`)
    return r.data
}

async function creer(endpoint, data) {
    const r = await api.post(`/${endpoint}`, data)
    return r.data
}

async function modifier(endpoint, id, data) {
    const r = await api.put(`/${endpoint}/${id}`, data)
    return r.data
}

async function supprimer(endpoint, id) {
    const r = await api.delete(`/${endpoint}/${id}`)
    return r.data
}

// ── Exports organisés par section ─────────────────────────────────────────

const AdminService = {
    // 1. Utilisateurs & Sécurité
    utilisateurs: {
        lister: () => lister('utilisateurs'),
        obtenir: (id) => api.get(`/utilisateurs/${id}`).then(r => r.data),
        creer: (data) => creer('utilisateurs', data),
        modifier: (id, data) => modifier('utilisateurs', id, data),
        supprimer: (id) => supprimer('utilisateurs', id),
        reinitialiserMdp: (id, motDePasse) =>
            api.post(`/utilisateurs/${id}/reinitialiser-mdp`, { motDePasse }).then(r => r.data),
    },
    roles: {
        lister: () => lister('roles'),
        creer: (data) => creer('roles', data),
        modifier: (id, data) => modifier('roles', id, data),
        supprimer: (id) => supprimer('roles', id),
    },
    autorisations: {
        lister: () => lister('autorisations'),
        modifier: (idUtilisateur, niveauAutorisation) =>
            api.put(`/autorisations/utilisateur/${idUtilisateur}`, { niveauAutorisation }).then(r => r.data),
    },
    suspensions: {
        lister: () => lister('suspensions'),
        debloquer: (idUtilisateur) =>
            api.post(`/suspensions/${idUtilisateur}/debloquer`).then(r => r.data),
    },
    actions: {
        lister: () => lister('actions'),
        creer: (data) => creer('actions', data),
        modifier: (id, data) => modifier('actions', id, data),
        supprimer: (id) => supprimer('actions', id),
    },

    // 2. Structure organisationnelle
    agences: {
        lister: () => lister('agences'),
        creer: (data) => creer('agences', data),
        modifier: (id, data) => modifier('agences', id, data),
        supprimer: (id) => supprimer('agences', id),
    },
    bureaux: {
        lister: () => lister('bureaux'),
        creer: (data) => creer('bureaux', data),
        modifier: (id, data) => modifier('bureaux', id, data),
        supprimer: (id) => supprimer('bureaux', id),
    },

    // 3. Référentiels métier transit
    modesTransport: {
        lister: () => lister('modes-transport'),
        creer: (data) => creer('modes-transport', data),
        modifier: (id, data) => modifier('modes-transport', id, data),
        supprimer: (id) => supprimer('modes-transport', id),
    },
    typesProduction: {
        lister: () => lister('types-production'),
        creer: (data) => creer('types-production', data),
        modifier: (id, data) => modifier('types-production', id, data),
        supprimer: (id) => supprimer('types-production', id),
    },
    typesRegime: {
        lister: () => lister('types-regime'),
        creer: (data) => creer('types-regime', data),
        modifier: (id, data) => modifier('types-regime', id, data),
        supprimer: (id) => supprimer('types-regime', id),
    },
    phasesDossier: {
        lister: () => lister('phases-dossier'),
        creer: (data) => creer('phases-dossier', data),
        modifier: (id, data) => modifier('phases-dossier', id, data),
        supprimer: (id) => supprimer('phases-dossier', id),
    },
    statutsDossier: {
        lister: () => lister('statuts-dossier'),
        creer: (data) => creer('statuts-dossier', data),
        modifier: (id, data) => modifier('statuts-dossier', id, data),
        supprimer: (id) => supprimer('statuts-dossier', id),
    },
    statutsOt: {
        lister: () => lister('statuts-ot'),
        creer: (data) => creer('statuts-ot', data),
        modifier: (id, data) => modifier('statuts-ot', id, data),
        supprimer: (id) => supprimer('statuts-ot', id),
    },
    incotermes: {
        lister: () => lister('incotermes'),
        creer: (data) => creer('incotermes', data),
        modifier: (id, data) => modifier('incotermes', id, data),
        supprimer: (id) => supprimer('incotermes', id),
    },
    naturesMarchandise: {
        lister: () => lister('natures-marchandise'),
        creer: (data) => creer('natures-marchandise', data),
        modifier: (id, data) => modifier('natures-marchandise', id, data),
        supprimer: (id) => supprimer('natures-marchandise', id),
    },
    unitesMesure: {
        lister: () => lister('unites-mesure'),
        creer: (data) => creer('unites-mesure', data),
        modifier: (id, data) => modifier('unites-mesure', id, data),
        supprimer: (id) => supprimer('unites-mesure', id),
    },
    typesContainer: {
        lister: () => lister('types-container'),
        creer: (data) => creer('types-container', data),
        modifier: (id, data) => modifier('types-container', id, data),
        supprimer: (id) => supprimer('types-container', id),
    },

    // 4. Documents
    typesDocument: {
        lister: () => lister('types-document'),
        creer: (data) => creer('types-document', data),
        modifier: (id, data) => modifier('types-document', id, data),
        supprimer: (id) => supprimer('types-document', id),
    },

    // 5. Tiers / annuaires
    compagnies: {
        lister: () => lister('compagnies'),
        creer: (data) => creer('compagnies', data),
        modifier: (id, data) => modifier('compagnies', id, data),
        supprimer: (id) => supprimer('compagnies', id),
    },
    navires: {
        lister: () => lister('navires'),
        creer: (data) => creer('navires', data),
        modifier: (id, data) => modifier('navires', id, data),
        supprimer: (id) => supprimer('navires', id),
    },
    pays: {
        lister: () => lister('pays'),
        creer: (data) => creer('pays', data),
        modifier: (id, data) => modifier('pays', id, data),
        supprimer: (id) => supprimer('pays', id),
    },

    // 6. Opérations Dossier (Gestion Admin & Drag-and-Drop)
    operationsDossier: {
        lister: (typeProductionId) => typeProductionId ? api.get(`/operations-dossier?typeProductionId=${typeProductionId}`).then(r => r.data) : lister('operations-dossier'),
        creer: (data) => creer('operations-dossier', data),
        modifier: (id, data) => modifier('operations-dossier', id, data),
        supprimer: (id) => supprimer('operations-dossier', id),
        supprimerBatch: (ids) => api.post('/operations-dossier/batch-delete', ids).then(r => r.data),
        reordonner: (ids) => api.put('/operations-dossier/reorder', ids).then(r => r.data),
    },
}

export default AdminService

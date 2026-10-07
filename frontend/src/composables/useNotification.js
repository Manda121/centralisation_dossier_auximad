import { reactive } from 'vue'

const state = reactive({ toasts: [], currentConfirm: null, confirmQueue: [] })
const timers = new Map()
let compteur = 0

const titres = {
    success: 'Opération réussie', error: 'Une erreur est survenue',
    warning: 'Attention', info: 'Information'
}
const messages = {
    success: 'L’opération a été effectuée.',
    error: 'L’opération n’a pas pu être effectuée. Réessayez.',
    warning: 'Vérifiez les informations saisies.',
    info: 'Une nouvelle information est disponible.'
}

function texteNotification(value, profondeur = 0) {
    if (value == null || profondeur > 4) return ''
    if (typeof value === 'string' || typeof value === 'number') return String(value).trim()
    if (Array.isArray(value)) return value.map(item => texteNotification(item, profondeur + 1)).filter(Boolean).join('\n')
    if (typeof value !== 'object') return ''

    if (value.response?.data) return texteNotification(value.response.data, profondeur + 1)
    const principal = texteNotification(value.message ?? value.erreur ?? value.error, profondeur + 1)
    const details = texteNotification(value.details, profondeur + 1)
    if (principal || details) return [principal, details].filter((part, index) => part && (index === 0 || part !== principal)).join('\n')
    if (value.errors) return texteNotification(Object.values(value.errors), profondeur + 1)
    return ''
}

function retirer(id) {
    clearTimeout(timers.get(id))
    timers.delete(id)
    const index = state.toasts.findIndex(item => item.id === id)
    if (index !== -1) state.toasts.splice(index, 1)
}

function push(message, type = 'info', title = null) {
    const categorie = titres[type] ? type : 'info'
    const id = ++compteur
    state.toasts.push({
        id, type: categorie,
        title: texteNotification(title) || titres[categorie],
        message: texteNotification(message) || messages[categorie]
    })
    if (state.toasts.length > 4) retirer(state.toasts[0].id)
    timers.set(id, setTimeout(() => retirer(id), categorie === 'error' || categorie === 'warning' ? 8000 : 5500))
    return id
}

function fermer(resultat = false) {
    const notification = state.currentConfirm
    if (!notification) return
    state.currentConfirm = state.confirmQueue.shift() || null
    notification.resolve(Boolean(resultat))
}

function confirm(message, title = null) {
    return new Promise(resolve => {
        const notification = {
            id: ++compteur,
            title: texteNotification(title) || 'Confirmer l’action',
            message: texteNotification(message) || 'Voulez-vous continuer ?',
            resolve
        }
        if (state.currentConfirm) state.confirmQueue.push(notification)
        else state.currentConfirm = notification
    })
}

export function useNotification() {
    return {
        state, fermer, retirer, confirm,
        notify: (message, type, title) => push(message, type, title),
        notifySuccess: (message, title) => push(message, 'success', title),
        notifyError: (message, title) => push(message, 'error', title),
        notifyWarning: (message, title) => push(message, 'warning', title),
        notifyInfo: (message, title) => push(message, 'info', title)
    }
}

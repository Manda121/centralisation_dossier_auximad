import { reactive } from 'vue'

/**
 * Etat global partage par toutes les pages : une fenetre de notification
 * a la fois, avec file d'attente. Le composant <NotificationModal /> est
 * monte une seule fois dans App.vue et affiche cet etat.
 */
const state = reactive({
    current: null, // { id, type, title, message, isConfirm?, resolve? }
    queue: []
})

let compteur = 0

const titresParDefaut = {
    success: 'Succès',
    error: 'Erreur',
    warning: 'Attention',
    info: 'Information',
    confirm: 'Confirmation'
}

function push(message, type = 'info', title = null, extra = {}) {
    const notif = {
        id: ++compteur,
        type,
        title: title || titresParDefaut[type] || titresParDefaut.info,
        message: String(message ?? ''),
        ...extra
    }
    if (!state.current) {
        state.current = notif
    } else {
        state.queue.push(notif)
    }
    return notif.id
}

/** Ferme la notification courante et affiche la suivante de la file. */
function fermer(resultat = false) {
    // Si c'est une confirmation, résoudre la promesse avant de fermer
    if (state.current?.isConfirm && typeof state.current.resolve === 'function') {
        state.current.resolve(resultat)
    }
    state.current = state.queue.shift() || null
}

/**
 * Affiche une boîte de dialogue de confirmation (remplace window.confirm).
 * Retourne une Promise<boolean> : true si confirmé, false si annulé.
 */
function confirm(message, title = null) {
    return new Promise((resolve) => {
        const notif = {
            id: ++compteur,
            type: 'warning',
            title: title || 'Confirmation requise',
            message: String(message ?? ''),
            isConfirm: true,
            resolve
        }
        if (!state.current) {
            state.current = notif
        } else {
            state.queue.push(notif)
        }
    })
}

export function useNotification() {
    return {
        state,
        fermer,
        confirm,
        notify: (message, type, title) => push(message, type, title),
        notifySuccess: (message, title) => push(message, 'success', title),
        notifyError: (message, title) => push(message, 'error', title),
        notifyWarning: (message, title) => push(message, 'warning', title),
        notifyInfo: (message, title) => push(message, 'info', title)
    }
}

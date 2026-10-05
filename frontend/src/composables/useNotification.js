import { reactive } from 'vue'

/**
 * Etat global partage par toutes les pages : une fenetre de notification
 * a la fois, avec file d'attente. Le composant <NotificationModal /> est
 * monte une seule fois dans App.vue et affiche cet etat.
 */
const state = reactive({
    current: null, // { id, type, title, message }
    queue: []
})

let compteur = 0

const titresParDefaut = {
    success: 'Succès',
    error: 'Erreur',
    warning: 'Attention',
    info: 'Information'
}

function push(message, type = 'info', title = null) {
    const notif = {
        id: ++compteur,
        type,
        title: title || titresParDefaut[type] || titresParDefaut.info,
        message: String(message ?? '')
    }
    if (!state.current) {
        state.current = notif
    } else {
        state.queue.push(notif)
    }
    return notif.id
}

/** Ferme la notification courante et affiche la suivante de la file. */
function fermer() {
    state.current = state.queue.shift() || null
}

export function useNotification() {
    return {
        state,
        fermer,
        notify: (message, type, title) => push(message, type, title),
        notifySuccess: (message, title) => push(message, 'success', title),
        notifyError: (message, title) => push(message, 'error', title),
        notifyWarning: (message, title) => push(message, 'warning', title),
        notifyInfo: (message, title) => push(message, 'info', title)
    }
}

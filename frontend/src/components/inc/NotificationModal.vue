<script setup>
import { useNotification } from '../../composables/useNotification'

// Fenetre de notification commune a toutes les pages :
// remplace les popups d'alerte natifs (alert()).
const { state, fermer } = useNotification()

const styleParType = {
    success: { header: 'bg-success', icon: '✅' },
    error: { header: 'bg-danger', icon: '⛔' },
    warning: { header: 'bg-warning text-dark', icon: '⚠️' },
    info: { header: 'bg-primary', icon: 'ℹ️' }
}

const style = (type) => styleParType[type] || styleParType.info
</script>

<template>
    <div v-if="state.current" class="notification-overlay" @click.self="fermer">
        <div class="modal-dialog modal-dialog-centered" role="dialog" aria-modal="true">
            <div class="modal-content border-0 shadow">
                <div class="modal-header py-3 text-white" :class="style(state.current.type).header">
                    <h5 class="modal-title fw-bold">
                        {{ style(state.current.type).icon }} {{ state.current.title }}
                    </h5>
                    <button type="button" class="btn-close" :class="{ 'btn-close-white': state.current.type !== 'warning' }" aria-label="Fermer" @click="fermer"></button>
                </div>
                <div class="modal-body py-4">
                    <p class="mb-0 notification-message">{{ state.current.message }}</p>
                </div>
                <div class="modal-footer bg-light py-2">
                    <button type="button" class="btn btn-primary px-4" @click="fermer">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.notification-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
}
.notification-overlay .modal-dialog {
    width: 100%;
    max-width: 480px;
    margin: 0;
}
.notification-message {
    white-space: pre-line;
    overflow-wrap: anywhere;
}
</style>

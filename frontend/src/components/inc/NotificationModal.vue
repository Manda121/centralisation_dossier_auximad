<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useNotification } from '../../composables/useNotification'

const { state, fermer, retirer } = useNotification()
const annulerBouton = ref(null)
const confirmerBouton = ref(null)
let ancienFocus = null

watch(() => state.currentConfirm?.id, async id => {
    if (id) {
        ancienFocus ||= document.activeElement
        await nextTick()
        annulerBouton.value?.focus()
    } else if (ancienFocus) {
        ancienFocus.focus?.()
        ancienFocus = null
    }
})

function gererClavier(event) {
    if (!state.currentConfirm) return
    if (event.key === 'Escape') {
        event.preventDefault()
        fermer(false)
    } else if (event.key === 'Tab') {
        const premier = annulerBouton.value
        const dernier = confirmerBouton.value
        if (event.shiftKey && document.activeElement === premier) {
            event.preventDefault()
            dernier?.focus()
        } else if (!event.shiftKey && document.activeElement === dernier) {
            event.preventDefault()
            premier?.focus()
        }
    }
}

onMounted(() => document.addEventListener('keydown', gererClavier))
onUnmounted(() => document.removeEventListener('keydown', gererClavier))
</script>

<template>
    <Teleport to="body">
        <div class="notification-stack" aria-label="Notifications">
            <TransitionGroup name="notification">
                <article v-for="item in state.toasts" :key="item.id" class="notification-card"
                    :class="`notification-card--${item.type}`" :role="item.type === 'error' ? 'alert' : 'status'">
                    <div class="notification-card__icon" aria-hidden="true">
                        <svg v-if="item.type === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" />
                        </svg>
                        <svg v-else-if="item.type === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <circle cx="12" cy="12" r="9" /><path d="m9 9 6 6m0-6-6 6" />
                        </svg>
                        <svg v-else-if="item.type === 'warning'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <path d="M10.3 4.3 2.5 18a1.5 1.5 0 0 0 1.3 2.2h16.4a1.5 1.5 0 0 0 1.3-2.2L13.7 4.3a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4m0 3h.01" />
                        </svg>
                        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                            <circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8h.01" />
                        </svg>
                    </div>
                    <div class="notification-card__content">
                        <strong>{{ item.title }}</strong>
                        <p>{{ item.message }}</p>
                    </div>
                    <button type="button" class="notification-card__close" :aria-label="`Fermer : ${item.title}`" @click="retirer(item.id)">×</button>
                </article>
            </TransitionGroup>
        </div>

        <Transition name="dialogue">
            <div v-if="state.currentConfirm" class="notification-overlay">
                <section class="notification-dialogue" role="alertdialog" aria-modal="true"
                    aria-labelledby="notification-dialogue-titre" aria-describedby="notification-dialogue-message">
                    <div class="notification-dialogue__top">
                        <span class="notification-dialogue__icon" aria-hidden="true">!</span>
                        <div>
                            <span class="notification-dialogue__eyebrow">Vérification</span>
                            <h2 id="notification-dialogue-titre">{{ state.currentConfirm.title }}</h2>
                        </div>
                    </div>
                    <p id="notification-dialogue-message" class="notification-dialogue__message">{{ state.currentConfirm.message }}</p>
                    <div class="notification-dialogue__actions">
                        <button ref="annulerBouton" type="button" class="notification-dialogue__cancel" @click="fermer(false)">Annuler</button>
                        <button ref="confirmerBouton" type="button" class="notification-dialogue__confirm" @click="fermer(true)">Confirmer</button>
                    </div>
                </section>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.notification-stack { position: fixed; top: 24px; right: 24px; z-index: 3000; display: grid; gap: 12px; width: min(420px, calc(100vw - 32px)); pointer-events: none; }
.notification-card { --accent: #35699a; --tint: #eaf3fc; display: flex; align-items: flex-start; gap: 12px; padding: 16px; border: 1px solid #dce4ed; border-left: 4px solid var(--accent); border-radius: 14px; background: #fff; box-shadow: 0 14px 36px rgba(25, 42, 66, .18); color: #1c2b3f; pointer-events: auto; }
.notification-card--success { --accent: #16824c; --tint: #e6f6ed; }
.notification-card--error { --accent: #c3353f; --tint: #fff0f0; }
.notification-card--warning { --accent: #ac6b10; --tint: #fff5df; }
.notification-card__icon { display: grid; place-items: center; width: 34px; height: 34px; flex: 0 0 34px; border-radius: 10px; background: var(--tint); color: var(--accent); }
.notification-card__icon svg { width: 20px; height: 20px; stroke-linecap: round; stroke-linejoin: round; }
.notification-card__content { min-width: 0; flex: 1; }
.notification-card__content strong { display: block; margin: 1px 0 5px; font-size: 14px; line-height: 1.3; }
.notification-card__content p { margin: 0; color: #4a5b70; font-size: 13px; line-height: 1.5; white-space: pre-line; overflow-wrap: anywhere; max-height: 190px; overflow-y: auto; }
.notification-card__close { border: 0; background: transparent; color: #66778a; font-size: 23px; line-height: 1; padding: 0 2px; cursor: pointer; }
.notification-card__close:hover { color: #1c2b3f; }
.notification-overlay { position: fixed; inset: 0; z-index: 3100; display: grid; place-items: center; padding: 20px; background: rgba(13, 28, 47, .58); }
.notification-dialogue { width: min(520px, 100%); padding: 28px; border-radius: 18px; background: #fff; box-shadow: 0 24px 70px rgba(0, 0, 0, .25); }
.notification-dialogue__top { display: flex; align-items: center; gap: 15px; }
.notification-dialogue__icon { display: grid; place-items: center; width: 44px; height: 44px; flex: 0 0 44px; border-radius: 12px; background: #fff0da; color: #a65a08; font-size: 27px; font-weight: 700; }
.notification-dialogue__eyebrow { color: #9a5b13; font-size: 11px; font-weight: 700; letter-spacing: .09em; text-transform: uppercase; }
.notification-dialogue h2 { margin: 2px 0 0; color: #1c2b3f; font-size: 20px; font-weight: 700; }
.notification-dialogue__message { margin: 20px 0 24px; color: #44546a; font-size: 14px; line-height: 1.6; white-space: pre-line; overflow-wrap: anywhere; max-height: 50vh; overflow-y: auto; }
.notification-dialogue__actions { display: flex; justify-content: flex-end; gap: 10px; }
.notification-dialogue__actions button { border-radius: 9px; padding: 9px 18px; font-size: 14px; font-weight: 600; cursor: pointer; }
.notification-dialogue__cancel { border: 1px solid #cbd5e1; background: #fff; color: #334155; }
.notification-dialogue__confirm { border: 1px solid #225a8b; background: #2b6599; color: #fff; }
.notification-dialogue__cancel:hover { background: #f3f6fa; }
.notification-dialogue__confirm:hover { background: #1d4e79; }
.notification-enter-active, .notification-leave-active, .dialogue-enter-active, .dialogue-leave-active { transition: opacity .2s ease, transform .2s ease; }
.notification-enter-from, .notification-leave-to { opacity: 0; transform: translateX(20px); }
.dialogue-enter-from, .dialogue-leave-to { opacity: 0; }
@media (max-width: 540px) { .notification-stack { top: 12px; right: 16px; } .notification-dialogue { padding: 22px; } .notification-dialogue__actions button { flex: 1; } }
@media (prefers-reduced-motion: reduce) { .notification-enter-active, .notification-leave-active, .dialogue-enter-active, .dialogue-leave-active { transition: none; } }
</style>

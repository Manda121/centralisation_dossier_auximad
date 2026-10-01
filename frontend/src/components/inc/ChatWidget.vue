<template>
  <!-- ─────────── Bouton flottant ─────────── -->
  <button
    v-if="!ouvert"
    class="chat-fab"
    :class="{ 'chat-fab--loading': enCours }"
    type="button"
    :title="enCours ? 'L’assistant réfléchit…' : 'Ouvrir l’assistant'"
    :aria-label="enCours ? 'L’assistant réfléchit' : 'Ouvrir l’assistant'"
    @click="ouvrir"
  >
    <svg viewBox="0 0 16 16" fill="currentColor" class="chat-fab__icon" aria-hidden="true">
      <path
        d="M8 15c-3.87 0-7-2.24-7-5 0-.76.2-1.48.56-2.14A5.01 5.01 0 0 1 1 6a5.5 5.5 0 0 1 2.24-4.24A5.5 5.5 0 0 1 7 1a5.5 5.5 0 0 1 3.76.76A5.5 5.5 0 0 1 13 6c0 1.24-.44 2.4-1.2 3.34l.2.02A4.98 4.98 0 0 1 14 10c0 2.76-3.13 5-7 5m0-1c3.31 0 6-1.79 6-4 0-1.27-.68-2.44-1.8-3.25l-.66-.48-.4.7A4.6 4.6 0 0 1 8.9 8v.5a4.5 4.5 0 0 1-4 4c-.45 0-.9-.05-1.32-.15l-.55-.13.2.53c.9.02 1.82.06 2.77.06z" />
      <circle cx="5" cy="6" r=".9" />
      <circle cx="8" cy="6" r=".9" />
      <circle cx="11" cy="6" r=".9" />
    </svg>
    <span v-if="enCours" class="chat-fab__spinner" aria-hidden="true"></span>
  </button>

  <!-- ─────────── Panneau ─────────── -->
  <section v-else class="chat-panel" role="dialog" aria-label="Assistant Auximad">
    <header class="chat-panel__header">
      <div class="chat-panel__titles">
        <span class="chat-panel__title">Assistant Auximad</span>
        <span class="chat-panel__subtitle">
          <span class="chat-panel__dot" :class="{ 'chat-panel__dot--off': !disponible }"></span>
          {{ disponible ? 'Données à jour' : 'Connexion au modèle…' }}
        </span>
      </div>
      <div class="chat-panel__actions">
        <button class="chat-panel__icon-btn" type="button" title="Nouvelle conversation" @click="nouvelleConversation">
          <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path
              d="M8 2.5a.5.5 0 0 1 .5.5v4.5H13a.5.5 0 0 1 0 1H8.5V13a.5.5 0 0 1-1 0V8.5H3a.5.5 0 0 1 0-1h4.5V3a.5.5 0 0 1 .5-.5" />
            <path
              d="M2 3.5a.5.5 0 0 1 .5-.5h3.793a.5.5 0 0 1 .354.146l.707.707A.5.5 0 0 0 7.707 4H13.5a.5.5 0 0 1 .5.5v8a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5z" />
          </svg>
        </button>
        <button class="chat-panel__icon-btn" type="button" title="Fermer" @click="fermer">
          <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path
              d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708" />
          </svg>
        </button>
      </div>
    </header>

    <!-- ─────────── Messages ─────────── -->
    <div ref="zoneMessages" class="chat-panel__messages">
      <div v-if="messages.length === 0" class="chat-panel__intro">
        <p class="chat-panel__intro-title">Que puis-je faire pour vous ?</p>
        <p class="chat-panel__intro-text">
          Je consulte les données réelles de l’application : dossiers, clients, ordres de transit, documents,
          historique, demandes inter-agences. Toute opération modifiant la base vous sera d’abord présentée
          pour confirmation.
        </p>
      </div>

      <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="`chat-msg--${msg.role}`">
        <div class="chat-msg__bubble">
          <p class="chat-msg__texte">{{ msg.texte }}</p>

          <ul v-if="msg.sources?.length" class="chat-msg__sources">
            <li v-for="(s, j) in msg.sources" :key="j" class="chat-msg__source">{{ s }}</li>
          </ul>

          <div v-if="msg.outils?.length" class="chat-msg__outils">
            <span v-for="(o, j) in msg.outils" :key="j" class="chat-msg__outil">{{ o }}</span>
          </div>
        </div>
      </div>

      <!-- ─────────── Carte de confirmation ─────────── -->
      <div v-if="confirmation" class="chat-confirm" :class="{ 'chat-confirm--critique': confirmation.danger === 'CRITIQUE' }">
        <div class="chat-confirm__head">
          <span class="chat-confirm__badge">{{ confirmation.danger === 'CRITIQUE' ? 'Opération critique' : 'Confirmation requise' }}</span>
          <span class="chat-confirm__outil">{{ confirmation.outil }}</span>
        </div>
        <p class="chat-confirm__resume">{{ confirmation.resume }}</p>
        <ul v-if="confirmation.details?.length" class="chat-confirm__details">
          <li v-for="(d, j) in confirmation.details" :key="j">{{ d }}</li>
        </ul>
        <div class="chat-confirm__actions">
          <button class="chat-confirm__btn chat-confirm__btn--cancel" type="button" :disabled="enCours" @click="decider(false)">
            Annuler
          </button>
          <button class="chat-confirm__btn chat-confirm__btn--ok" type="button" :disabled="enCours" @click="decider(true)">
            {{ confirmation.danger === 'CRITIQUE' ? 'Confirmer définitivement' : 'Confirmer' }}
          </button>
        </div>
      </div>

      <div v-if="enCours" class="chat-msg chat-msg--assistant">
        <div class="chat-msg__bubble chat-msg__bubble--typing">
          <span></span><span></span><span></span>
        </div>
      </div>

      <p v-if="avertissement" class="chat-panel__error">{{ avertissement }}</p>
    </div>

    <!-- ─────────── Suggestions ─────────── -->
    <div v-if="messages.length === 0 && suggestions.length" class="chat-panel__suggestions">
      <button v-for="(s, i) in suggestions" :key="i" class="chat-panel__suggestion" type="button" @click="poser(s)">
        {{ s }}
      </button>
    </div>

    <!-- ─────────── Saisie ─────────── -->
    <form class="chat-panel__input" @submit.prevent="envoyer">
      <textarea
        v-model="brouillon"
        class="chat-panel__textarea"
        rows="1"
        :placeholder="confirmation ? 'Confirmez ou annulez l’opération ci-dessus…' : 'Posez votre question…'"
        :disabled="enCours"
        @keydown.enter.exact.prevent="envoyer"
        @keydown.enter.shift.stop
      ></textarea>
      <button class="chat-panel__send" type="submit" :disabled="enCours || !brouillon.trim()" title="Envoyer" aria-label="Envoyer">
        <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M15.5 1.9a.5.5 0 0 0-.62-.62l-14 6.5a.5.5 0 0 0 0 .91l5.86 2.72 2.72 5.86a.5.5 0 0 0 .91 0z" />
        </svg>
      </button>
    </form>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import ChatService from '../../services/ChatService'

const emit = defineEmits(['ouvrir', 'fermer'])

const ouvert = ref(false)
const enCours = ref(false)
const disponible = ref(true)
const avertissement = ref('')
const brouillon = ref('')
const suggestions = ref([])
const confirmation = ref(null)
const messages = ref([])
const zoneMessages = ref(null)

/** Ouvre le panneau et charge les suggestions au premier usage. */
async function ouvrir() {
  ouvert.value = true
  emit('ouvrir')
  if (suggestions.value.length === 0) {
    try {
      suggestions.value = await ChatService.getSuggestions()
    } catch (e) {
      suggestions.value = []
    }
  }
  await faireDefiler()
}

function fermer() {
  ouvert.value = false
  emit('fermer')
}

function ajouter(role, texte, data = {}) {
  messages.value.push({
    role,
    texte: texte || '',
    sources: data.sources || [],
    outils: data.outilsUtilises || []
  })
  faireDefiler()
}

async function faireDefiler() {
  await nextTick()
  if (zoneMessages.value) {
    zoneMessages.value.scrollTop = zoneMessages.value.scrollHeight
  }
}

/** Applique la réponse du backend : texte, sources, outils et éventuelle confirmation. */
function appliquerReponse(reponse) {
  confirmation.value = reponse.confirmation || null
  if (reponse.confirmation) {
    avertissement.value = ''
    return
  }
  if (reponse.reponse) {
    ajouter('assistant', reponse.reponse, reponse)
  }
  if (reponse.avertissement) {
    avertissement.value = reponse.avertissement
  } else {
    avertissement.value = ''
  }
}

async function poser(texte) {
  if (enCours.value) return
  brouillon.value = ''
  await envoyerTexte(texte)
}

async function envoyer() {
  if (enCours.value) return
  const texte = brouillon.value.trim()
  if (!texte) return
  brouillon.value = ''
  await envoyerTexte(texte)
}

async function envoyerTexte(texte) {
  ajouter('user', texte)
  enCours.value = true
  try {
    const reponse = await ChatService.envoyer(texte)
    appliquerReponse(reponse)
  } catch (e) {
    avertissement.value = ChatService.messageErreur(e)
  } finally {
    enCours.value = false
    faireDefiler()
  }
}

async function decider(accepte) {
  if (enCours.value || !confirmation.value) return
  const enCoursId = confirmation.value.id
  enCours.value = true
  try {
    const reponse = await ChatService.confirmer(enCoursId, accepte)
    appliquerReponse(reponse)
  } catch (e) {
    avertissement.value = ChatService.messageErreur(e)
    confirmation.value = null
  } finally {
    enCours.value = false
    faireDefiler()
  }
}

async function nouvelleConversation() {
  if (enCours.value) return
  try {
    await ChatService.nouvelleConversation()
  } catch (e) {
    avertissement.value = ChatService.messageErreur(e)
  }
  messages.value = []
  confirmation.value = null
  avertissement.value = ''
  try {
    suggestions.value = await ChatService.getSuggestions()
  } catch (e) {
    suggestions.value = []
  }
}

onMounted(async () => {
  // L'état du modèle est interrogé une fois pour signaler proprement une indisponibilité.
  try {
    const etat = await ChatService.getEtat()
    disponible.value = etat?.disponible !== false
    if (!disponible) {
      avertissement.value =
        "Le modèle Ollama est injoignable. Lancez « ollama serve » puis « ollama pull qwen2.5:3b »."
    }
  } catch (e) {
    disponible.value = true
  }
})

defineExpose({ ouvrir, fermer })
</script>

<style scoped>
/* ── Variables ── */
.chat-panel,
.chat-fab {
  --cw-bg: #ffffff;
  --cw-border: #e2e6ec;
  --cw-text: #1e2a3a;
  --cw-text-muted: #5e6f88;
  --cw-text-light: #8a9aa8;
  --cw-primary: #3b6a9c;
  --cw-primary-soft: #e6edf6;
  --cw-user-bg: #3b6a9c;
  --cw-user-text: #ffffff;
  --cw-danger: #b4442f;
  --cw-danger-soft: #fbeae6;
  --cw-radius: 10px;
  --cw-shadow: 0 8px 28px rgba(0, 0, 0, 0.14);
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: var(--cw-text);
}

.chat-fab,
.chat-panel {
  position: fixed;
  bottom: 22px;
  right: 22px;
  z-index: 1200;
}

/* ── Bouton flottant ── */
.chat-fab {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: none;
  background: var(--cw-primary);
  color: #fff;
  box-shadow: var(--cw-shadow);
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: transform 0.15s ease, background 0.15s ease;
}

.chat-fab:hover {
  transform: scale(1.06);
  background: #345f8c;
}

.chat-fab__icon {
  width: 26px;
  height: 26px;
}

.chat-fab__spinner,
.chat-fab__spinner::after {
  position: absolute;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: chat-spin 0.7s linear infinite;
}

.chat-fab__spinner {
  width: 22px;
  height: 22px;
  inset: 0;
  margin: auto;
}

@keyframes chat-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── Panneau ── */
.chat-panel {
  width: 400px;
  max-width: calc(100vw - 32px);
  height: 560px;
  max-height: calc(100vh - 44px);
  background: var(--cw-bg);
  border: 1px solid var(--cw-border);
  border-radius: var(--cw-radius);
  box-shadow: var(--cw-shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--cw-border);
  background: #f8fafc;
}

.chat-panel__titles {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-panel__title {
  font-size: 0.95rem;
  font-weight: 700;
}

.chat-panel__subtitle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.72rem;
  color: var(--cw-text-light);
}

.chat-panel__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2e9e5b;
}

.chat-panel__dot--off {
  background: #d08700;
}

.chat-panel__actions {
  display: flex;
  gap: 2px;
}

.chat-panel__icon-btn {
  border: none;
  background: transparent;
  color: var(--cw-text-muted);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  display: grid;
  place-items: center;
}

.chat-panel__icon-btn:hover {
  background: var(--cw-primary-soft);
  color: var(--cw-primary);
}

.chat-panel__icon-btn svg {
  width: 15px;
  height: 15px;
}

/* ── Messages ── */
.chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-panel__intro-title {
  margin: 0 0 6px;
  font-weight: 700;
  font-size: 0.92rem;
}

.chat-panel__intro-text {
  margin: 0;
  font-size: 0.82rem;
  color: var(--cw-text-muted);
  line-height: 1.5;
}

.chat-msg {
  display: flex;
}

.chat-msg--user {
  justify-content: flex-end;
}

.chat-msg__bubble {
  max-width: 88%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 0.85rem;
  line-height: 1.5;
  background: #f2f4f8;
  border: 1px solid var(--cw-border);
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-msg--user .chat-msg__bubble {
  background: var(--cw-user-bg);
  color: var(--cw-user-text);
  border-color: var(--cw-user-bg);
}

.chat-msg__texte {
  margin: 0;
}

.chat-msg__sources {
  margin: 8px 0 0;
  padding-left: 16px;
  font-size: 0.72rem;
  color: var(--cw-text-light);
}

.chat-msg__sources li {
  margin-bottom: 2px;
}

.chat-msg__outils {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.chat-msg__outil {
  font-size: 0.66rem;
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--cw-primary-soft);
  color: var(--cw-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.chat-msg__bubble--typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 12px;
}

.chat-msg__bubble--typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cw-text-light);
  animation: chat-bounce 1s infinite ease-in-out;
}

.chat-msg__bubble--typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.chat-msg__bubble--typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes chat-bounce {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

/* ── Confirmation ── */
.chat-confirm {
  border: 1px solid #e5c07b;
  background: #fdf6e3;
  border-radius: 10px;
  padding: 11px 12px;
}

.chat-confirm--critique {
  border-color: var(--cw-danger);
  background: var(--cw-danger-soft);
}

.chat-confirm__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.chat-confirm__badge {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #e5c07b;
  color: #4a3a10;
}

.chat-confirm--critique .chat-confirm__badge {
  background: var(--cw-danger);
  color: #fff;
}

.chat-confirm__outil {
  font-size: 0.68rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: var(--cw-text-muted);
}

.chat-confirm__resume {
  margin: 0 0 6px;
  font-size: 0.85rem;
  font-weight: 600;
}

.chat-confirm__details {
  margin: 0 0 10px;
  padding-left: 16px;
  font-size: 0.75rem;
  color: var(--cw-text-muted);
}

.chat-confirm__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.chat-confirm__btn {
  border: 1px solid transparent;
  border-radius: 7px;
  padding: 7px 14px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.chat-confirm__btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.chat-confirm__btn--cancel {
  background: #fff;
  border-color: var(--cw-border);
  color: var(--cw-text-muted);
}

.chat-confirm__btn--cancel:hover:not(:disabled) {
  border-color: var(--cw-text-light);
}

.chat-confirm__btn--ok {
  background: var(--cw-primary);
  color: #fff;
}

.chat-confirm--critique .chat-confirm__btn--ok {
  background: var(--cw-danger);
}

.chat-confirm__btn--ok:hover:not(:disabled) {
  filter: brightness(1.08);
}

.chat-panel__error {
  margin: 0;
  font-size: 0.76rem;
  color: var(--cw-danger);
  background: var(--cw-danger-soft);
  border-radius: 8px;
  padding: 8px 10px;
}

/* ── Suggestions ── */
.chat-panel__suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 14px 10px;
}

.chat-panel__suggestion {
  font-size: 0.74rem;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--cw-border);
  background: #fff;
  color: var(--cw-text-muted);
  cursor: pointer;
  text-align: left;
}

.chat-panel__suggestion:hover {
  border-color: var(--cw-primary);
  color: var(--cw-primary);
  background: var(--cw-primary-soft);
}

/* ── Saisie ── */
.chat-panel__input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--cw-border);
  background: #f8fafc;
}

.chat-panel__textarea {
  flex: 1;
  resize: none;
  border: 1px solid var(--cw-border);
  border-radius: 8px;
  padding: 9px 10px;
  font: inherit;
  font-size: 0.85rem;
  max-height: 110px;
  background: #fff;
  color: var(--cw-text);
}

.chat-panel__textarea:focus {
  outline: none;
  border-color: var(--cw-primary);
}

.chat-panel__send {
  border: none;
  background: var(--cw-primary);
  color: #fff;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.chat-panel__send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chat-panel__send svg {
  width: 15px;
  height: 15px;
}
</style>
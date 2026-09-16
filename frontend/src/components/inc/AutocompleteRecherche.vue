<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'

/**
 * Champ de recherche avec liste de suggestions (autocomplétion).
 * Pensé pour les numéros / codes qu'on peut oublier ou saisir de travers
 * (n° d'OT, code client, NIF, STAT, ...).
 *
 * Le parent fournit `fetchResults(q, limite)` qui renvoie un tableau d'items,
 * et reçoit l'item choisi via l'événement `selection`.
 */
const props = defineProps({
    modelValue: { type: [String, Number], default: '' },
    /** async (q, limite) => Array d'items */
    fetchResults: { type: Function, required: true },
    /** Item -> texte principal (ex : le numéro) */
    labelFn: { type: Function, default: (item) => String(item ?? '') },
    /** Item -> texte secondaire (ex : le client) */
    sublabelFn: { type: Function, default: () => '' },
    /** Item -> valeur placée dans le champ après sélection (labelFn par défaut) */
    valueFn: { type: Function, default: null },
    /** Item -> clé unique pour le v-for */
    keyFn: { type: Function, default: (item) => item?.id ?? JSON.stringify(item) },
    placeholder: { type: String, default: '' },
    /** Nombre de caractères minimum avant de lancer la recherche */
    minChars: { type: Number, default: 1 },
    /** Délai anti-rebond (ms) */
    debounce: { type: Number, default: 250 },
    /** Nombre maximum de suggestions demandées au serveur */
    limite: { type: Number, default: 8 },
    disabled: { type: Boolean, default: false },
    messageVide: { type: String, default: 'Aucun résultat' },
    /** N'autorise que les chiffres dans le champ (numéros d'OT, ...) */
    numerique: { type: Boolean, default: false },
    /** Taille « sm » des contrôles (cohérent avec les formulaires de filtres) */
    petit: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'selection', 'efface'])

const texte = ref(props.modelValue ?? '')
const resultats = ref([])
const chargement = ref(false)
const ouvert = ref(false)
const indexActif = ref(-1)

const racine = ref(null)
const inputRef = ref(null)

let timer = null
let appelEnCours = 0

watch(() => props.modelValue, (val) => {
    const valeur = val ?? ''
    if (String(valeur) !== String(texte.value)) {
        texte.value = valeur
    }
})

function onInput(event) {
    let valeur = event.target.value
    if (props.numerique) {
        valeur = valeur.replace(/[^\d]/g, '')
        if (event.target.value !== valeur) {
            event.target.value = valeur
        }
    }

    texte.value = valeur
    emit('update:modelValue', texte.value)

    if (!texte.value.trim()) {
        clearTimeout(timer)
        appelEnCours += 1
        resultats.value = []
        ouvert.value = false
        chargement.value = false
        emit('efface')
        return
    }
    planifierRecherche()
}

function planifierRecherche() {
    clearTimeout(timer)
    if (texte.value.trim().length < props.minChars) {
        resultats.value = []
        ouvert.value = false
        return
    }
    timer = setTimeout(rechercher, props.debounce)
}

async function rechercher() {
    const q = texte.value.trim()
    if (q.length < props.minChars) return

    const appel = ++appelEnCours
    chargement.value = true
    ouvert.value = true
    try {
        const res = await props.fetchResults(q, props.limite)
        if (appel !== appelEnCours) return // réponse obsolète
        resultats.value = Array.isArray(res) ? res : []
        indexActif.value = resultats.value.length > 0 ? 0 : -1
    } catch (e) {
        if (appel !== appelEnCours) return
        console.error('AutocompleteRecherche — erreur de recherche :', e)
        resultats.value = []
        indexActif.value = -1
    } finally {
        if (appel === appelEnCours) chargement.value = false
    }
}

function valeurItem(item) {
    return props.valueFn ? props.valueFn(item) : props.labelFn(item)
}

function selectionner(item) {
    const valeur = valeurItem(item)
    texte.value = valeur ?? ''
    emit('update:modelValue', texte.value)
    emit('selection', item)
    ouvert.value = false
    indexActif.value = -1
}

function validerClavier() {
    if (ouvert.value && resultats.value.length > 0) {
        const index = indexActif.value >= 0 ? indexActif.value : 0
        selectionner(resultats.value[index])
        return
    }
    // Rien de proposé sous les yeux : on relance immédiatement la recherche
    rechercher()
}

function descendre() {
    if (resultats.value.length === 0) return
    ouvert.value = true
    indexActif.value = (indexActif.value + 1) % resultats.value.length
}

function monter() {
    if (resultats.value.length === 0) return
    ouvert.value = true
    indexActif.value = (indexActif.value - 1 + resultats.value.length) % resultats.value.length
}

function onFocus() {
    if (texte.value.trim().length >= props.minChars && resultats.value.length > 0) {
        ouvert.value = true
    }
}

function fermer() {
    ouvert.value = false
}

function effacer() {
    clearTimeout(timer)
    appelEnCours += 1
    texte.value = ''
    resultats.value = []
    ouvert.value = false
    indexActif.value = -1
    emit('update:modelValue', '')
    emit('efface')
    inputRef.value?.focus()
}

function onClicExterieur(event) {
    if (racine.value && !racine.value.contains(event.target)) {
        ouvert.value = false
    }
}

onMounted(() => document.addEventListener('mousedown', onClicExterieur))
onBeforeUnmount(() => {
    document.removeEventListener('mousedown', onClicExterieur)
    clearTimeout(timer)
})

defineExpose({ rechercher, fermer, effacer, selectionner, inputRef })
</script>

<template>
    <div ref="racine" class="autocomplete position-relative">
        <div class="input-group" :class="{ 'input-group-sm': petit }">
            <input ref="inputRef" type="text" class="form-control" :class="{ 'form-control-sm': petit }"
                :placeholder="placeholder" :disabled="disabled" autocomplete="off" :value="texte" @input="onInput"
                @focus="onFocus" @keydown.down.prevent="descendre" @keydown.up.prevent="monter"
                @keydown.enter.prevent="validerClavier" @keydown.esc="fermer">

            <button v-if="texte && !disabled" type="button" class="btn btn-outline-secondary" tabindex="-1"
                title="Effacer" @click="effacer">✕</button>

            <slot name="action"></slot>
        </div>

        <div v-if="ouvert && !disabled" class="autocomplete__liste shadow-sm">
            <div v-if="chargement" class="autocomplete__etat">
                <span class="spinner-border spinner-border-sm me-2"></span>Recherche…
            </div>

            <template v-else-if="resultats.length > 0">
                <button v-for="(item, index) in resultats" :key="keyFn(item)" type="button"
                    class="autocomplete__item" :class="{ 'autocomplete__item--actif': index === indexActif }"
                    @mouseenter="indexActif = index" @mousedown.prevent="selectionner(item)">
                    <span class="fw-semibold">{{ labelFn(item) }}</span>
                    <span v-if="sublabelFn(item)" class="text-secondary ms-2">{{ sublabelFn(item) }}</span>
                </button>
            </template>

            <div v-else class="autocomplete__etat fst-italic">{{ messageVide }}</div>
        </div>
    </div>
</template>

<style scoped>
.autocomplete__liste {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 1050;
    margin-top: 2px;
    max-height: 260px;
    overflow-y: auto;
    background: #fff;
    border: 1px solid #dee2e6;
    border-radius: 6px;
}

.autocomplete__item {
    display: block;
    width: 100%;
    padding: 6px 10px;
    border: 0;
    background: transparent;
    text-align: left;
    font-size: 0.8rem;
    line-height: 1.3;
}

.autocomplete__item:hover {
    background: #f0f4ff;
}

.autocomplete__item--actif {
    background: #e8f0fe;
}

.autocomplete__etat {
    padding: 8px 10px;
    font-size: 0.75rem;
    color: #6c757d;
}
</style>

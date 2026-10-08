<script setup>
import { ref } from 'vue'
import { useNotification } from '../../composables/useNotification'
import { exporterListe, imprimerListe, ouvrirFenetreImpression } from '../../utils/exportDocuments'

const props = defineProps({
  title: { type: String, required: true },
  columns: { type: Array, required: true },
  loadRows: { type: Function, required: true },
  disabled: { type: Boolean, default: false }
})

const format = ref('pdf')
const busy = ref(false)
const { notifyError, notifySuccess } = useNotification()

async function exporter() {
  if (busy.value || props.disabled) return
  busy.value = true
  try {
    const lignes = await props.loadRows()
    await exporterListe({ titre: props.title, colonnes: props.columns, lignes, format: format.value })
    notifySuccess(`${lignes.length} ligne(s) exportée(s).`)
  } catch (erreur) {
    notifyError(erreur?.message || 'Impossible de créer le fichier.')
  } finally {
    busy.value = false
  }
}

async function imprimer() {
  if (busy.value || props.disabled) return
  let fenetre
  try {
    fenetre = ouvrirFenetreImpression()
    busy.value = true
    const lignes = await props.loadRows()
    imprimerListe(fenetre, props.title, props.columns, lignes)
  } catch (erreur) {
    fenetre?.close()
    notifyError(erreur?.message || 'Impossible de préparer l’impression.')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="d-inline-flex align-items-center gap-1" role="group" :aria-label="`Exporter ${title}`">
    <select v-model="format" class="form-select form-select-sm" style="width:90px" aria-label="Format d’export" :disabled="busy || disabled">
      <option value="pdf">PDF</option>
      <option value="xlsx">Excel</option>
      <option value="csv">CSV</option>
      <option value="txt">TXT</option>
    </select>
    <button type="button" class="btn btn-outline-primary btn-sm" :disabled="busy || disabled" @click="exporter">
      {{ busy ? 'Préparation…' : 'Exporter' }}
    </button>
    <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="busy || disabled" @click="imprimer">Imprimer</button>
  </div>
</template>

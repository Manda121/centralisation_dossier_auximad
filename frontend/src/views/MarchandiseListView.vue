<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Header from '../components/inc/Header.vue'
import RechercheOt from '../components/marchandise/RechercheOt.vue'
import ListeMarchandises from '../components/marchandise/ListeMarchandises.vue'

const route = useRoute()
const selectedOt = ref(null)
const rechercheOtRef = ref(null)

function onOtSelected(ot) {
  selectedOt.value = ot
}

onMounted(() => {
  const otId = route.query.otId
  if (otId && rechercheOtRef.value) {
    rechercheOtRef.value.chargerParId(otId)
  }
})
</script>

<template>
  <div class="d-flex min-vh-100 bg-light">
    <Header />

    <main class="flex-grow-1 p-4" style="margin-left: 280px;">
      <div class="container-fluid">
        <div class="mb-4">
          <h1 class="h4 fw-bold text-dark mb-1">Marchandises par Ordre de Transit</h1>
          <p class="text-secondary mb-0 small">
            Gestion, conditionnement et détails logistiques des marchandises.
          </p>
        </div>

        <RechercheOt
          ref="rechercheOtRef"
          :initial-numero="route.query.numero || ''"
          @ot-selected="onOtSelected"
        />

        <ListeMarchandises
          v-if="selectedOt"
          :ordre-transit="selectedOt"
        />
      </div>
    </main>
  </div>
</template>

<style scoped>
</style>
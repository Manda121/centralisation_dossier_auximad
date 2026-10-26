<script setup>
import { computed } from 'vue'
import { construireEvolutionMensuelle } from '../../utils/dashboard-chart'

const props = defineProps({ evolution: { type: Array, default: () => [] } })
const graphique = computed(() => construireEvolutionMensuelle(props.evolution))
const formatNombre = valeur => valeur.toLocaleString('fr-FR')
</script>

<template>
  <div v-if="graphique.points.length">
    <div class="evolution-scroll" tabindex="0" role="region" aria-label="Évolution mensuelle des dossiers, graphique défilable">
      <svg :viewBox="`0 0 ${graphique.largeur} ${graphique.hauteur}`" class="evolution-chart"
        role="img" aria-label="Nombre de dossiers créés par mois. Les valeurs sont indiquées au-dessus des points et dans le tableau ci-dessous.">
        <text :x="graphique.gauche" y="16" class="chart-label">Nombre de dossiers</text>
        <g v-for="graduation in graphique.graduations" :key="graduation.valeur">
          <line :x1="graphique.gauche" :x2="graphique.droite" :y1="graduation.y" :y2="graduation.y" class="chart-grid" />
          <text :x="graphique.gauche - 12" :y="graduation.y + 4" text-anchor="end" class="chart-label">
            {{ formatNombre(graduation.valeur) }}
          </text>
        </g>
        <polygon :points="graphique.aire" class="chart-area" />
        <polyline :points="graphique.ligne" class="chart-line" />
        <g v-for="point in graphique.points" :key="point.mois">
          <circle :cx="point.x" :cy="point.y" r="4" class="chart-point">
            <title>{{ point.label }} : {{ formatNombre(point.valeur) }} dossier(s)</title>
          </circle>
          <text :x="point.x" :y="point.y - 12" text-anchor="middle" class="chart-value">{{ formatNombre(point.valeur) }}</text>
          <text :x="point.x" :y="graphique.bas + 26" text-anchor="middle" class="chart-label">
            {{ point.labelCourt }}
          </text>
        </g>
      </svg>
    </div>
    <details class="mt-2">
      <summary class="small text-secondary">Voir les valeurs mensuelles</summary>
      <div class="table-responsive mt-2">
        <table class="table table-sm table-bordered mb-0 small">
          <caption class="visually-hidden">Nombre de dossiers créés par mois</caption>
          <thead class="table-light"><tr><th scope="col">Mois</th><th scope="col" class="text-end">Dossiers créés</th></tr></thead>
          <tbody>
            <tr v-for="point in graphique.points" :key="point.mois">
              <th scope="row" class="fw-normal">{{ point.label }}</th>
              <td class="text-end">{{ formatNombre(point.valeur) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </details>
  </div>
  <p v-else class="text-secondary small text-center py-4 mb-0">Aucune donnée de tendance disponible.</p>
</template>

<style scoped>
.evolution-scroll { overflow-x: auto; }
.evolution-chart { display: block; width: 100%; min-width: 680px; height: auto; }
.chart-grid { stroke: var(--bs-border-color); stroke-width: 1; }
.chart-label { fill: var(--bs-secondary-color); font-size: 12px; }
.chart-area { fill: rgba(var(--bs-primary-rgb), 0.08); }
.chart-line { fill: none; stroke: var(--bs-primary); stroke-width: 2.5; stroke-linejoin: round; }
.chart-point { fill: var(--bs-primary); }
.chart-value { fill: var(--bs-body-color); font-size: 12px; font-weight: 600; }
</style>

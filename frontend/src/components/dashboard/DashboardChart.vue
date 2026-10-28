<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import Chart from 'chart.js/auto'

const props = defineProps({
    type: { type: String, default: 'bar' },
    labels: { type: Array, default: () => [] },
    valeurs: { type: Array, default: () => [] },
    couleurs: { type: Array, default: () => [] },
    ariaLabel: { type: String, default: 'Graphique' }
})

const emit = defineEmits(['selection'])

const couleursDefaut = [
    '#3b6a9c', '#4d9e8a', '#c98b3d', '#8a5aa8', '#a85454',
    '#3d8fc9', '#6fa04d', '#a83d68', '#b08d3d', '#546f88',
    '#7d6fa8', '#54a8a4'
]

const canvasRef = ref(null)
let instance = null

function construireOptions() {
    const options = {
        responsive: true,
        maintainAspectRatio: false,
        cutout: props.type === 'doughnut' ? '62%' : undefined,
        onClick: (evt, elements) => {
            if (elements && elements.length > 0) emit('selection', elements[0].index)
        },
        onHover: (evt, elements) => {
            evt.native.target.style.cursor = elements.length > 0 ? 'pointer' : 'default'
        },
        plugins: {
            legend: props.type === 'doughnut'
                ? { display: true, position: 'right', labels: { boxWidth: 12, padding: 8, font: { size: 11 } } }
                : { display: false },
            tooltip: {
                backgroundColor: 'rgba(30, 42, 58, 0.92)',
                titleFont: { size: 11, weight: '600' },
                bodyFont: { size: 11 },
                padding: 8,
                displayColors: false
            }
        },
        scales: undefined
    }
    if (props.type === 'line') {
        options.scales = {
            y: { beginAtZero: true, ticks: { precision: 0, font: { size: 10 }, color: '#5e6f88' }, grid: { color: '#eef1f5' } },
            x: { ticks: { font: { size: 10 }, color: '#5e6f88', maxRotation: 0, autoSkip: true, maxTicksLimit: 8 }, grid: { display: false } }
        }
    } else if (props.type === 'barH') {
        options.indexAxis = 'y'
        options.scales = {
            x: { beginAtZero: true, ticks: { precision: 0, font: { size: 10 }, color: '#5e6f88' }, grid: { color: '#eef1f5' } },
            y: { ticks: { font: { size: 11 }, color: '#1e2a3a' }, grid: { display: false } }
        }
    }
    return options
}

function construireDatasets() {
    if (props.type === 'doughnut') {
        const couleurs = props.labels.map((_, i) =>
            props.couleurs[i] || couleursDefaut[i % couleursDefaut.length])
        return [{ data: props.valeurs, backgroundColor: couleurs, borderWidth: 2, borderColor: '#ffffff' }]
    }
    if (props.type === 'line') {
        return [{
            data: props.valeurs,
            borderColor: '#3b6a9c',
            backgroundColor: 'rgba(59, 106, 156, 0.12)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
            pointBackgroundColor: '#3b6a9c',
            pointHoverRadius: 5
        }]
    }
    // barres horizontales
    const couleurs = props.valeurs.map((_, i) =>
        props.couleurs[i] || couleursDefaut[i % couleursDefaut.length])
    return [{ data: props.valeurs, backgroundColor: couleurs, borderWidth: 0, borderRadius: 3, maxBarThickness: 22 }]
}

function rendre() {
    if (!canvasRef.value) return
    if (instance) {
        instance.data.labels = props.labels
        instance.data.datasets = construireDatasets()
        instance.update()
        return
    }
    instance = new Chart(canvasRef.value, {
        type: props.type === 'barH' ? 'bar' : props.type,
        data: { labels: props.labels, datasets: construireDatasets() },
        options: construireOptions()
    })
}

onMounted(rendre)
watch(() => [props.labels, props.valeurs], rendre, { deep: true })
onBeforeUnmount(() => {
    if (instance) {
        instance.destroy()
        instance = null
    }
})
</script>

<template>
    <div class="graphique-wrapper" :aria-label="ariaLabel" role="img">
        <canvas ref="canvasRef"></canvas>
    </div>
</template>

<style scoped>
.graphique-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 180px;
}
</style>

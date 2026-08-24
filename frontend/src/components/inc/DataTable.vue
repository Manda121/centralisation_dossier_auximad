<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
    columns: {
        type: Array,
        required: true
    },
    fetchFunction: {
        type: Function,
        required: true
    },
    filters: {
        type: Object,
        default: () => ({})
    },
    defaultSort: {
        type: String,
        default: ''
    },
    defaultSortDir: {
        type: String,
        default: 'desc'
    },
    pageSize: {
        type: Number,
        default: 15
    },
    rowClickable: {
        type: Boolean,
        default: false
    }
})

const emit = defineEmits(['row-click'])

const data = ref([])
const loading = ref(false)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const sortBy = ref(props.defaultSort)
const sortDir = ref(props.defaultSortDir)
const showFilters = ref(false)
const localFilters = ref({ ...props.filters })

const hasFilters = computed(() => Object.keys(props.filters).length > 0)
const activeFilterCount = computed(() => {
    return Object.values(localFilters.value).filter(v => v !== '' && v !== null && v !== undefined).length
})

const visiblePages = computed(() => {
    const total = totalPages.value
    const current = currentPage.value
    const pages = []

    if (total <= 7) {
        for (let i = 0; i < total; i++) pages.push(i)
        return pages
    }

    pages.push(0)
    if (current > 2) pages.push('...')

    const start = Math.max(1, current - 1)
    const end = Math.min(total - 2, current + 1)
    for (let i = start; i <= end; i++) pages.push(i)

    if (current < total - 3) pages.push('...')
    pages.push(total - 1)

    return pages
})

async function fetchData() {
    loading.value = true
    try {
        const response = await props.fetchFunction(
            currentPage.value,
            props.pageSize,
            sortBy.value,
            sortDir.value,
            localFilters.value
        )
        data.value = response.content || []
        totalElements.value = response.totalElements || 0
        totalPages.value = response.totalPages || 0
    } catch (error) {
        console.error('DataTable — erreur de chargement:', error)
        data.value = []
        totalElements.value = 0
        totalPages.value = 0
    } finally {
        loading.value = false
    }
}

function goToPage(page) {
    if (page < 0 || page >= totalPages.value) return
    currentPage.value = page
    fetchData()
}

function toggleSort(key) {
    if (sortBy.value === key) {
        sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
    } else {
        sortBy.value = key
        sortDir.value = 'asc'
    }
    currentPage.value = 0
    fetchData()
}

function applyFilters() {
    currentPage.value = 0
    fetchData()
}

function resetFilters() {
    Object.keys(localFilters.value).forEach(key => {
        localFilters.value[key] = ''
    })
    currentPage.value = 0
    fetchData()
}

function getCellValue(row, key) {
    return key.split('.').reduce((obj, k) => obj?.[k], row)
}

function formatCellValue(value, col) {
    if (value === null || value === undefined) return '—'
    if (col.format === 'date' && value) {
        try {
            return new Date(value).toLocaleDateString('fr-FR')
        } catch {
            return value
        }
    }
    if (col.format === 'datetime' && value) {
        try {
            return new Date(value).toLocaleString('fr-FR')
        } catch {
            return value
        }
    }
    return value
}

function refresh() {
    fetchData()
}

watch(() => props.filters, (newFilters) => {
    localFilters.value = { ...newFilters }
}, { deep: true })

onMounted(() => {
    fetchData()
})

defineExpose({ refresh })
</script>

<template>
    <div class="datatable-container">
        <div v-if="hasFilters" class="mb-2">
            <button type="button" class="btn btn-outline-secondary btn-sm" @click="showFilters = !showFilters">
                {{ showFilters ? 'Masquer les filtres' : 'Filtres avancés' }}
                <span v-if="activeFilterCount > 0" class="badge bg-secondary ms-1">{{ activeFilterCount }}</span>
            </button>
        </div>

        <div v-if="showFilters && hasFilters" class="border rounded p-3 mb-3 bg-white">
            <slot name="filters" :filters="localFilters" :apply="applyFilters" :reset="resetFilters"></slot>
            <div class="d-flex justify-content-end gap-2 mt-3 pt-2 border-top">
                <button type="button" class="btn btn-secondary btn-sm" @click="resetFilters">Réinitialiser</button>
                <button type="button" class="btn btn-primary btn-sm" @click="applyFilters">Rechercher</button>
            </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mb-2 small text-secondary">
            <span v-if="totalElements > 0">
                {{ totalElements }} résultat{{ totalElements > 1 ? 's' : '' }}
                <span v-if="totalPages > 1">— Page {{ currentPage + 1 }} / {{ totalPages }}</span>
            </span>
            <span v-else-if="!loading">Aucun résultat</span>
        </div>

        <div class="table-responsive">
            <table class="table table-hover table-sm align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th v-for="col in columns" :key="col.key" :style="col.width ? `width: ${col.width}` : ''"
                            :class="{ 'sortable': col.sortable !== false }"
                            @click="col.sortable !== false && toggleSort(col.key)">
                            <span>{{ col.label }}</span>
                            <span v-if="col.sortable !== false" class="ms-1 text-secondary">
                                <template v-if="sortBy === col.key">{{ sortDir === 'asc' ? '▲' : '▼' }}</template>
                                <template v-else>⇅</template>
                            </span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="loading">
                        <td :colspan="columns.length" class="text-center py-4">
                            <div class="spinner-border spinner-border-sm text-secondary me-2" role="status"></div>
                            Chargement...
                        </td>
                    </tr>
                    <tr v-else-if="data.length === 0">
                        <td :colspan="columns.length" class="text-center py-4 text-secondary">
                            Aucune donnée disponible
                        </td>
                    </tr>
                    <template v-else>
                        <tr v-for="(row, index) in data" :key="row.id || index"
                            :class="{ 'table-active': false }" @click="$emit('row-click', row)">
                            <td v-for="col in columns" :key="col.key">
                                <slot :name="`cell-${col.key}`" :row="row" :value="getCellValue(row, col.key)">
                                    {{ formatCellValue(getCellValue(row, col.key), col) }}
                                </slot>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>

        <div v-if="totalPages > 1" class="d-flex justify-content-center align-items-center gap-1 mt-3">
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="currentPage === 0" @click="goToPage(0)">«</button>
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">‹</button>

            <template v-for="p in visiblePages" :key="p">
                <button v-if="p === '...'" type="button" class="btn btn-outline-secondary btn-sm" disabled>…</button>
                <button v-else type="button" class="btn btn-sm" :class="p === currentPage ? 'btn-primary' : 'btn-outline-secondary'" @click="goToPage(p)">
                    {{ p + 1 }}
                </button>
            </template>

            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="currentPage >= totalPages - 1" @click="goToPage(currentPage + 1)">›</button>
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="currentPage >= totalPages - 1" @click="goToPage(totalPages - 1)">»</button>
        </div>
    </div>
</template>

<style scoped>
.sortable {
    cursor: pointer;
    user-select: none;
}
.sortable:hover {
    background-color: #e9ecef;
}
</style>
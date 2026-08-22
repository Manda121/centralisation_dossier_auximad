<template>
    <div class="datatable-container">
        <!-- Tiroir filtres avancés -->
        <div v-if="hasFilters" class="datatable-filter-toggle">
            <button type="button" class="btn btn-outline-secondary btn-sm d-flex align-items-center gap-2"
                @click="showFilters = !showFilters">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
                    <path
                        d="M1.5 1.5A.5.5 0 0 1 2 1h12a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.128.334L10 8.692V13.5a.5.5 0 0 1-.342.474l-3 1A.5.5 0 0 1 6 14.5V8.692L1.628 3.834A.5.5 0 0 1 1.5 3.5z" />
                </svg>
                <span>{{ showFilters ? 'Masquer les filtres' : 'Filtres avancés' }}</span>
                <span v-if="activeFilterCount > 0" class="badge bg-primary rounded-pill">{{ activeFilterCount
                }}</span>
            </button>
        </div>

        <!-- Panneau de filtres repliable -->
        <transition name="slide">
            <div v-if="showFilters && hasFilters" class="datatable-filters">
                <slot name="filters" :filters="localFilters" :apply="applyFilters" :reset="resetFilters">
                </slot>
                <div class="datatable-filters-actions">
                    <button type="button" class="btn-secondary btn-sm" @click="resetFilters">Réinitialiser</button>
                    <button type="button" class="btn-primary btn-sm" @click="applyFilters">Rechercher</button>
                </div>
            </div>
        </transition>

        <!-- Info résultats & tri -->
        <div class="datatable-header">
            <div class="datatable-info text-muted">
                <span v-if="totalElements > 0">
                    {{ totalElements }} résultat{{ totalElements > 1 ? 's' : '' }}
                    <span v-if="totalPages > 1">— Page {{ currentPage + 1 }} / {{ totalPages }}</span>
                </span>
                <span v-else-if="!loading">Aucun résultat</span>
            </div>
        </div>

        <!-- Tableau -->
        <div class="datatable-table-wrapper">
            <table class="datatable-table">
                <thead>
                    <tr>
                        <th v-for="col in columns" :key="col.key" :style="col.width ? `width: ${col.width}` : ''"
                            :class="{ 'sortable': col.sortable !== false, 'sorted': sortBy === col.key }"
                            @click="col.sortable !== false && toggleSort(col.key)">
                            <div class="th-content">
                                <span>{{ col.label }}</span>
                                <span v-if="col.sortable !== false" class="sort-icon">
                                    <template v-if="sortBy === col.key">
                                        {{ sortDir === 'asc' ? '▲' : '▼' }}
                                    </template>
                                    <template v-else>⇅</template>
                                </span>
                            </div>
                        </th>
                        <th v-if="$slots.actions" style="width: 100px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="loading">
                        <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="text-center py-4">
                            <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                            Chargement...
                        </td>
                    </tr>
                    <tr v-else-if="data.length === 0">
                        <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="text-center py-4 text-muted">
                            Aucune donnée disponible
                        </td>
                    </tr>
                    <template v-else>
                        <tr v-for="(row, index) in data" :key="row.id || index" class="datatable-row"
                            :class="{ 'clickable': rowClickable }" @click="$emit('row-click', row)">
                            <td v-for="col in columns" :key="col.key">
                                <slot :name="`cell-${col.key}`" :row="row" :value="getCellValue(row, col.key)">
                                    {{ formatCellValue(getCellValue(row, col.key), col) }}
                                </slot>
                            </td>
                            <td v-if="$slots.actions">
                                <slot name="actions" :row="row"></slot>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="datatable-pagination">
            <button type="button" class="btn-secondary btn-sm" :disabled="currentPage === 0" @click="goToPage(0)">
                «
            </button>
            <button type="button" class="btn-secondary btn-sm" :disabled="currentPage === 0"
                @click="goToPage(currentPage - 1)">
                ‹
            </button>

            <template v-for="p in visiblePages" :key="p">
                <button v-if="p === '...'" type="button" class="btn-secondary btn-sm" disabled>…</button>
                <button v-else type="button" class="btn-sm" :class="p === currentPage ? 'btn-primary' : 'btn-secondary'"
                    @click="goToPage(p)">
                    {{ p + 1 }}
                </button>
            </template>

            <button type="button" class="btn-secondary btn-sm" :disabled="currentPage >= totalPages - 1"
                @click="goToPage(currentPage + 1)">
                ›
            </button>
            <button type="button" class="btn-secondary btn-sm" :disabled="currentPage >= totalPages - 1"
                @click="goToPage(totalPages - 1)">
                »
            </button>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
    columns: {
        type: Array,
        required: true
        // Format: [{ key: 'nom', label: 'Nom', sortable: true, width: '200px', format: 'date' }]
    },
    fetchFunction: {
        type: Function,
        required: true
        // Signature: (page, size, sortBy, sortDir, filters) => Promise<PageResponse>
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

// État
const data = ref([])
const loading = ref(false)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const sortBy = ref(props.defaultSort)
const sortDir = ref(props.defaultSortDir)
const showFilters = ref(false)
const localFilters = ref({ ...props.filters })

// Nombre de filtres actifs
const hasFilters = computed(() => Object.keys(props.filters).length > 0)
const activeFilterCount = computed(() => {
    return Object.values(localFilters.value).filter(v => v !== '' && v !== null && v !== undefined).length
})

// Pages visibles pour la pagination
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

// Récupérer les données
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

// Navigation
function goToPage(page) {
    if (page < 0 || page >= totalPages.value) return
    currentPage.value = page
    fetchData()
}

// Tri
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

// Filtres
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

// Utilitaires cellules
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

// Méthode publique pour rafraîchir depuis le parent
function refresh() {
    fetchData()
}

// Watcher pour changements externes de filtres
watch(() => props.filters, (newFilters) => {
    localFilters.value = { ...newFilters }
}, { deep: true })

onMounted(() => {
    fetchData()
})

defineExpose({ refresh })
</script>

<style scoped>
.datatable-container {
    width: 100%;
}

.datatable-filter-toggle {
    margin-bottom: 1rem;
}

.datatable-filters {
    padding: 1.25rem;
    margin-bottom: 1rem;
    background: var(--color-surface, #ffffff);
    border: 1px solid var(--color-border, #e2e8f0);
    border-radius: var(--radius-lg, 14px);
    box-shadow: var(--shadow-sm, 0 1px 2px rgba(15, 23, 42, 0.06));
}

.datatable-filters-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 1rem;
    padding-top: 1rem;
    border-top: 1px solid var(--color-border, #e2e8f0);
}

.datatable-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
    font-size: 0.85rem;
}

.datatable-table-wrapper {
    overflow-x: auto;
}

.datatable-table {
    margin: 0;
}

.datatable-table th.sortable {
    cursor: pointer;
    user-select: none;
}

.datatable-table th.sortable:hover {
    background: #e2e8f0;
}

.datatable-table th.sorted {
    color: var(--bo-primary, #4f46e5);
}

.th-content {
    display: flex;
    align-items: center;
    gap: 0.35rem;
}

.sort-icon {
    font-size: 0.7rem;
    opacity: 0.5;
}

.sorted .sort-icon {
    opacity: 1;
}

.datatable-row.clickable {
    cursor: pointer;
}

.datatable-row.clickable:hover {
    background: #f0f4ff;
}

.datatable-pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.35rem;
    margin-top: 1rem;
    padding-top: 1rem;
}

.datatable-pagination button {
    min-width: 36px;
    height: 36px;
    padding: 0 0.5rem;
    font-size: 0.85rem;
}

/* Transition tiroir */
.slide-enter-active,
.slide-leave-active {
    transition: all 0.25s ease;
    overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
    opacity: 0;
    max-height: 0;
    padding-top: 0;
    padding-bottom: 0;
    margin-bottom: 0;
}

.slide-enter-to,
.slide-leave-from {
    opacity: 1;
    max-height: 600px;
}
</style>

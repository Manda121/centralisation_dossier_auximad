<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import Skeleton from './Skeleton.vue'

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
const loading = ref(true)
const loadError = ref('')
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
    loadError.value = ''
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
        loadError.value = error?.response?.status === 403
            ? 'Vous n’avez pas accès à ces données.'
            : 'Impossible de charger les données. Vérifiez la connexion au serveur et réessayez.'
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
    if (props.filters && typeof props.filters === 'object') {
        Object.assign(props.filters, localFilters.value)
    }
    fetchData()
}

function resetFilters() {
    Object.keys(localFilters.value).forEach(key => {
        localFilters.value[key] = ''
    })
    if (props.filters && typeof props.filters === 'object') {
        Object.assign(props.filters, localFilters.value)
    }
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

function refresh(resetPage = false) {
    if (resetPage) {
        currentPage.value = 0
    }
    if (props.filters && typeof props.filters === 'object') {
        Object.assign(localFilters.value, props.filters)
    }
    fetchData()
}

watch(() => props.filters, (newFilters) => {
    if (newFilters && typeof newFilters === 'object') {
        localFilters.value = { ...newFilters }
    }
}, { deep: true })

onMounted(() => {
    fetchData()
})

defineExpose({ refresh })
</script>

<template>
    <div class="datatable">
        <!-- Toolbar -->
        <div class="datatable__toolbar">
            <div class="datatable__toolbar-left">
                <span v-if="totalElements > 0" class="datatable__count">
                    {{ totalElements }} résultat{{ totalElements > 1 ? 's' : '' }}
                    <span v-if="totalPages > 1">— Page {{ currentPage + 1 }} / {{ totalPages }}</span>
                </span>
                <span v-else-if="!loading" class="datatable__count">Aucun résultat</span>
            </div>

            <div class="datatable__toolbar-right">
                <button
                    v-if="hasFilters"
                    type="button"
                    class="datatable__filter-toggle"
                    :class="{ 'datatable__filter-toggle--active': showFilters }"
                    @click="showFilters = !showFilters"
                >
                    <span class="datatable__filter-toggle-icon">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="4" y1="21" x2="4" y2="14"></line>
                            <line x1="4" y1="10" x2="4" y2="3"></line>
                            <line x1="12" y1="21" x2="12" y2="12"></line>
                            <line x1="12" y1="8" x2="12" y2="3"></line>
                            <line x1="20" y1="21" x2="20" y2="16"></line>
                            <line x1="20" y1="12" x2="20" y2="3"></line>
                            <line x1="1" y1="14" x2="7" y2="14"></line>
                            <line x1="9" y1="8" x2="15" y2="8"></line>
                            <line x1="17" y1="16" x2="23" y2="16"></line>
                        </svg>
                    </span>
                    Filtres
                    <span v-if="activeFilterCount > 0" class="datatable__filter-badge">{{ activeFilterCount }}</span>
                </button>
            </div>
        </div>

        <!-- Filters panel -->
        <div v-if="showFilters && hasFilters" class="datatable__filters">
            <slot name="filters" :filters="localFilters" :apply="applyFilters" :reset="resetFilters"></slot>
            <div class="datatable__filters-actions">
                <button type="button" class="datatable__btn datatable__btn--secondary" @click="resetFilters">
                    Réinitialiser
                </button>
                <button type="button" class="datatable__btn datatable__btn--primary" @click="applyFilters">
                    Rechercher
                </button>
            </div>
        </div>

        <!-- Table wrapper -->
        <div class="datatable__table-wrap">
            <table class="datatable__table">
                <thead class="datatable__thead">
                    <tr>
                        <th
                            v-for="col in columns"
                            :key="col.key"
                            :style="col.width ? `width: ${col.width}` : ''"
                            class="datatable__th"
                            :class="{
                                'datatable__th--sortable': col.sortable !== false,
                                'datatable__th--sorted': sortBy === col.key
                            }"
                            @click="col.sortable !== false && toggleSort(col.key)"
                        >
                            <span class="datatable__th-label">{{ col.label }}</span>
                            <span v-if="col.sortable !== false" class="datatable__th-sort">
                                <template v-if="sortBy === col.key">
                                    <span class="datatable__th-sort-icon">{{ sortDir === 'asc' ? '↑' : '↓' }}</span>
                                </template>
                                <template v-else>
                                    <span class="datatable__th-sort-icon datatable__th-sort-icon--muted">↕</span>
                                </template>
                            </span>
                        </th>
                    </tr>
                </thead>
                <tbody class="datatable__tbody">
                    <!-- Loading skeleton -->
                    <template v-if="loading">
                        <tr v-for="n in 8" :key="'sk-' + n" class="datatable__row datatable__row--skeleton">
                            <td v-for="col in columns" :key="col.key" class="datatable__td">
                                <Skeleton :width="n % 2 === 0 ? '72%' : '88%'" height="12px" />
                            </td>
                        </tr>
                    </template>

                    <!-- Empty -->
                    <tr v-else-if="loadError">
                        <td :colspan="columns.length" class="datatable__empty" role="alert">
                            <span>{{ loadError }}</span>
                            <button type="button" class="datatable__btn datatable__btn--secondary" @click="fetchData">
                                Réessayer
                            </button>
                        </td>
                    </tr>
                    <tr v-else-if="data.length === 0">
                        <td :colspan="columns.length" class="datatable__empty">
                            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="datatable__empty-icon">
                                <rect x="2" y="3" width="20" height="18" rx="2" ry="2"></rect>
                                <line x1="8" y1="21" x2="16" y2="21"></line>
                                <line x1="12" y1="17" x2="12" y2="21"></line>
                            </svg>
                            <span>Aucune donnée disponible</span>
                        </td>
                    </tr>

                    <!-- Data rows -->
                    <tr
                        v-else
                        v-for="(row, index) in data"
                        :key="row.id || index"
                        class="datatable__row"
                        :class="{ 'datatable__row--clickable': rowClickable }"
                        @click="rowClickable && emit('row-click', row)"
                    >
                        <td v-for="col in columns" :key="col.key" class="datatable__td">
                            <slot :name="`cell-${col.key}`" :row="row" :value="getCellValue(row, col.key)">
                                {{ formatCellValue(getCellValue(row, col.key), col) }}
                            </slot>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="datatable__pagination">
            <button
                type="button"
                class="datatable__page-btn"
                :disabled="currentPage === 0"
                @click="goToPage(0)"
            >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="11 17 6 12 11 7"></polyline>
                    <polyline points="18 17 13 12 18 7"></polyline>
                </svg>
            </button>
            <button
                type="button"
                class="datatable__page-btn"
                :disabled="currentPage === 0"
                @click="goToPage(currentPage - 1)"
            >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="15 18 9 12 15 6"></polyline>
                </svg>
            </button>

            <template v-for="p in visiblePages" :key="p">
                <button
                    v-if="p === '...'"
                    type="button"
                    class="datatable__page-btn datatable__page-btn--ellipsis"
                    disabled
                >…</button>
                <button
                    v-else
                    type="button"
                    class="datatable__page-btn"
                    :class="{ 'datatable__page-btn--active': p === currentPage }"
                    @click="goToPage(p)"
                >{{ p + 1 }}</button>
            </template>

            <button
                type="button"
                class="datatable__page-btn"
                :disabled="currentPage >= totalPages - 1"
                @click="goToPage(currentPage + 1)"
            >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
            </button>
            <button
                type="button"
                class="datatable__page-btn"
                :disabled="currentPage >= totalPages - 1"
                @click="goToPage(totalPages - 1)"
            >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="13 17 18 12 13 7"></polyline>
                    <polyline points="6 17 11 12 6 7"></polyline>
                </svg>
            </button>
        </div>
    </div>
</template>

<style scoped>
/* ── Variables ── */
.datatable {
    --dt-bg: #ffffff;
    --dt-border: #d0d5dd;
    --dt-border-light: #e4e7ec;
    --dt-text: #1a2332;
    --dt-text-secondary: #4a5568;
    --dt-text-muted: #8a94a6;
    --dt-header-bg: #f2f4f7;
    --dt-header-text: #1a2332;
    --dt-row-hover: #f5f6f9;
    --dt-row-stripe: #fafbfc;
    --dt-primary: #3b6a9c;
    --dt-primary-hover: #2d5380;
    --dt-primary-light: #e8eef6;
    --dt-primary-text: #3b6a9c;
    --dt-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    --dt-radius: 6px;
    --dt-font: system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
    font-family: var(--dt-font);
    color: var(--dt-text);
    font-size: 14px;
    line-height: 1.5;
}

/* ── Toolbar ── */
.datatable__toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
}

.datatable__toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
}

.datatable__toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
}

.datatable__count {
    font-size: 13px;
    color: var(--dt-text-secondary);
}

/* ── Filter toggle ── */
.datatable__filter-toggle {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    font-size: 13px;
    font-weight: 500;
    color: var(--dt-text-secondary);
    background: var(--dt-header-bg);
    border: 1px solid var(--dt-border-light);
    border-radius: var(--dt-radius);
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, color 0.15s;
    user-select: none;
}

.datatable__filter-toggle:hover {
    background: #e8eaef;
    border-color: #c5cad4;
}

.datatable__filter-toggle--active {
    color: var(--dt-primary-text);
    background: var(--dt-primary-light);
    border-color: var(--dt-primary);
}

.datatable__filter-toggle-icon {
    display: inline-flex;
    opacity: 0.7;
}

.datatable__filter-toggle--active .datatable__filter-toggle-icon {
    opacity: 1;
}

.datatable__filter-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    font-size: 11px;
    font-weight: 600;
    color: #fff;
    background: var(--dt-primary);
    border-radius: 999px;
    line-height: 1;
}

/* ── Filters panel ── */
.datatable__filters {
    background: #f8f9fc;
    border: 1px solid var(--dt-border-light);
    border-radius: var(--dt-radius);
    padding: 18px 20px 14px;
    margin-bottom: 16px;
}

.datatable__filters-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid var(--dt-border-light);
}

/* ── Buttons ── */
.datatable__btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 6px 18px;
    font-size: 13px;
    font-weight: 500;
    border: 1px solid transparent;
    border-radius: var(--dt-radius);
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, color 0.15s;
    user-select: none;
    font-family: inherit;
    line-height: 1.4;
}

.datatable__btn--secondary {
    color: var(--dt-text-secondary);
    background: #fff;
    border-color: var(--dt-border-light);
}

.datatable__btn--secondary:hover {
    background: var(--dt-header-bg);
    border-color: #c5cad4;
}

.datatable__btn--primary {
    color: #fff;
    background: var(--dt-primary);
    border-color: var(--dt-primary);
}

.datatable__btn--primary:hover {
    background: var(--dt-primary-hover);
    border-color: var(--dt-primary-hover);
}

/* ── Table ── */
.datatable__table-wrap {
    border: 1px solid var(--dt-border-light);
    border-radius: var(--dt-radius);
    overflow: hidden;
    background: var(--dt-bg);
    box-shadow: var(--dt-shadow);
}

.datatable__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}

/* ── Thead ── */
.datatable__thead {
    background: var(--dt-header-bg);
}

.datatable__th {
    padding: 10px 14px;
    text-align: left;
    font-weight: 600;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    color: var(--dt-text-secondary);
    border-bottom: 1px solid var(--dt-border-light);
    white-space: nowrap;
    user-select: none;
}

.datatable__th--sortable {
    cursor: pointer;
    transition: background 0.12s;
}

.datatable__th--sortable:hover {
    background: #e8eaef;
}

.datatable__th--sorted {
    color: var(--dt-primary-text);
}

.datatable__th-label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.datatable__th-sort {
    display: inline-flex;
    align-items: center;
    margin-left: 4px;
    font-weight: 400;
    opacity: 0.5;
}

.datatable__th--sorted .datatable__th-sort {
    opacity: 1;
}

.datatable__th-sort-icon--muted {
    opacity: 0.3;
}

/* ── Tbody ── */
.datatable__tbody {
    background: var(--dt-bg);
}

.datatable__row {
    transition: background 0.1s;
    border-bottom: 1px solid #f0f1f4;
}

.datatable__row:last-child {
    border-bottom: none;
}

.datatable__row:nth-child(even) {
    background: var(--dt-row-stripe);
}

.datatable__row:hover {
    background: var(--dt-row-hover);
}

.datatable__row--clickable {
    cursor: pointer;
}

.datatable__td {
    padding: 10px 14px;
    vertical-align: middle;
    color: var(--dt-text);
    word-break: break-word;
}

/* ── Empty state ── */
.datatable__empty {
    padding: 40px 20px;
    text-align: center;
    color: var(--dt-text-muted);
    font-size: 14px;
}

.datatable__empty-icon {
    display: block;
    margin: 0 auto 10px;
    color: var(--dt-text-muted);
    opacity: 0.5;
}

/* ── Spinner ── */
.datatable__spinner {
    display: inline-block;
    width: 18px;
    height: 18px;
    margin-right: 10px;
    border: 2px solid var(--dt-border-light);
    border-top-color: var(--dt-primary);
    border-radius: 50%;
    animation: dt-spin 0.7s linear infinite;
    vertical-align: middle;
}

@keyframes dt-spin {
    to {
        transform: rotate(360deg);
    }
}

/* ── Pagination ── */
.datatable__pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 18px;
    flex-wrap: wrap;
}

.datatable__page-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 34px;
    height: 34px;
    padding: 0 8px;
    font-size: 13px;
    font-weight: 500;
    color: var(--dt-text-secondary);
    background: transparent;
    border: 1px solid transparent;
    border-radius: var(--dt-radius);
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, color 0.15s;
    user-select: none;
    font-family: inherit;
}

.datatable__page-btn:hover:not(:disabled):not(.datatable__page-btn--active) {
    background: var(--dt-header-bg);
    border-color: var(--dt-border-light);
}

.datatable__page-btn:disabled {
    opacity: 0.35;
    cursor: not-allowed;
}

.datatable__page-btn--active {
    color: #fff;
    background: var(--dt-primary);
    border-color: var(--dt-primary);
}

.datatable__page-btn--active:hover {
    background: var(--dt-primary-hover);
    border-color: var(--dt-primary-hover);
}

.datatable__page-btn--ellipsis {
    cursor: default;
    color: var(--dt-text-muted);
}

.datatable__page-btn--ellipsis:hover {
    background: transparent;
    border-color: transparent;
}

/* ── Responsive adjustments ── */
@media (max-width: 640px) {
    .datatable__toolbar {
        flex-direction: column;
        align-items: stretch;
        gap: 10px;
    }

    .datatable__toolbar-left,
    .datatable__toolbar-right {
        justify-content: center;
    }

    .datatable__th,
    .datatable__td {
        padding: 8px 10px;
        font-size: 12px;
    }

    .datatable__filters {
        padding: 14px 14px 10px;
    }

    .datatable__filters-actions {
        flex-wrap: wrap;
        justify-content: center;
    }

    .datatable__pagination {
        gap: 3px;
    }

    .datatable__page-btn {
        min-width: 30px;
        height: 30px;
        font-size: 12px;
        padding: 0 6px;
    }
}
</style>

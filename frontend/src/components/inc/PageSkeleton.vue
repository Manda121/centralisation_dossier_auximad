<script setup>
import Header from './Header.vue'
import Skeleton from './Skeleton.vue'

defineProps({
    variant: {
        type: String,
        default: 'list',
        validator: (v) => ['list', 'detail', 'form'].includes(v)
    },
    withLayout: {
        type: Boolean,
        default: true
    }
})
</script>

<template>
    <div :class="withLayout ? 'd-flex min-vh-100 bg-light' : ''">
        <Header v-if="withLayout" />

        <main :class="withLayout ? 'flex-grow-1 p-4' : ''" :style="withLayout ? 'margin-left: 280px;' : ''">
            <div class="container-fluid">
                <!-- Liste -->
                <template v-if="variant === 'list'">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <Skeleton width="220px" height="22px" class="d-block mb-2" />
                            <Skeleton width="320px" height="12px" class="d-block" />
                        </div>
                        <Skeleton width="140px" height="32px" radius="6px" />
                    </div>
                    <div class="card border shadow-sm">
                        <div class="card-body p-3">
                            <div class="d-flex justify-content-between mb-3">
                                <Skeleton width="160px" height="12px" />
                                <Skeleton width="90px" height="28px" radius="6px" />
                            </div>
                            <div class="table-responsive">
                                <table class="table mb-0">
                                    <thead>
                                        <tr>
                                            <th v-for="n in 5" :key="'h' + n" class="border-0">
                                                <Skeleton width="70%" height="10px" />
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr v-for="r in 8" :key="'r' + r">
                                            <td v-for="c in 5" :key="'c' + c" class="py-3">
                                                <Skeleton :width="c === 1 ? '55%' : '80%'" height="12px" />
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </template>

                <!-- Détail -->
                <template v-else-if="variant === 'detail'">
                    <div class="d-flex justify-content-between align-items-start mb-4">
                        <div>
                            <Skeleton width="240px" height="24px" class="d-block mb-2" />
                            <Skeleton width="180px" height="12px" class="d-block" />
                        </div>
                        <Skeleton width="120px" height="32px" radius="6px" />
                    </div>
                    <div class="row g-3">
                        <div class="col-lg-8">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white py-3">
                                    <Skeleton width="180px" height="14px" />
                                </div>
                                <div class="card-body">
                                    <div class="row g-3">
                                        <div v-for="n in 8" :key="'d' + n" class="col-md-6">
                                            <Skeleton width="90px" height="10px" class="d-block mb-2" />
                                            <Skeleton width="70%" height="14px" />
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="card border shadow-sm">
                                <div class="card-header bg-white py-3">
                                    <Skeleton width="150px" height="14px" />
                                </div>
                                <div class="card-body">
                                    <div class="row g-3">
                                        <div v-for="n in 3" :key="'p' + n" class="col-md-4">
                                            <Skeleton width="80px" height="10px" class="d-block mb-2" />
                                            <Skeleton width="60%" height="16px" />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-4">
                            <div class="card border shadow-sm mb-3">
                                <div class="card-header bg-white py-3">
                                    <Skeleton width="140px" height="14px" />
                                </div>
                                <div class="card-body">
                                    <Skeleton width="50%" height="16px" class="d-block mb-3" />
                                    <Skeleton width="90%" height="12px" class="d-block mb-2" />
                                    <Skeleton width="75%" height="12px" class="d-block" />
                                </div>
                            </div>
                            <div class="card border shadow-sm">
                                <div class="card-header bg-white py-3">
                                    <Skeleton width="120px" height="14px" />
                                </div>
                                <div class="card-body">
                                    <Skeleton width="100%" height="12px" class="d-block mb-2" />
                                    <Skeleton width="80%" height="12px" class="d-block mb-3" />
                                    <Skeleton width="100%" height="32px" radius="6px" />
                                </div>
                            </div>
                        </div>
                    </div>
                </template>

                <!-- Formulaire -->
                <template v-else>
                    <div class="card border shadow-sm p-4">
                        <Skeleton width="260px" height="22px" class="d-block mb-4" />
                        <div class="row g-4">
                            <div v-for="n in 9" :key="'f' + n" class="col-md-4">
                                <Skeleton width="110px" height="10px" class="d-block mb-2" />
                                <Skeleton width="100%" height="32px" radius="6px" />
                            </div>
                        </div>
                        <div class="d-flex justify-content-end gap-2 mt-5 pt-3 border-top">
                            <Skeleton width="90px" height="32px" radius="6px" />
                            <Skeleton width="130px" height="32px" radius="6px" />
                        </div>
                    </div>
                </template>
            </div>
        </main>
    </div>
</template>

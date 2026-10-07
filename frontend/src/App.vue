<script setup>
import { ref } from 'vue'
import { useRouter, START_LOCATION } from 'vue-router'
import PageSkeleton from './components/inc/PageSkeleton.vue'
import NotificationModal from './components/inc/NotificationModal.vue'

const router = useRouter()
const isNavigating = ref(false)
const currentVariant = ref('list')

function skeletonVariant(name) {
    if (!name) return 'list'
    if (name.includes('detail')) return 'detail'
    if (name.includes('creer') || name.includes('wizard') || name.includes('nouveau')) return 'form'
    return 'list'
}

router.beforeEach((to, from) => {
    if (to.name === 'login') {
        isNavigating.value = false
        return
    }
    if (from === START_LOCATION || to.fullPath !== from.fullPath) {
        currentVariant.value = skeletonVariant(String(to.name || ''))
        isNavigating.value = true
    }
})

router.afterEach(() => {
    isNavigating.value = false
})

router.onError(() => {
    isNavigating.value = false
})
</script>

<template>
    <router-view v-slot="{ Component }">
        <PageSkeleton v-if="isNavigating" :variant="currentVariant" />
        <component :is="Component" v-show="!isNavigating" />
    </router-view>

    <!-- Notifications et confirmations communes à toutes les pages -->
    <NotificationModal />
</template>

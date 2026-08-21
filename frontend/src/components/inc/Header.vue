<template>
    <header class="main-header glass-panel">
        <div class="header-container">
            <!-- Logo / Title -->
            <div class="header-brand">
                <h2 class="header-title">Auximad</h2>
                <span v-if="userInfo && userInfo.agence" class="agence-badge">
                    {{ userInfo.agence }} ({{ userInfo.codeAgence }})
                </span>
            </div>

            <!-- User context & Action -->
            <div class="header-user" v-if="isLoggedIn">
                <div class="user-details">
                    <span class="user-name">{{ userInfo?.nomComplet }}</span>
                    <span class="user-role">{{ userInfo?.role }}</span>
                </div>

                <button @click="handleLogout" class="logout-btn" title="Se déconnecter">
                    <svg xmlns="http://www.w3.org/2001/svg" class="logout-icon" fill="none" viewBox="0 0 24 24"
                        stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    <span>Déconnexion</span>
                </button>
            </div>
        </div>
    </header>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import authService from '../../utils/auth.service'

const router = useRouter()
const userInfo = ref(null)

const isLoggedIn = computed(() => {
    return authService.isLoggedIn()
})

const fetchUserInfo = () => {
    userInfo.value = authService.getUserInfo()
}

onMounted(() => {
    fetchUserInfo()
})

const handleLogout = () => {
    authService.logout()
    router.push('/login')
}
</script>

<style scoped>
.main-header {
    position: sticky;
    top: 0;
    z-index: 100;
    width: 100%;
    padding: 1rem 2rem;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(15, 23, 42, 0.75);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
}

.header-container {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-width: 1280px;
    margin: 0 auto;
}

.header-brand {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.header-title {
    font-size: 1.5rem;
    font-weight: 700;
    background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin: 0;
    letter-spacing: -0.025em;
}

.agence-badge {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-primary-light);
    background: rgba(59, 130, 246, 0.15);
    border: 1px solid rgba(59, 130, 246, 0.3);
    padding: 0.25rem 0.6rem;
    border-radius: 9999px;
}

.header-user {
    display: flex;
    align-items: center;
    gap: 1.5rem;
}

.user-details {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.user-name {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-text);
}

.user-role {
    font-size: 0.75rem;
    font-weight: 500;
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.logout-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: rgba(239, 68, 68, 0.1);
    border: 1px solid rgba(239, 68, 68, 0.2);
    color: #fca5a5;
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    font-family: var(--font-family);
}

.logout-btn:hover {
    background: rgba(239, 68, 68, 0.25);
    border-color: rgba(239, 68, 68, 0.4);
    color: #fff;
    transform: translateY(-1px);
}

.logout-btn:active {
    transform: translateY(0);
}

.logout-icon {
    width: 1.2rem;
    height: 1.2rem;
}
</style>

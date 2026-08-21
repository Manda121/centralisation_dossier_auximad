<template>
    <div class="home-container">
        <!-- Header component -->
        <Header />

        <!-- Main content area -->
        <main class="main-content animate-fade-in">
            <div class="dashboard-card glass-panel">
                <h1 class="welcome-title">Bienvenue sur Auximad</h1>
                <p class="welcome-text">
                    Vous êtes connecté au système de centralisation des dossiers. Utilisez la navigation pour gérer vos
                    activités.
                </p>
            </div>
        </main>
    </div>
</template>

<script setup>
import Header from '../components/inc/Header.vue'
</script>

<style scoped>
.home-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.main-content {
    flex: 1;
    padding: 3rem 2rem;
    max-width: 1280px;
    width: 100%;
    margin: 0 auto;
}

.dashboard-card {
    padding: 3rem;
    border-radius: 1.5rem;
    text-align: center;
}

.welcome-title {
    font-size: 2.25rem;
    font-weight: 700;
    margin-bottom: 1rem;
    background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
}

.welcome-text {
    color: var(--color-text-muted);
    font-size: 1.1rem;
    line-height: 1.6;
    max-width: 600px;
    margin: 0 auto;
}
</style>

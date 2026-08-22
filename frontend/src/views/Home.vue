<template>
    <div class="d-flex min-vh-100 bg-light">
        <!-- Header / Sidebar Component -->
        <Header />

        <!-- Main Content Area -->
        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="card border-0 shadow-sm p-4 p-md-5 bg-white rounded-4 text-center">
                    <h1 class="h2 fw-bold text-primary mb-3">Bienvenue sur Auximad</h1>
                    <p class="text-secondary lead max-w-600 mx-auto mb-0">
                        Vous êtes connecté au système de centralisation des dossiers. Utilisez la navigation pour gérer
                        vos activités.
                    </p>
                </div>
            </div>
        </main>
    </div>
</template>

<script setup>
import Header from '../components/inc/Header.vue'
</script>

<style scoped>
.max-w-600 {
    max-width: 600px;
}
</style>
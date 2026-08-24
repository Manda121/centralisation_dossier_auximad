<script setup>
import Header from '../components/inc/Header.vue'
</script>

<template>
    <div class="d-flex min-vh-100 bg-light">
        <Header />

        <main class="flex-grow-1 p-4" style="margin-left: 280px;">
            <div class="container-fluid">
                <div class="card border shadow-sm">
                    <div class="card-body p-5 text-center">
                        <h1 class="h4 fw-bold text-dark mb-3">Bienvenue sur Auximad</h1>
                        <p class="text-secondary mb-0">
                            Vous êtes connecté au système de centralisation des dossiers. Utilisez la navigation pour gérer vos activités.
                        </p>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<style scoped>
</style>
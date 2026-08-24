<script setup>
const props = defineProps({ message: String })
</script>

<template>
    <div class="alert alert-secondary">
        <h2 class="h5">CHARGEMENT</h2>
        <p class="mb-0">{{ props.message }}</p>
    </div>
</template>
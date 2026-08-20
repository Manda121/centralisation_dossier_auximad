<script setup>
    const props = defineProps({ message: String });
</script>

<template>
    <div class="chargement">
        <h2>LOADNING</h2>
        <p>{{ props.message }}</p>
    </div>
</template>
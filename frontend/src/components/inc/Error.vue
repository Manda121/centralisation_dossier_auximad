<script setup>
const props = defineProps({ error: String })
</script>

<template>
    <div class="alert alert-danger">
        <h2 class="h5">ERROR</h2>
        <p class="mb-0">{{ props.error }}</p>
    </div>
</template>
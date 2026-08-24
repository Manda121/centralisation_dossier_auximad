<script setup>
const props = defineProps({ warning: String })
</script>

<template>
    <div class="alert alert-warning">
        <h2 class="h5">WARNING</h2>
        <p class="mb-0">{{ props.warning }}</p>
    </div>
</template>
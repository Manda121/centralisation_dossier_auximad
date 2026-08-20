<script setup>
const props = defineProps({error: String});
</script>

<template>
    <div class="error">
        <h2>ERROR</h2>
        <p>{{ props.error }}</p>
    </div>
</template>
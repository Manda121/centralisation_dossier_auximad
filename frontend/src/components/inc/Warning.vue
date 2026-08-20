<script setup>
const props = defineProps({warning: String});
</script>

<template>
    <div class="warning">
        <h2>WARNING</h2>
        <p>{{ props.warning }}</p>
    </div>
</template>
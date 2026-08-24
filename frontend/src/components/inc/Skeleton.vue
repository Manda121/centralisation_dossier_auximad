<script setup>
defineProps({
    width: { type: String, default: '100%' },
    height: { type: String, default: '14px' },
    radius: { type: String, default: '4px' },
    className: { type: String, default: '' }
})
</script>

<template>
    <span
        class="sk-bone"
        :class="className"
        :style="{ width, height, borderRadius: radius }"
        aria-hidden="true"
    ></span>
</template>

<style scoped>
.sk-bone {
    display: inline-block;
    max-width: 100%;
    vertical-align: middle;
    background: linear-gradient(90deg, #e6ebf1 0%, #f4f6f9 45%, #e6ebf1 90%);
    background-size: 200% 100%;
    animation: sk-shimmer 1.15s ease-in-out infinite;
}

@keyframes sk-shimmer {
    0% { background-position: 100% 0; }
    100% { background-position: -100% 0; }
}
</style>

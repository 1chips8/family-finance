<script setup lang="ts">
withDefaults(defineProps<{ title: string; description?: string; actionText?: string; loading?: boolean }>(), { description: '', actionText: '' })
const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div v-if="loading" class="state-block loading-state" aria-live="polite" role="status" :aria-label="title">
    <span class="skeleton-line wide" /><span class="skeleton-line" /><span class="skeleton-line short" />
  </div>
  <div v-else class="state-block">
    <div class="state-dot">·</div><h3>{{ title }}</h3><p>{{ description }}</p>
    <button v-if="actionText" class="secondary-button" @click="emit('action')">{{ actionText }}</button>
  </div>
</template>

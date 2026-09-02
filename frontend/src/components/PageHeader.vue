<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

defineProps<{ title: string; subtitle: string }>()
const auth = useAuthStore()
const householdName = computed(() => auth.household?.name || auth.user?.household?.name || '家庭理财')
</script>

<template>
  <div class="page-header"><div><p class="section-kicker">{{ householdName }}</p><h1>{{ title }}</h1><p class="page-subtitle">{{ subtitle }}</p></div><div class="page-actions"><slot /></div></div>
</template>

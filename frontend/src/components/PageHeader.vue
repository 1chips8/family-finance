<script setup lang="ts">
defineProps<{ title: string; subtitle: string }>()
</script>
<template>
  <div class="page-header"><div><p class="section-kicker">家庭理财</p><h1>{{ title }}</h1><p class="page-subtitle">{{ subtitle }}</p></div><div class="page-actions"><slot /></div></div>
</template>

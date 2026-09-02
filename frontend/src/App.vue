<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { FORBIDDEN_EVENT } from './api/client'

const router = useRouter()
function handleForbidden() {
  if (router.currentRoute.value.path !== '/forbidden') void router.push('/forbidden')
}
onMounted(() => window.addEventListener(FORBIDDEN_EVENT, handleForbidden))
onUnmounted(() => window.removeEventListener(FORBIDDEN_EVENT, handleForbidden))
</script>

<template>
  <RouterView />
</template>

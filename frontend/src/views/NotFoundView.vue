<script setup lang="ts">
import { FileQuestion } from 'lucide-vue-next'
</script>

<template>
  <main class="placeholder-page">
    <FileQuestion :size="38" class="not-found-icon" />
    <div class="eyebrow">404</div>
    <h1>页面不存在</h1>
    <p>请检查地址，或返回登录页继续操作。</p>
    <RouterLink class="back-link" to="/login">返回登录</RouterLink>
  </main>
</template>

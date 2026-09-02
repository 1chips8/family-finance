<script setup lang="ts">
import { ArrowLeft, LayoutDashboard, ShieldX } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="placeholder-page">
    <ShieldX :size="40" class="not-found-icon" />
    <div class="eyebrow">403</div>
    <h1>无权访问此功能</h1>
    <p>当前账号没有执行这项操作的权限。返回仪表盘，继续查看家庭数据。</p>
    <div class="status-actions"><button type="button" class="primary-button" data-test="back-dashboard" @click="router.push('/dashboard')"><LayoutDashboard :size="16" />返回仪表盘</button><button type="button" class="secondary-button" data-test="back-previous" @click="router.back()"><ArrowLeft :size="16" />返回上一页</button></div>
  </main>
</template>

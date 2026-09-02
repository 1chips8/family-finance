<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { WalletCards, LayoutDashboard, ReceiptText, UsersRound, Tags, UserRound, Plus } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.user?.displayName || '家庭成员')
const navItems = [
  { path: '/dashboard', label: '概览', icon: LayoutDashboard },
  { path: '/entries', label: '收支明细', icon: ReceiptText },
  { path: '/members', label: '家庭成员', icon: UsersRound, parentOnly: false },
  { path: '/categories', label: '收支分类', icon: Tags, parentOnly: false },
]
async function logout() { await auth.logout(); await router.push('/login') }
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <RouterLink class="app-brand" to="/dashboard"><span class="brand-mark">家</span><span>家账</span></RouterLink>
      <nav class="pill-nav" aria-label="主导航">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" :class="{ active: route.path === item.path }">
          <component :is="item.icon" :size="16" :stroke-width="2.2" />{{ item.label }}
        </RouterLink>
      </nav>
      <div class="header-actions">
        <RouterLink class="primary-button compact" to="/entries?new=1"><Plus :size="17" />记一笔</RouterLink>
        <el-dropdown trigger="click">
          <button class="avatar-button" :aria-label="`${displayName}的账户菜单`"><span>{{ displayName.slice(0, 1) }}</span><span class="avatar-name">{{ displayName }}</span></button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')"><UserRound :size="15" />个人资料</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <main class="app-content"><RouterView /></main>
  </div>
</template>

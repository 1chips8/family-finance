<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowUpRight, ArrowDownRight, Wallet, RefreshCw, Plus, TrendingUp } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import TrendChart from '../components/TrendChart.vue'
import AmountText from '../components/AmountText.vue'
import StateBlock from '../components/StateBlock.vue'
import { statisticsApi } from '../api/statistics'
import { errorMessage, errorStatus } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { Dashboard } from '../types/domain'

const router = useRouter(), auth = useAuthStore(), dashboard = ref<Dashboard | null>(null), loading = ref(true), error = ref('')
const format = (value: string | number) => Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const expenseComposition = computed(() => dashboard.value?.composition.filter((item) => item.type === 'EXPENSE') || [])
const incomeComposition = computed(() => dashboard.value?.composition.filter((item) => item.type === 'INCOME') || [])
async function load() {
  loading.value = true; error.value = ''
  try { dashboard.value = await statisticsApi.dashboard() } catch (e) { error.value = errorMessage(e); if (errorStatus(e) === 401) { await auth.logout(); await router.push('/login') } }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <PageHeader title="今天，也看清一点" :subtitle="`${auth.user?.displayName || '你好'}，这是你的家庭财务概览。`"><button class="primary-button" @click="router.push('/entries?new=1')"><Plus :size="17" />记一笔</button></PageHeader>
    <StateBlock v-if="loading" title="正在整理家庭账目" description="很快就好。" :loading="true" />
    <StateBlock v-else-if="error" title="概览暂时无法加载" :description="error" action-text="重新加载" @action="load" />
    <template v-else-if="dashboard">
      <section class="kpi-grid">
        <article class="kpi-card income-card"><div class="kpi-label"><span class="kpi-icon"><ArrowUpRight :size="18" /></span>本期收入</div><strong>¥{{ format(dashboard.totals.income) }}</strong><span class="kpi-foot">所选时间范围</span></article>
        <article class="kpi-card expense-card"><div class="kpi-label"><span class="kpi-icon"><ArrowDownRight :size="18" /></span>本期支出</div><strong>¥{{ format(dashboard.totals.expense) }}</strong><span class="kpi-foot">所选时间范围</span></article>
        <article class="kpi-card balance-card"><div class="kpi-label"><span class="kpi-icon"><Wallet :size="18" /></span>本期结余</div><strong>¥{{ format(dashboard.totals.balance) }}</strong><span class="kpi-foot">收入减去支出</span></article>
      </section>
      <section class="dashboard-grid">
        <article class="surface trend-panel"><div class="surface-heading"><div><h2>近 12 个月</h2><p>收入和支出的节奏</p></div><TrendingUp :size="20" class="heading-icon" /></div><TrendChart v-if="dashboard.trend.length" :points="dashboard.trend" /><div v-else class="chart-empty">还没有足够的记录形成趋势。</div></article>
        <article class="surface composition-panel"><div class="surface-heading"><div><h2>支出构成</h2><p>钱主要去了哪里</p></div></div><div v-if="expenseComposition.length" class="composition-list"><div v-for="item in expenseComposition.slice(0, 5)" :key="item.categoryId" class="composition-row"><span class="composition-name">{{ item.categoryName }}</span><strong>¥{{ format(item.amount) }}</strong><div class="composition-bar"><i :style="{ width: `${Math.min(100, Number(item.amount) / Math.max(1, Number(dashboard.totals.expense)) * 100)}%` }" /></div></div></div><div v-else class="chart-empty">当前范围还没有支出记录。</div></article>
        <article class="surface member-panel"><div class="surface-heading"><div><h2>家庭成员</h2><p>每个人的收支对比</p></div></div><div v-if="dashboard.members.length" class="member-list"><div v-for="member in dashboard.members" :key="member.memberId" class="member-row"><div class="member-avatar">{{ member.memberName.slice(0, 1) }}</div><div class="member-info"><strong>{{ member.memberName }}</strong><span>{{ member.memberNo }}</span></div><AmountText :amount="member.balance" type="BALANCE" /></div></div><div v-else class="chart-empty">加入家庭后，这里会显示成员对比。</div></article>
        <article class="surface recent-panel"><div class="surface-heading"><div><h2>最近流水</h2><p>{{ auth.isParent ? '家庭最近的几笔记录' : '你最近的几笔记录' }}</p></div><button class="text-button" @click="router.push('/entries')">查看全部 <ArrowUpRight :size="15" /></button></div><div v-if="dashboard.recentEntries.length" class="recent-list"><div v-for="entry in dashboard.recentEntries" :key="entry.id" class="recent-row"><div class="recent-mark" :class="entry.type.toLowerCase()">{{ entry.type === 'INCOME' ? '+' : '−' }}</div><div class="recent-info"><strong>{{ entry.categoryName }}</strong><span>{{ entry.memberName }} · {{ entry.occurredOn }}</span></div><AmountText :amount="entry.amount" :type="entry.type" /></div></div><div v-else class="empty-inline"><Wallet :size="28" /><span>还没有流水，记下第一笔吧。</span><button class="secondary-button" @click="router.push('/entries?new=1')">记录第一笔</button></div></article>
      </section>
    </template>
  </div>
</template>

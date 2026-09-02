<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowUpRight, ArrowDownRight, Wallet, Plus, TrendingUp } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import TrendChart from '../components/TrendChart.vue'
import AmountText from '../components/AmountText.vue'
import StateBlock from '../components/StateBlock.vue'
import { statisticsApi } from '../api/statistics'
import { errorMessage, errorStatus } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { CategoryType, Dashboard, Money } from '../types/domain'

type RangeKey = 'month' | 'threeMonths' | 'year' | 'custom'
type DateRange = { from: string; to: string }

const router = useRouter()
const auth = useAuthStore()
const dashboard = ref<Dashboard | null>(null)
const loading = ref(true)
const error = ref('')
const rangeKey = ref<RangeKey>('month')
const customFrom = ref('')
const customTo = ref('')
const compositionType = ref<CategoryType>('EXPENSE')
const rangeOptions: { key: RangeKey; label: string }[] = [
  { key: 'month', label: '本月' },
  { key: 'threeMonths', label: '近 3 个月' },
  { key: 'year', label: '今年' },
  { key: 'custom', label: '自定义' },
]
const rangeLabels: Record<RangeKey, string> = { month: '本月', threeMonths: '近 3 个月', year: '今年', custom: '自定义范围' }

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
function getPresetRange(key: Exclude<RangeKey, 'custom'>): DateRange {
  const now = new Date()
  const from = key === 'month'
    ? new Date(now.getFullYear(), now.getMonth(), 1)
    : key === 'threeMonths'
      ? new Date(now.getFullYear(), now.getMonth() - 2, 1)
      : new Date(now.getFullYear(), 0, 1)
  return { from: formatDate(from), to: formatDate(now) }
}
const displayedRange = computed<DateRange | null>(() => rangeKey.value === 'custom'
  ? customFrom.value && customTo.value ? { from: customFrom.value, to: customTo.value } : null
  : getPresetRange(rangeKey.value))
const rangeLabel = computed(() => displayedRange.value
  ? `${rangeLabels[rangeKey.value]} · ${displayedRange.value.from} 至 ${displayedRange.value.to}`
  : rangeLabels.custom)
const todayLabel = computed(() => formatDate(new Date()))

const format = (value: Money) => {
  const number = Number(value)
  return (Number.isFinite(number) ? number : 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
const expenseComposition = computed(() => dashboard.value?.composition.filter((item) => item.type === 'EXPENSE') || [])
const incomeComposition = computed(() => dashboard.value?.composition.filter((item) => item.type === 'INCOME') || [])
const activeComposition = computed(() => compositionType.value === 'INCOME' ? incomeComposition.value : expenseComposition.value)
const activeCompositionTotal = computed(() => {
  const total = compositionType.value === 'INCOME' ? dashboard.value?.totals.income : dashboard.value?.totals.expense
  const number = Number(total)
  return Number.isFinite(number) ? number : 0
})
function compositionPercent(amount: Money) {
  const number = Number(amount)
  return activeCompositionTotal.value > 0 && Number.isFinite(number) ? number / activeCompositionTotal.value * 100 : 0
}
function compositionPercentLabel(amount: Money) { return `${compositionPercent(amount).toFixed(1)}%` }

async function load(dateRange: DateRange) {
  loading.value = true
  error.value = ''
  try {
    dashboard.value = await statisticsApi.dashboard(dateRange)
  } catch (e) {
    error.value = errorMessage(e)
    if (errorStatus(e) === 401) {
      await auth.logout()
      await router.push('/login')
    }
  } finally {
    loading.value = false
  }
}
async function selectRange(key: RangeKey) {
  rangeKey.value = key
  if (key !== 'custom') await load(getPresetRange(key))
}
async function applyCustomRange() {
  if (!customFrom.value || !customTo.value) {
    ElMessage.warning('请选择开始和结束日期')
    return
  }
  if (customFrom.value > customTo.value) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  await load({ from: customFrom.value, to: customTo.value })
}
onMounted(() => load(getPresetRange('month')))
</script>

<template>
  <div class="page-wrap dashboard-page">
    <PageHeader title="今天，也看清一点" :subtitle="`${auth.user?.displayName || '你好'}，这是你的家庭财务概览。`"><button class="primary-button" @click="router.push('/entries?new=1')"><Plus :size="17" />记一笔</button></PageHeader>
    <section class="dashboard-toolbar surface" aria-label="统计时间范围">
      <div class="dashboard-range-copy"><span>统计范围</span><strong>{{ rangeLabel }}</strong></div>
      <div class="range-controls" role="group" aria-label="选择统计时间范围">
        <button v-for="option in rangeOptions" :key="option.key" type="button" :data-range="option.key" :aria-pressed="rangeKey === option.key" :class="{ active: rangeKey === option.key }" @click="selectRange(option.key)">{{ option.label }}</button>
      </div>
      <div v-if="rangeKey === 'custom'" class="custom-range" data-test="custom-range">
        <label>开始日期<input v-model="customFrom" data-test="custom-from" type="date" :max="todayLabel" /></label>
        <span aria-hidden="true">至</span>
        <label>结束日期<input v-model="customTo" data-test="custom-to" type="date" :max="todayLabel" /></label>
        <button type="button" class="secondary-button" data-test="apply-custom" @click="applyCustomRange">应用范围</button>
      </div>
    </section>
    <StateBlock v-if="loading" title="正在整理家庭账目" description="很快就好。" :loading="true" />
    <StateBlock v-else-if="error" title="概览暂时无法加载" :description="error" action-text="重新加载" @action="load(displayedRange || getPresetRange('month'))" />
    <template v-else-if="dashboard">
      <section class="kpi-grid">
        <article class="kpi-card income-card"><div class="kpi-label"><span class="kpi-icon"><ArrowUpRight :size="18" /></span>本期收入</div><strong>¥{{ format(dashboard.totals.income) }}</strong><span class="kpi-foot">{{ rangeLabel }}</span></article>
        <article class="kpi-card expense-card"><div class="kpi-label"><span class="kpi-icon"><ArrowDownRight :size="18" /></span>本期支出</div><strong>¥{{ format(dashboard.totals.expense) }}</strong><span class="kpi-foot">{{ rangeLabel }}</span></article>
        <article class="kpi-card balance-card"><div class="kpi-label"><span class="kpi-icon"><Wallet :size="18" /></span>本期结余</div><strong>¥{{ format(dashboard.totals.balance) }}</strong><span class="kpi-foot">收入减去支出 · {{ rangeLabel }}</span></article>
      </section>
      <section class="dashboard-grid">
        <article class="surface trend-panel"><div class="surface-heading"><div><h2>近 12 个月</h2><p>收入和支出的节奏</p></div><TrendingUp :size="20" class="heading-icon" /></div><TrendChart v-if="dashboard.trend.length" :points="dashboard.trend" /><div v-else class="chart-empty">还没有足够的记录形成趋势。</div></article>
        <article class="surface composition-panel"><div class="surface-heading composition-heading"><div><h2>{{ compositionType === 'EXPENSE' ? '支出构成' : '收入构成' }}</h2><p>{{ compositionType === 'EXPENSE' ? '钱主要去了哪里' : '家庭收入来自哪里' }}</p></div><div class="type-toggle composition-switch" role="tablist" aria-label="收支构成类型"><button type="button" data-composition="expense" :class="{ active: compositionType === 'EXPENSE' }" :aria-selected="compositionType === 'EXPENSE'" @click="compositionType = 'EXPENSE'">支出</button><button type="button" data-composition="income" :class="{ active: compositionType === 'INCOME' }" :aria-selected="compositionType === 'INCOME'" @click="compositionType = 'INCOME'">收入</button></div></div><div v-if="activeComposition.length" class="composition-list"><div v-for="item in activeComposition.slice(0, 5)" :key="item.categoryId" class="composition-row"><span class="composition-name">{{ item.categoryName }}</span><strong>¥{{ format(item.amount) }}</strong><span class="composition-percent">{{ compositionPercentLabel(item.amount) }}</span><div class="composition-bar"><i :class="compositionType.toLowerCase()" :style="{ width: `${Math.min(100, compositionPercent(item.amount))}%` }" /></div></div></div><div v-else class="chart-empty">当前范围还没有{{ compositionType === 'EXPENSE' ? '支出' : '收入' }}记录。</div></article>
        <article class="surface member-panel"><div class="surface-heading"><div><h2>家庭成员</h2><p>每个人的收支对比</p></div></div><div v-if="dashboard.members.length" class="member-list"><div v-for="member in dashboard.members" :key="member.memberId" class="member-row"><div class="member-avatar">{{ member.memberName.slice(0, 1) }}</div><div class="member-info"><strong>{{ member.memberName }}</strong><span>{{ member.memberNo }}</span></div><div class="member-metrics"><div><span>收入</span><AmountText :amount="member.income" type="INCOME" /></div><div><span>支出</span><AmountText :amount="member.expense" type="EXPENSE" /></div><div><span>结余</span><AmountText :amount="member.balance" type="BALANCE" /></div></div></div></div><div v-else class="chart-empty">加入家庭后，这里会显示成员对比。</div></article>
        <article class="surface recent-panel"><div class="surface-heading"><div><h2>最近流水</h2><p>{{ auth.isParent ? '家庭最近的几笔记录' : '你最近的几笔记录' }}</p></div><button class="text-button" @click="router.push('/entries')">查看全部 <ArrowUpRight :size="15" /></button></div><div v-if="dashboard.recentEntries.length" class="recent-list"><div v-for="entry in dashboard.recentEntries" :key="entry.id" class="recent-row"><div class="recent-mark" :class="entry.type.toLowerCase()">{{ entry.type === 'INCOME' ? '+' : '−' }}</div><div class="recent-info"><strong>{{ entry.categoryName }}</strong><span>{{ entry.memberName }} · {{ entry.occurredOn }}</span></div><AmountText :amount="entry.amount" :type="entry.type" /></div></div><div v-else class="empty-inline"><Wallet :size="28" /><span>还没有流水，记下第一笔吧。</span><button class="secondary-button" @click="router.push('/entries?new=1')">记录第一笔</button></div></article>
      </section>
    </template>
  </div>
</template>

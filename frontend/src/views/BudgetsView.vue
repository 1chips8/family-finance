<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WalletCards, Save, Trash2 } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import BudgetProgress from '../components/BudgetProgress.vue'
import { budgetApi } from '../api/budgets'
import { categoryApi } from '../api/categories'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { localMonthString } from '../utils/date'
import type { BudgetItem, BudgetOverview, Category } from '../types/domain'

const auth = useAuthStore()
const month = ref(localMonthString())
const overview = ref<BudgetOverview | null>(null)
const categories = ref<Category[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const totalAmount = ref('')
const categoryAmounts = reactive<Record<number, string>>({})
const expenseCategories = computed(() => categories.value.filter((item) => item.type === 'EXPENSE' && item.status === 'ACTIVE'))
const categoryBudgetRows = computed(() => expenseCategories.value.map((category) => ({
  category,
  item: overview.value?.categories.find((item) => item.categoryId === category.id) ?? null,
})))

function itemFrom(raw: any, categoryName?: string): BudgetItem {
  const budget = raw?.budget ?? raw?.amount ?? raw?.budgetAmount ?? 0
  const spent = raw?.spent ?? raw?.spentAmount ?? 0
  const remaining = raw?.remaining ?? Number(budget) - Number(spent)
  const usageRate = Number(raw?.usageRate ?? raw?.usage ?? (Number(budget) > 0 ? Number(spent) / Number(budget) : 0))
  const status = raw?.status ?? (usageRate > 1 ? 'OVER' : usageRate >= 0.8 ? 'WARNING' : 'NORMAL')
  return { ...raw, categoryName: raw?.categoryName ?? categoryName ?? null, budget, spent, remaining, usageRate, status }
}
function normalize(raw: any): BudgetOverview {
  const totalRaw = raw?.total ?? (raw?.totalBudget != null ? { budget: raw.totalBudget, spent: raw.totalSpent, remaining: raw.totalRemaining, usageRate: raw.usageRate, status: raw.totalStatus } : null)
  const list = raw?.categories ?? raw?.categoryBudgets ?? []
  return { month: raw?.month ?? month.value, total: totalRaw ? itemFrom(totalRaw, '家庭总预算') : null, categories: list.map((item: any) => itemFrom(item)) }
}
async function load() {
  loading.value = true; error.value = ''
  try {
    const [result, list] = await Promise.all([budgetApi.overview(month.value), categoryApi.list(false)])
    overview.value = normalize(result); categories.value = list
    totalAmount.value = overview.value.total ? String(overview.value.total.budget) : ''
    Object.keys(categoryAmounts).forEach((key) => delete categoryAmounts[Number(key)])
    overview.value.categories.forEach((item) => { if (item.categoryId) categoryAmounts[item.categoryId] = String(item.budget) })
  } catch (e) { error.value = errorMessage(e, '预算暂时无法加载') }
  finally { loading.value = false }
}
async function changeMonth() { await load() }
async function saveTotal() {
  if (!/^\d+(\.\d{1,2})?$/.test(totalAmount.value) || Number(totalAmount.value) <= 0) { ElMessage.warning('请输入大于 0 且最多两位小数的预算'); return }
  saving.value = true
  try { overview.value = normalize(await budgetApi.upsertTotal(month.value, totalAmount.value)); ElMessage.success('总预算已保存') }
  catch (e) { ElMessage.error(errorMessage(e, '总预算保存失败')) }
  finally { saving.value = false }
}
async function saveCategory(categoryId: number) {
  const amount = categoryAmounts[categoryId] || ''
  if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) { ElMessage.warning('请输入有效的分类预算'); return }
  saving.value = true
  try { overview.value = normalize(await budgetApi.upsertCategory(categoryId, month.value, amount)); ElMessage.success('分类预算已保存') }
  catch (e) { ElMessage.error(errorMessage(e, '分类预算保存失败')) }
  finally { saving.value = false }
}
async function removeCategory(categoryId: number) {
  try {
    await ElMessageBox.confirm('删除后该分类本月不再参与预算提醒，历史流水不会受影响。', '删除分类预算？', { confirmButtonText: '确认删除', cancelButtonText: '保留预算', type: 'warning' })
    await budgetApi.removeCategory(categoryId, month.value); delete categoryAmounts[categoryId]; await load(); ElMessage.success('分类预算已删除')
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(errorMessage(e, '分类预算删除失败')) }
}
onMounted(load)
</script>

<template>
  <div class="page-wrap budgets-page">
    <PageHeader title="家庭预算" subtitle="给这个月的选择留出清楚的边界。"><label class="month-picker">报告月份<input v-model="month" type="month" aria-label="选择预算月份" @change="changeMonth" /></label></PageHeader>
    <StateBlock v-if="loading" title="正在整理预算" :loading="true" />
    <StateBlock v-else-if="error" title="预算暂时无法加载" :description="error" action-text="重试" @action="load" />
    <template v-else>
      <section class="surface budget-overview"><div class="surface-heading"><div><span class="section-kicker">{{ month }}</span><h2>本月预算执行</h2><p>{{ auth.isParent ? '你可以设置家庭总额和分类额度。' : '你可以查看家庭预算的使用情况。' }}</p></div><WalletCards :size="22" class="heading-icon" /></div><div v-if="overview?.total" class="budget-total-layout"><BudgetProgress :item="overview.total" label="家庭总预算" /><form v-if="auth.isParent" class="budget-edit-form" @submit.prevent="saveTotal"><label for="total-budget">总预算金额</label><input id="total-budget" v-model="totalAmount" inputmode="decimal" aria-describedby="total-budget-help" /><small id="total-budget-help">按自然月单独设置，不会自动继承。</small><button class="primary-button" :disabled="saving"><Save :size="15" />保存总预算</button></form></div><div v-else class="budget-empty"><div class="empty-inline"><WalletCards :size="25" /><span>本月还没有家庭总预算。</span></div><form v-if="auth.isParent" class="budget-edit-form" @submit.prevent="saveTotal"><label for="total-budget">总预算金额</label><input id="total-budget" v-model="totalAmount" inputmode="decimal" aria-describedby="total-budget-help" placeholder="例如 8000.00" /><small id="total-budget-help">按自然月单独设置，不会自动继承。</small><button class="primary-button" :disabled="saving"><Save :size="15" />设置总预算</button></form></div></section>
      <section class="surface category-budget-section"><div class="surface-heading"><div><h2>分类预算</h2><p>为已有支出分类设置额度，接近上限和超支状态会自动标记。</p></div></div><div v-if="categoryBudgetRows.length" class="budget-list"><article v-for="row in categoryBudgetRows" :key="row.category.id" class="budget-list-item"><BudgetProgress v-if="row.item" :item="row.item" /><div v-else class="budget-unconfigured"><strong>{{ row.category.name }}</strong><span>{{ auth.isParent ? '尚未设置额度' : '家长尚未设置额度' }}</span></div><div v-if="auth.isParent" class="budget-category-edit"><input v-model="categoryAmounts[row.category.id]" :aria-label="`${row.category.name}预算金额`" inputmode="decimal" placeholder="0.00" /><button class="icon-button" :aria-label="`保存${row.category.name}预算`" :disabled="saving" @click="saveCategory(row.category.id)"><Save :size="16" /></button><button v-if="row.item" class="icon-button danger" :aria-label="`删除${row.category.name}预算`" @click="removeCategory(row.category.id)"><Trash2 :size="16" /></button></div></article></div><p v-else class="empty-inline">还没有可用的支出分类，请先创建支出分类。</p><div v-if="auth.isParent && expenseCategories.length" class="budget-add-note">每个自然月单独设置；留空的分类不会计入预算提醒。</div></section>
    </template>
  </div>
</template>

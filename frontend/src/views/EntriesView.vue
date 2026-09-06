<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Filter, Plus, RefreshCw, Download, Upload, Trash2, Pencil, ReceiptText } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import EntryForm from '../components/EntryForm.vue'
import AmountText from '../components/AmountText.vue'
import { entryApi, type EntryPayload } from '../api/entries'
import { categoryApi } from '../api/categories'
import { memberApi } from '../api/members'
import { errorMessage, errorStatus } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { Category, Entry, Member } from '../types/domain'
import EntryImportDialog from '../components/EntryImportDialog.vue'

const route = useRoute(), router = useRouter(), auth = useAuthStore()
const entries = ref<Entry[]>([]), members = ref<Member[]>([]), categories = ref<Category[]>([]), total = ref(0), loading = ref(true), error = ref(''), drawer = ref(false), importOpen = ref(false), saving = ref(false), editing = ref<Entry | null>(null)
const filters = reactive({ type: '', categoryId: '', memberId: '', from: '', to: '' }), page = ref(1), pageSize = 20
const usableCategories = computed(() => categories.value.filter((category) => category.status === 'ACTIVE'))
const filteredCategoryOptions = computed(() => filters.type ? usableCategories.value.filter((category) => category.type === filters.type) : usableCategories.value)
async function load() {
  loading.value = true; error.value = ''
  try {
    const result = await entryApi.list({ ...filters, page: page.value, pageSize }); entries.value = result.items; total.value = result.total
  } catch (e) { error.value = errorMessage(e); if (errorStatus(e) === 401) { await auth.logout(); await router.push('/login') } }
  finally { loading.value = false }
}
async function loadOptions() { try { [members.value, categories.value] = await Promise.all([memberApi.list(), categoryApi.list(false)]) } catch (e) { error.value = errorMessage(e) } }
function resetFilters() { Object.assign(filters, { type: '', categoryId: '', memberId: '', from: '', to: '' }); page.value = 1; load() }
function openCreate() { editing.value = null; drawer.value = true }
function openEdit(entry: Entry) { editing.value = entry; drawer.value = true }
function clearNewQuery() {
  if (route.query.new === '1') {
    const query = { ...route.query }
    delete query.new
    void router.replace({ query })
  }
}
function closeDrawer() { drawer.value = false; clearNewQuery() }
async function save(payload: EntryPayload) {
  saving.value = true
  try { if (editing.value) await entryApi.update(editing.value.id, payload); else await entryApi.create(payload); ElMessage.success(editing.value ? '流水已更新' : '流水已记录'); drawer.value = false; await load() }
  catch (e) { ElMessage.error(errorMessage(e, '保存失败，请检查填写内容')) }
  finally { saving.value = false }
}
async function remove(entry: Entry) {
  try { await ElMessageBox.confirm(`删除“${entry.categoryName} · ¥${entry.amount}”后，它将不再参与列表和统计。`, '删除这笔流水？', { confirmButtonText: '确认删除', cancelButtonText: '保留记录', type: 'warning' }); await entryApi.remove(entry.id); ElMessage.success('流水已删除'); await load() } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(errorMessage(e, '删除失败')) }
}
async function exportCsv() {
  try {
    const anchor = document.createElement('a')
    anchor.href = entryApi.exportUrl({ ...filters })
    anchor.download = '家庭流水.csv'
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    ElMessage.success('流水已导出')
  } catch (e) { ElMessage.error(errorMessage(e, '导出失败')) }
}
onMounted(async () => { await Promise.all([loadOptions(), load()]) })
watch(() => route.query.new, (value) => { if (value === '1') openCreate() }, { immediate: true })
watch(drawer, (open) => { if (!open) clearNewQuery() })
</script>

<template>
  <div class="page-wrap entries-page">
    <PageHeader title="收支明细" subtitle="每一笔记录，都让家庭的选择更清楚。" />
    <div class="page-tools" aria-label="流水工具"><button class="secondary-button" type="button" @click="importOpen = true"><Upload :size="16" />导入 CSV</button><button class="secondary-button" type="button" @click="exportCsv"><Download :size="16" />导出当前结果</button><RouterLink class="text-button" to="/recurring">周期流水 <span aria-hidden="true">→</span></RouterLink></div>
    <section class="filter-bar surface"><div class="filter-title"><Filter :size="17" />筛选</div><el-select v-model="filters.type" clearable placeholder="全部类型" @change="filters.categoryId = ''; page = 1; load()"><el-option label="收入" value="INCOME" /><el-option label="支出" value="EXPENSE" /></el-select><el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="page = 1; load()"><el-option v-for="category in filteredCategoryOptions" :key="category.id" :label="category.name" :value="category.id" /></el-select><el-select v-if="auth.isParent" v-model="filters.memberId" clearable placeholder="全部成员" @change="page = 1; load()"><el-option v-for="member in members" :key="member.id" :label="member.displayName" :value="member.id" /></el-select><el-date-picker :model-value="filters.from && filters.to ? [filters.from, filters.to] : undefined" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @update:model-value="(value: [string, string] | undefined) => { filters.from = value?.[0] || ''; filters.to = value?.[1] || ''; page = 1; load() }" /><button class="icon-button" aria-label="重置筛选" title="重置筛选" @click="resetFilters"><RefreshCw :size="17" /></button></section>
    <StateBlock v-if="loading" title="正在加载流水" :loading="true" />
    <StateBlock v-else-if="error" title="流水暂时无法加载" :description="error" action-text="重试" @action="load" />
    <section v-else class="surface table-surface">
      <div v-if="!entries.length" class="empty-state"><ReceiptText :size="34" /><h3>当前还没有流水</h3><p>记录第一笔收入或支出，家庭概览就会开始有内容。</p><button class="secondary-button" @click="openCreate"><Plus :size="17" />记录第一笔</button></div>
      <template v-else><el-table :data="entries" class="desktop-table"><el-table-column label="记录" min-width="220"><template #default="{ row }"><div class="entry-cell"><span class="recent-mark" :class="row.type.toLowerCase()">{{ row.type === 'INCOME' ? '+' : '−' }}</span><div><strong>{{ row.categoryName }}</strong><small>{{ row.note || '没有备注' }}</small></div></div></template></el-table-column><el-table-column v-if="auth.isParent" label="成员" width="140"><template #default="{ row }">{{ row.memberName }}<small class="table-subtext">{{ row.memberNo }}</small></template></el-table-column><el-table-column label="日期" prop="occurredOn" width="130" /><el-table-column label="金额" width="150" align="right"><template #default="{ row }"><AmountText :amount="row.amount" :type="row.type" /></template></el-table-column><el-table-column label="操作" width="120" align="right"><template #default="{ row }"><button class="table-action" :aria-label="`编辑${row.categoryName}流水`" title="编辑" @click="openEdit(row)"><Pencil :size="15" /></button><button class="table-action danger" :aria-label="`删除${row.categoryName}流水`" title="删除" @click="remove(row)"><Trash2 :size="15" /></button></template></el-table-column></el-table><div class="mobile-entry-list"><article v-for="entry in entries" :key="entry.id" class="mobile-entry"><div class="entry-cell"><span class="recent-mark" :class="entry.type.toLowerCase()">{{ entry.type === 'INCOME' ? '+' : '−' }}</span><div><strong>{{ entry.categoryName }}</strong><small>{{ entry.memberName }} · {{ entry.occurredOn }}</small></div><AmountText :amount="entry.amount" :type="entry.type" /></div><div class="mobile-entry-actions"><span>{{ entry.note || '没有备注' }}</span><div><button class="table-action" :aria-label="`编辑${entry.categoryName}流水`" @click="openEdit(entry)"><Pencil :size="15" /></button><button class="table-action danger" :aria-label="`删除${entry.categoryName}流水`" @click="remove(entry)"><Trash2 :size="15" /></button></div></div></article></div><div class="pagination-row"><span>共 {{ total }} 条记录</span><el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" background @current-change="load" /></div></template>
    </section>
    <el-drawer v-model="drawer" :title="editing ? '编辑流水' : '记一笔'" direction="rtl" size="min(460px, 100%)" @close="clearNewQuery"><EntryForm :entry="editing" :members="members" :categories="categories" :parent="Boolean(auth.isParent)" :current-member-id="auth.user?.id" :saving="saving" @save="save" @cancel="closeDrawer" /></el-drawer>
    <EntryImportDialog v-model="importOpen" @committed="load" />
  </div>
</template>

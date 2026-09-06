<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Play, Plus, Pencil, Power, Trash2, Repeat2 } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import RecurringForm from '../components/RecurringForm.vue'
import { recurringApi, type RecurringPayload } from '../api/recurring'
import { memberApi } from '../api/members'
import { categoryApi } from '../api/categories'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { localMonthString } from '../utils/date'
import type { Category, Member, RecurringGenerationResult, RecurringTemplate } from '../types/domain'

const auth = useAuthStore()
const templates = ref<RecurringTemplate[]>([]), members = ref<Member[]>([]), categories = ref<Category[]>([])
const loading = ref(true), error = ref(''), saving = ref(false), dialog = ref(false), generating = ref(false)
const editing = ref<RecurringTemplate | null>(null), month = ref(localMonthString()), generation = ref<RecurringGenerationResult | null>(null)
const visibleTemplates = computed(() => templates.value)
function normalize(item: any): RecurringTemplate { return { ...item, dayOfMonth: item.dayOfMonth ?? item.occurrenceDay ?? item.day ?? 1, active: item.active ?? item.status === 'ACTIVE' } }
async function load() {
  loading.value = true; error.value = ''
  try { const [list, memberList, categoryList] = await Promise.all([recurringApi.list(), memberApi.list(), categoryApi.list(false)]); templates.value = list.map(normalize); members.value = memberList; categories.value = categoryList }
  catch (e) { error.value = errorMessage(e, '周期流水暂时无法加载') }
  finally { loading.value = false }
}
function openCreate() { editing.value = null; dialog.value = true }
function openEdit(item: RecurringTemplate) { editing.value = item; dialog.value = true }
async function save(payload: RecurringPayload) { saving.value = true; try { if (editing.value) await recurringApi.update(editing.value.id, payload); else await recurringApi.create(payload); dialog.value = false; await load(); ElMessage.success(editing.value ? '周期模板已更新' : '周期模板已创建') } catch (e) { ElMessage.error(errorMessage(e, '保存失败')) } finally { saving.value = false } }
async function toggle(item: RecurringTemplate) { try { await recurringApi.status(item.id, !item.active); await load(); ElMessage.success(item.active ? '周期模板已停用' : '周期模板已恢复') } catch (e) { ElMessage.error(errorMessage(e, '状态更新失败')) } }
async function remove(item: RecurringTemplate) {
  try {
    await ElMessageBox.confirm('删除模板不会删除已经生成的历史流水。', '删除周期模板？', { confirmButtonText: '确认删除', cancelButtonText: '保留模板', type: 'warning' })
    await recurringApi.remove(item.id); await load(); ElMessage.success('周期模板已删除')
  } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(errorMessage(e, '删除失败')) }
}
async function generate() { generating.value = true; generation.value = null; try { const result = await recurringApi.generate(month.value); generation.value = { ...result, skippedReasons: result.skippedReasons ?? result.skipReasons?.map((item) => item.reason) ?? [] }; ElMessage.success(`本月已生成 ${generation.value.created} 条流水`); await load() } catch (e) { ElMessage.error(errorMessage(e, '生成本月流水失败')) } finally { generating.value = false } }
onMounted(load)
</script>

<template>
  <div class="page-wrap recurring-page">
    <PageHeader title="周期流水" subtitle="把固定的生活支出交给模板，月底再一起核对。"><div class="page-actions-inline"><label class="month-picker">生成月份<input v-model="month" type="month" /></label><button class="primary-button" :disabled="generating" data-test="generate-recurring" @click="generate"><Play :size="16" />{{ generating ? '生成中…' : '生成本月流水' }}</button></div></PageHeader>
    <StateBlock v-if="loading" title="正在加载周期模板" :loading="true" /><StateBlock v-else-if="error" title="周期流水暂时无法加载" :description="error" action-text="重试" @action="load" />
    <template v-else><section class="surface recurring-toolbar"><div><strong>{{ visibleTemplates.length }} 个模板</strong><p>每个模板在一个月内最多生成一笔流水。</p></div><button class="secondary-button" data-test="new-recurring" @click="openCreate"><Plus :size="16" />新建模板</button></section><section v-if="generation" class="generation-result" aria-live="polite"><strong>{{ generation.month }} 生成结果</strong><span>已创建 {{ generation.created }} 条</span><span>已存在 {{ generation.alreadyGenerated }} 条</span><span>跳过 {{ generation.skipped }} 条</span><ul v-if="generation.skippedReasons?.length"><li v-for="reason in generation.skippedReasons" :key="reason">{{ reason }}</li></ul></section><section class="surface recurring-list"><div v-if="!visibleTemplates.length" class="empty-state"><Repeat2 :size="34" /><h3>还没有周期模板</h3><p>从房租、订阅或固定收入开始，让每月记账少一步。</p><button class="primary-button" @click="openCreate"><Plus :size="16" />创建第一个模板</button></div><article v-for="item in visibleTemplates" :key="item.id" class="recurring-row"><div class="recurring-symbol"><Repeat2 :size="17" /></div><div class="recurring-info"><strong>{{ item.categoryName || '未命名分类' }} · ¥{{ Number(item.amount).toFixed(2) }}</strong><span>{{ item.memberName || item.memberNo || '本人' }} · 每月 {{ item.dayOfMonth }} 日{{ item.note ? ` · ${item.note}` : '' }}</span></div><span :class="['status-tag', item.active ? 'active' : 'inactive']">{{ item.active ? '启用中' : '已停用' }}</span><div class="recurring-actions"><button class="table-action" aria-label="编辑周期模板" @click="openEdit(item)"><Pencil :size="15" /></button><button class="table-action" :class="{ danger: item.active }" :aria-label="item.active ? '停用周期模板' : '恢复周期模板'" @click="toggle(item)"><Power :size="15" /></button><button class="table-action danger" aria-label="删除周期模板" @click="remove(item)"><Trash2 :size="15" /></button></div></article></section></template>
    <div v-if="dialog" class="modal-backdrop" @click.self="dialog = false"><section class="modal-surface" role="dialog" aria-modal="true" aria-labelledby="recurring-dialog-title"><div class="surface-heading"><div><h2 id="recurring-dialog-title">{{ editing ? '编辑周期模板' : '新建周期模板' }}</h2><p>停用成员或分类后，本月生成会给出跳过原因。</p></div><button class="icon-button" aria-label="关闭模板表单" @click="dialog = false">×</button></div><RecurringForm :template="editing" :members="members" :categories="categories" :parent="Boolean(auth.isParent)" :current-member-id="auth.user?.id" :saving="saving" @save="save" @cancel="dialog = false" /></section></div>
  </div>
</template>

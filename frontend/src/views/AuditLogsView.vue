<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ScrollText, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import { auditApi } from '../api/audit'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { AuditLogPage } from '../types/domain'

const auth = useAuthStore()
const page = ref<AuditLogPage | null>(null), loading = ref(true), error = ref(''), currentPage = ref(1), pageSize = 20
async function load() { loading.value = true; error.value = ''; try { page.value = await auditApi.list({ page: currentPage.value, pageSize }) } catch (e) { error.value = errorMessage(e, '操作日志暂时无法加载') } finally { loading.value = false } }
async function go(delta: number) { if (!page.value) return; const next = currentPage.value + delta; if (next < 1 || next > page.value.totalPages) return; currentPage.value = next; await load() }
function formatDate(value: string) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) }
onMounted(load)
</script>

<template>
  <div class="page-wrap audit-page">
    <PageHeader title="操作日志" subtitle="记录家庭共同维护过的关键变化。"><span class="page-count">仅家长可见</span></PageHeader>
    <StateBlock v-if="!auth.isParent" title="暂无访问权限" description="操作日志只对家庭家长开放。" />
    <StateBlock v-else-if="loading" title="正在读取操作日志" :loading="true" />
    <StateBlock v-else-if="error" title="操作日志暂时无法加载" :description="error" action-text="重试" @action="load" />
    <section v-else class="surface audit-timeline"><div v-if="!page?.items.length" class="empty-state"><ScrollText :size="34" /><h3>还没有操作记录</h3><p>家庭成员的关键资料、流水和预算变更会显示在这里。</p></div><ol v-else class="audit-list"><li v-for="item in page.items" :key="item.id" class="audit-item"><div class="audit-dot" aria-hidden="true" /><div class="audit-copy"><div class="audit-meta"><time :datetime="item.createdAt">{{ formatDate(item.createdAt) }}</time><span>{{ item.actorName || item.actorMemberNo || '家庭成员' }}</span><span>{{ item.action }} · {{ item.objectType }}</span></div><p>{{ item.summary }}</p></div></li></ol><div v-if="page && page.totalPages > 1" class="pagination-row"><span>第 {{ page.page }} / {{ page.totalPages }} 页，共 {{ page.total }} 条</span><div class="pagination-actions"><button class="icon-button" aria-label="上一页操作日志" :disabled="currentPage <= 1" @click="go(-1)"><ChevronLeft :size="17" /></button><button class="icon-button" aria-label="下一页操作日志" :disabled="currentPage >= page.totalPages" @click="go(1)"><ChevronRight :size="17" /></button></div></div></section>
  </div>
</template>

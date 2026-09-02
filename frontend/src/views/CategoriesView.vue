<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Power, Tag, ShieldCheck } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import { categoryApi } from '../api/categories'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { Category, CategoryType } from '../types/domain'

const auth = useAuthStore(), categories = ref<Category[]>([]), loading = ref(true), error = ref(''), dialog = ref(false), saving = ref(false), editing = ref<Category | null>(null), activeTab = ref<CategoryType>('EXPENSE')
const form = reactive({ type: 'EXPENSE' as CategoryType, name: '' })
const visible = computed(() => categories.value.filter((category) => category.type === activeTab.value))
async function load() { loading.value = true; error.value = ''; try { categories.value = await categoryApi.list(true) } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
function openCreate() { editing.value = null; Object.assign(form, { type: activeTab.value, name: '' }); dialog.value = true }
function openEdit(category: Category) { editing.value = category; form.name = category.name; dialog.value = true }
async function save() { if (!form.name.trim()) { ElMessage.warning('请输入分类名称'); return }; saving.value = true; try { if (editing.value) await categoryApi.update(editing.value.id, form.name); else await categoryApi.create(form); ElMessage.success(editing.value ? '分类已更新' : '分类已创建'); dialog.value = false; await load() } catch (e) { ElMessage.error(errorMessage(e, '保存失败，分类名称可能已存在')) } finally { saving.value = false } }
async function toggle(category: Category) { const active = category.status !== 'ACTIVE'; if (!active) { try { await ElMessageBox.confirm(`停用后不能用于新增流水，但历史记录仍会保留。`, '停用这个分类？', { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' }) } catch { return } } try { await categoryApi.status(category.id, active); ElMessage.success(active ? '分类已恢复' : '分类已停用'); await load() } catch (e) { ElMessage.error(errorMessage(e, '状态更新失败')) } }
onMounted(load)
</script>

<template>
  <div class="page-wrap"><PageHeader title="收支分类" subtitle="用熟悉的方式整理家庭里的每一种流动。"><button v-if="auth.isParent" class="primary-button" @click="openCreate"><Plus :size="17" />新建分类</button></PageHeader><StateBlock v-if="loading" title="正在加载分类" :loading="true" /><StateBlock v-else-if="error" title="分类暂时无法加载" :description="error" action-text="重试" @action="load" /><section v-else class="surface categories-surface"><div class="permission-note"><ShieldCheck :size="18" /><span>{{ auth.isParent ? '系统分类用于全家共享；你可以维护家庭自定义分类。' : '系统分类和家庭自定义分类均可用于查看，只有家长可以维护。' }}</span></div><div class="category-tabs"><button :class="{ active: activeTab === 'EXPENSE' }" @click="activeTab = 'EXPENSE'">支出 <span>{{ categories.filter((category) => category.type === 'EXPENSE').length }}</span></button><button :class="{ active: activeTab === 'INCOME' }" @click="activeTab = 'INCOME'">收入 <span>{{ categories.filter((category) => category.type === 'INCOME').length }}</span></button></div><div class="category-list"><article v-for="category in visible" :key="category.id" class="category-row"><div class="category-symbol" :class="category.scope.toLowerCase()"><Tag :size="17" /></div><div class="category-name"><strong>{{ category.name }}</strong><small>{{ category.scope === 'SYSTEM' ? '系统分类' : '家庭自定义' }}</small></div><span :class="['status-tag', category.status.toLowerCase()]">{{ category.status === 'ACTIVE' ? '启用中' : '已停用' }}</span><div v-if="auth.isParent && category.scope === 'CUSTOM'" class="category-actions"><button class="table-action" title="编辑" @click="openEdit(category)"><Pencil :size="15" /></button><button class="table-action" :class="{ danger: category.status === 'ACTIVE' }" :title="category.status === 'ACTIVE' ? '停用' : '恢复'" @click="toggle(category)"><Power :size="15" /></button></div></article></div></section><el-dialog v-model="dialog" :title="editing ? '编辑分类' : '新建分类'" width="min(440px, calc(100% - 32px))"><form class="form-stack" @submit.prevent="save"><div v-if="!editing" class="field"><label for="category-type">分类类型</label><el-select id="category-type" v-model="form.type"><el-option label="支出" value="EXPENSE" /><el-option label="收入" value="INCOME" /></el-select></div><div class="field"><label for="category-name">分类名称</label><el-input id="category-name" v-model="form.name" maxlength="30" show-word-limit /></div><div class="form-actions"><button type="button" class="secondary-button" @click="dialog = false">取消</button><button class="primary-button" :disabled="saving">保存分类</button></div></form></el-dialog></div>
</template>

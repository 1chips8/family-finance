<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pencil, ShieldCheck, UserRound, UserX, UserCheck, RefreshCw } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import StateBlock from '../components/StateBlock.vue'
import { memberApi } from '../api/members'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { Member, Role } from '../types/domain'

const auth = useAuthStore(), members = ref<Member[]>([]), loading = ref(true), error = ref(''), dialog = ref(false), saving = ref(false), editing = ref<Member | null>(null)
const form = reactive({ memberNo: '', displayName: '', role: 'MEMBER' as Role })
const activeParents = computed(() => members.value.filter((member) => member.status === 'ACTIVE' && member.role === 'PARENT').length)
async function load() { loading.value = true; error.value = ''; try { members.value = await memberApi.list() } catch (e) { error.value = errorMessage(e) } finally { loading.value = false } }
function openEdit(member: Member) { editing.value = member; Object.assign(form, { memberNo: member.memberNo, displayName: member.displayName, role: member.role }); dialog.value = true }
async function save() { saving.value = true; try { await memberApi.update(editing.value!.id, form); ElMessage.success('成员资料已更新'); dialog.value = false; await load() } catch (e) { ElMessage.error(errorMessage(e, '保存失败')) } finally { saving.value = false } }
async function toggle(member: Member) {
  const active = member.status !== 'ACTIVE'
  if (!active) { try { await ElMessageBox.confirm(`停用后，${member.displayName}将不能登录，但历史流水仍会保留。`, '停用这名成员？', { confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning' }) } catch { return } }
  try { await memberApi.status(member.id, active); ElMessage.success(active ? '成员已恢复' : '成员已停用'); await load() } catch (e) { ElMessage.error(errorMessage(e, '状态更新失败')) }
}
onMounted(load)
</script>

<template>
  <div class="page-wrap"><PageHeader title="家庭成员" subtitle="看见每个人，也守住每个人的边界。"><span class="page-count">{{ members.length }} 位成员</span></PageHeader><StateBlock v-if="loading" title="正在加载成员" :loading="true" /><StateBlock v-else-if="error" title="成员列表暂时无法加载" :description="error" action-text="重试" @action="load" /><section v-else class="surface members-surface"><div class="permission-note"><ShieldCheck :size="18" /><span>{{ auth.isParent ? '你是家长，可以维护成员资料和状态。' : '你可以查看家庭成员名单，但不能修改成员资料。' }}</span></div><div class="member-cards"><article v-for="member in members" :key="member.id" class="member-card"><div class="member-card-main"><div class="large-avatar">{{ member.displayName.slice(0, 1) }}</div><div><h3>{{ member.displayName }}</h3><p>{{ member.memberNo }} · {{ member.role === 'PARENT' ? '家长' : '普通成员' }}</p></div><span :class="['status-tag', member.status.toLowerCase()]">{{ member.status === 'ACTIVE' ? '正常' : '已停用' }}</span></div><div v-if="auth.isParent" class="member-card-actions"><button class="secondary-button small" @click="openEdit(member)"><Pencil :size="14" />编辑资料</button><button class="text-button" :class="{ danger: member.status === 'ACTIVE' }" :disabled="member.role === 'PARENT' && member.status === 'ACTIVE' && activeParents <= 1" @click="toggle(member)">{{ member.status === 'ACTIVE' ? '停用成员' : '恢复成员' }}</button></div><div v-else class="member-card-foot"><UserRound :size="14" />家庭成员</div></article></div></section><el-dialog v-model="dialog" title="编辑成员资料" width="min(440px, calc(100% - 32px))"><form class="form-stack" @submit.prevent="save"><div class="field"><label for="member-no">成员编号</label><el-input id="member-no" v-model="form.memberNo" /></div><div class="field"><label for="member-name">姓名</label><el-input id="member-name" v-model="form.displayName" /></div><div class="field"><label for="member-role">角色</label><el-select id="member-role" v-model="form.role"><el-option label="家长" value="PARENT" /><el-option label="普通成员" value="MEMBER" /></el-select></div><div class="form-actions"><button type="button" class="secondary-button" @click="dialog = false">取消</button><button class="primary-button" :disabled="saving">保存修改</button></div></form></el-dialog></div>
</template>

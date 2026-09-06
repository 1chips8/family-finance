<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Category, Entry, LedgerType, Member } from '../types/domain'
import type { EntryPayload } from '../api/entries'
import { localDateString } from '../utils/date'

const props = defineProps<{ entry?: Entry | null; members: Member[]; categories: Category[]; parent: boolean; currentMemberId?: number; saving?: boolean }>()
const emit = defineEmits<{ save: [payload: EntryPayload]; cancel: [] }>()
const today = localDateString()
const form = reactive<EntryPayload>({ memberId: undefined, type: 'EXPENSE', categoryId: 0, amount: '', occurredOn: today, note: '' })
const fieldErrors = reactive<Record<string, string>>({})
const activeCategories = computed(() => props.categories.filter((category) => category.type === form.type && category.status === 'ACTIVE'))
const currentMember = computed(() => props.members.find((member) => member.id === props.currentMemberId))
watch(() => props.entry, (entry) => {
  Object.assign(form, entry ? { memberId: entry.memberId, type: entry.type, categoryId: entry.categoryId, amount: String(entry.amount), occurredOn: entry.occurredOn, note: entry.note || '' } : { memberId: props.members[0]?.id, type: 'EXPENSE', categoryId: 0, amount: '', occurredOn: today, note: '' })
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
}, { immediate: true })
watch(() => props.members, (members) => { if (!props.entry && !form.memberId) form.memberId = members[0]?.id }, { deep: true })
watch(() => form.type, () => { if (!activeCategories.value.some((category) => category.id === form.categoryId)) form.categoryId = activeCategories.value[0]?.id || 0 })
function submit() {
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key])
  if (!form.categoryId) fieldErrors.categoryId = '请选择分类'
  if (!form.amount || Number(form.amount) <= 0 || !/^\d+(\.\d{1,2})?$/.test(form.amount)) fieldErrors.amount = '请输入大于 0 且最多两位小数的金额'
  if (!form.occurredOn || form.occurredOn > today) fieldErrors.occurredOn = '发生日期不能晚于今天'
  const firstError = Object.values(fieldErrors)[0]
  if (firstError) { ElMessage.warning(firstError); return }
  emit('save', { ...form, memberId: props.parent ? form.memberId : undefined, note: form.note?.trim() })
}
</script>

<template>
  <form class="form-stack entry-form" @submit.prevent="submit">
    <div v-if="parent" class="field"><label for="entry-member">归属成员</label><el-select id="entry-member" v-model="form.memberId" size="large" placeholder="选择成员"><el-option v-for="member in members.filter((item) => item.status === 'ACTIVE')" :key="member.id" :label="`${member.memberNo} · ${member.displayName}`" :value="member.id" /></el-select></div>
    <div v-else class="locked-member"><span class="field-caption">归属成员</span><strong>{{ currentMember?.displayName || '本人' }}</strong><small>普通成员只能记录自己的流水</small></div>
    <div class="field"><span class="field-caption">收支类型</span><div class="type-toggle" role="group" aria-label="收支类型"><button type="button" :class="{ active: form.type === 'EXPENSE' }" :aria-pressed="form.type === 'EXPENSE'" @click="form.type = 'EXPENSE'">支出</button><button type="button" :class="{ active: form.type === 'INCOME' }" :aria-pressed="form.type === 'INCOME'" @click="form.type = 'INCOME'">收入</button></div></div>
    <div class="field"><label for="entry-category">分类</label><el-select id="entry-category" v-model="form.categoryId" size="large" placeholder="选择分类" aria-describedby="entry-category-error"><el-option v-for="category in activeCategories" :key="category.id" :label="category.name" :value="category.id" /></el-select><p v-if="fieldErrors.categoryId" id="entry-category-error" class="form-error" role="alert">{{ fieldErrors.categoryId }}</p></div>
    <div class="field"><label for="entry-amount">金额</label><el-input id="entry-amount" v-model="form.amount" size="large" inputmode="decimal" placeholder="0.00" aria-describedby="entry-amount-error"><template #prefix>¥</template></el-input><p v-if="fieldErrors.amount" id="entry-amount-error" class="form-error" role="alert">{{ fieldErrors.amount }}</p></div>
    <div class="field"><label for="entry-date">发生日期</label><el-date-picker id="entry-date" v-model="form.occurredOn" type="date" value-format="YYYY-MM-DD" size="large" :disabled-date="(date: Date) => date.getTime() > Date.now()" placeholder="选择日期" aria-describedby="entry-date-error" /><p v-if="fieldErrors.occurredOn" id="entry-date-error" class="form-error" role="alert">{{ fieldErrors.occurredOn }}</p></div>
    <div class="field"><label for="entry-note">备注 <span class="optional">选填</span></label><el-input id="entry-note" v-model="form.note" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="这笔钱花在了哪里？" /></div>
    <div class="form-actions"><button type="button" class="secondary-button" @click="emit('cancel')">取消</button><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中…' : props.entry ? '保存修改' : '记下这笔' }}</button></div>
  </form>
</template>

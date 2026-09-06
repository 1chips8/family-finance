<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { Category, Member, RecurringTemplate } from '../types/domain'
import type { RecurringPayload } from '../api/recurring'

const props = defineProps<{ template?: RecurringTemplate | null; members: Member[]; categories: Category[]; parent: boolean; currentMemberId?: number; saving?: boolean }>()
const emit = defineEmits<{ save: [payload: RecurringPayload]; cancel: [] }>()
const form = reactive<RecurringPayload>({ memberId: undefined, type: 'EXPENSE', categoryId: 0, amount: '', dayOfMonth: 1, note: '', active: true })
const errors = reactive<Record<string, string>>({})
const activeCategories = computed(() => props.categories.filter((item) => item.type === form.type && item.status === 'ACTIVE'))
const currentMember = computed(() => props.members.find((member) => member.id === props.currentMemberId))
watch(() => props.template, (template) => {
  Object.assign(form, template ? { memberId: template.memberId, type: template.type, categoryId: template.categoryId, amount: String(template.amount), dayOfMonth: template.dayOfMonth, note: template.note || '', active: template.active } : { memberId: props.members[0]?.id, type: 'EXPENSE', categoryId: activeCategories.value[0]?.id || 0, amount: '', dayOfMonth: 1, note: '', active: true })
  Object.keys(errors).forEach((key) => delete errors[key])
}, { immediate: true })
watch(() => props.members, (members) => { if (!props.template && !form.memberId) form.memberId = members[0]?.id }, { deep: true })
watch(() => form.type, () => { if (!activeCategories.value.some((item) => item.id === form.categoryId)) form.categoryId = activeCategories.value[0]?.id || 0 })
function submit() {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (props.parent && !form.memberId) errors.memberId = '请选择成员'
  if (!form.categoryId) errors.categoryId = '请选择分类'
  if (!/^\d+(\.\d{1,2})?$/.test(form.amount) || Number(form.amount) <= 0) errors.amount = '请输入有效金额'
  if (!Number.isInteger(Number(form.dayOfMonth)) || Number(form.dayOfMonth) < 1 || Number(form.dayOfMonth) > 31) errors.dayOfMonth = '发生日需为 1-31'
  if (Object.keys(errors).length) return
  emit('save', { ...form, memberId: props.parent ? form.memberId : undefined, note: form.note?.trim() })
}
</script>

<template>
  <form class="form-stack recurring-form" @submit.prevent="submit">
    <div v-if="parent" class="field"><label for="recurring-member">归属成员</label><select id="recurring-member" v-model="form.memberId" aria-describedby="recurring-member-error"><option v-for="member in members.filter((item) => item.status === 'ACTIVE')" :key="member.id" :value="member.id">{{ member.memberNo }} · {{ member.displayName }}</option></select><p v-if="errors.memberId" id="recurring-member-error" class="form-error">{{ errors.memberId }}</p></div>
    <div v-else class="locked-member"><span class="field-caption">归属成员</span><strong>{{ currentMember?.displayName || '本人' }}</strong><small>普通成员只能创建自己的周期流水</small></div>
    <div class="field"><span class="field-caption">收支类型</span><div class="type-toggle" role="group" aria-label="收支类型"><button type="button" :aria-pressed="form.type === 'EXPENSE'" :class="{ active: form.type === 'EXPENSE' }" @click="form.type = 'EXPENSE'">支出</button><button type="button" :aria-pressed="form.type === 'INCOME'" :class="{ active: form.type === 'INCOME' }" @click="form.type = 'INCOME'">收入</button></div></div>
    <div class="field"><label for="recurring-category">分类</label><select id="recurring-category" v-model="form.categoryId" aria-describedby="recurring-category-error"><option v-for="category in activeCategories" :key="category.id" :value="category.id">{{ category.name }}</option></select><p v-if="errors.categoryId" id="recurring-category-error" class="form-error">{{ errors.categoryId }}</p></div>
    <div class="field"><label for="recurring-amount">金额</label><input id="recurring-amount" v-model="form.amount" inputmode="decimal" aria-describedby="recurring-amount-error" placeholder="0.00" /><p v-if="errors.amount" id="recurring-amount-error" class="form-error">{{ errors.amount }}</p></div>
    <div class="field"><label for="recurring-day">每月发生日</label><input id="recurring-day" v-model.number="form.dayOfMonth" type="number" min="1" max="31" aria-describedby="recurring-day-error" /><small id="recurring-day-error" class="form-hint">遇到没有 31 号的月份会自动落在月底。</small><p v-if="errors.dayOfMonth" class="form-error">{{ errors.dayOfMonth }}</p></div>
    <div class="field"><label for="recurring-note">备注 <span class="optional">选填</span></label><textarea id="recurring-note" v-model="form.note" rows="3" maxlength="255" /></div>
    <div class="form-actions"><button type="button" class="secondary-button" @click="emit('cancel')">取消</button><button type="submit" class="primary-button" :disabled="saving">{{ saving ? '保存中…' : template ? '保存修改' : '创建周期模板' }}</button></div>
  </form>
</template>

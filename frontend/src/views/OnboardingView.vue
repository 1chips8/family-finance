<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { householdApi } from '../api/households'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'

const router = useRouter(), auth = useAuthStore()
const createForm = reactive({ name: '' }), joinForm = reactive({ inviteCode: '' })
const fieldErrors = reactive({ name: '', inviteCode: '' })
const active = ref<'create' | 'join'>('create'), loading = ref(false), error = ref('')
async function submit() {
  error.value = ''
  fieldErrors.name = active.value === 'create' && !createForm.name.trim() ? '请给家庭起一个名字' : ''
  fieldErrors.inviteCode = active.value === 'join' && !/^[A-Za-z0-9]{8}$/.test(joinForm.inviteCode) ? '请输入8位邀请码' : ''
  if (fieldErrors.name || fieldErrors.inviteCode) return
  loading.value = true
  try {
    const household = active.value === 'create' ? await householdApi.create(createForm.name) : await householdApi.join(joinForm.inviteCode)
    auth.setHousehold(household); ElMessage.success(active.value === 'create' ? '家庭创建成功' : '加入家庭成功'); await router.push('/dashboard')
  } catch (e) { error.value = errorMessage(e, '操作失败，请检查信息后重试') }
  finally { loading.value = false }
}
</script>

<template>
  <main class="onboarding-page">
    <div class="onboarding-intro"><span class="mini-label">欢迎来到家账</span><h1>先找到你的<br /><em>家庭空间。</em></h1><p>一个家庭，一份共同的视野。你可以创建新的家庭，也可以使用家长提供的邀请码加入。</p></div>
    <section class="onboarding-panel">
      <div class="segmented"><button type="button" :class="{ active: active === 'create' }" :aria-pressed="active === 'create'" @click="active = 'create'; error = ''; fieldErrors.inviteCode = ''">创建家庭</button><button type="button" :class="{ active: active === 'join' }" :aria-pressed="active === 'join'" @click="active = 'join'; error = ''; fieldErrors.name = ''">输入邀请码</button></div>
      <form class="form-stack" @submit.prevent="submit">
        <div v-if="active === 'create'" class="field"><label for="household-name">家庭名称</label><el-input id="household-name" v-model="createForm.name" size="large" placeholder="例如：小林一家" maxlength="80" show-word-limit aria-describedby="household-name-error" :aria-invalid="Boolean(fieldErrors.name)" /><p v-if="fieldErrors.name" id="household-name-error" class="form-error" role="alert">{{ fieldErrors.name }}</p></div>
        <div v-else class="field"><label for="invite-code">家庭邀请码</label><el-input id="invite-code" v-model="joinForm.inviteCode" size="large" placeholder="8位大写字母或数字" maxlength="8" aria-describedby="invite-code-error" :aria-invalid="Boolean(fieldErrors.inviteCode)" @input="joinForm.inviteCode = joinForm.inviteCode.toUpperCase()" /><p v-if="fieldErrors.inviteCode" id="invite-code-error" class="form-error" role="alert">{{ fieldErrors.inviteCode }}</p></div>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="primary-button full" type="submit" :disabled="loading">{{ loading ? '正在处理…' : active === 'create' ? '创建家庭' : '加入家庭' }}</button>
      </form>
      <p class="onboarding-note">你之后仍可以在家庭成员页查看成员编号和当前权限。</p>
    </section>
  </main>
</template>

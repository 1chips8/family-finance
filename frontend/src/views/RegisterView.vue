<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { errorMessage } from '../api/client'

const router = useRouter(), auth = useAuthStore()
const form = reactive({ username: '', displayName: '', password: '', confirmPassword: '' })
const fieldErrors = reactive({ username: '', displayName: '', password: '', confirmPassword: '' })
const loading = ref(false), error = ref('')
function validate() {
  Object.assign(fieldErrors, { username: '', displayName: '', password: '', confirmPassword: '' })
  if (!/^[A-Za-z0-9_]{4,32}$/.test(form.username)) fieldErrors.username = '用户名需为4-32位字母、数字或下划线'
  if (!form.displayName.trim()) fieldErrors.displayName = '请输入姓名'
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(form.password)) fieldErrors.password = '密码需为8-72位且同时包含字母和数字'
  if (form.password !== form.confirmPassword) fieldErrors.confirmPassword = '两次输入的密码不一致'
  return !Object.values(fieldErrors).some(Boolean)
}
async function submit() {
  error.value = ''; if (!validate()) return
  loading.value = true
  try { await auth.register({ username: form.username, displayName: form.displayName, password: form.password }); ElMessage.success('注册成功，请登录'); await router.push('/login') }
  catch (e) { error.value = errorMessage(e, '注册失败，请检查填写内容') }
  finally { loading.value = false }
}
</script>

<template>
  <main class="auth-card register-card">
    <div class="auth-card-copy"><span class="mini-label">从今天开始</span><h1>让家庭账目，<br /><em>变得轻一点。</em></h1><p>先创建账号，再创建家庭或输入邀请码加入。</p></div>
    <form class="form-stack" @submit.prevent="submit">
      <div class="field"><label for="register-username">用户名</label><el-input id="register-username" v-model="form.username" placeholder="4-32位字母、数字或下划线" autocomplete="username" size="large" aria-describedby="register-username-error" :aria-invalid="Boolean(fieldErrors.username)" /><p v-if="fieldErrors.username" id="register-username-error" class="form-error" role="alert">{{ fieldErrors.username }}</p></div>
      <div class="field"><label for="register-name">你的称呼</label><el-input id="register-name" v-model="form.displayName" placeholder="例如：林晓" autocomplete="name" size="large" aria-describedby="register-name-error" :aria-invalid="Boolean(fieldErrors.displayName)" /><p v-if="fieldErrors.displayName" id="register-name-error" class="form-error" role="alert">{{ fieldErrors.displayName }}</p></div>
      <div class="field"><label for="register-password">设置密码</label><el-input id="register-password" v-model="form.password" type="password" show-password placeholder="至少8位，包含字母和数字" autocomplete="new-password" size="large" aria-describedby="register-password-error" :aria-invalid="Boolean(fieldErrors.password)" /><p v-if="fieldErrors.password" id="register-password-error" class="form-error" role="alert">{{ fieldErrors.password }}</p></div>
      <div class="field"><label for="register-confirm">确认密码</label><el-input id="register-confirm" v-model="form.confirmPassword" type="password" show-password placeholder="再输入一次密码" autocomplete="new-password" size="large" aria-describedby="register-confirm-error" :aria-invalid="Boolean(fieldErrors.confirmPassword)" /><p v-if="fieldErrors.confirmPassword" id="register-confirm-error" class="form-error" role="alert">{{ fieldErrors.confirmPassword }}</p></div>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button full" type="submit" :disabled="loading">{{ loading ? '正在创建…' : '创建账号' }}</button>
      <p class="form-hint">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
    </form>
  </main>
</template>

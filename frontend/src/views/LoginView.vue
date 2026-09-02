<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { errorMessage } from '../api/client'

const router = useRouter(), route = useRoute(), auth = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false), error = ref('')
const passwordChanged = route.query.passwordChanged === '1'
async function submit() {
  error.value = ''
  if (!form.username || !form.password) { error.value = '请输入用户名和密码'; return }
  loading.value = true
  try { await auth.login(form.username, form.password); await router.push(String(route.query.redirect || '/dashboard')) }
  catch (e) { error.value = errorMessage(e, '登录失败，请检查用户名和密码') }
  finally { loading.value = false }
}
</script>

<template>
  <main class="auth-card login-card">
    <div class="auth-card-copy"><span class="mini-label">家庭财务工作台</span><h1>把每一笔，<br /><em>放回生活里。</em></h1><p>清楚记录，共同计划。只和家人分享真正需要分享的部分。</p></div>
    <form class="form-stack" @submit.prevent="submit">
      <div class="field"><label for="login-username">用户名</label><el-input id="login-username" v-model="form.username" placeholder="输入用户名" autocomplete="username" size="large" /></div>
      <div class="field"><div class="field-label"><label for="login-password">密码</label><RouterLink to="/register">还没有账号？</RouterLink></div><el-input id="login-password" v-model="form.password" type="password" show-password placeholder="输入密码" autocomplete="current-password" size="large" /></div>
      <p v-if="passwordChanged" class="form-success" role="status">密码已修改，请使用新密码重新登录。</p>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="primary-button full" type="submit" :disabled="loading">{{ loading ? '正在登录…' : '登录家账' }}</button>
      <p class="form-hint">登录即表示你同意仅在家庭范围内使用本系统。</p>
    </form>
  </main>
</template>

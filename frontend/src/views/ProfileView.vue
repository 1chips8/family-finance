<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, LockKeyhole, UserRound } from 'lucide-vue-next'
import PageHeader from '../components/PageHeader.vue'
import { profileApi } from '../api/profile'
import { errorMessage } from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore(), profile = reactive({ displayName: auth.user?.displayName || '' }), password = reactive({ current: '', next: '', confirm: '' }), savingProfile = ref(false), savingPassword = ref(false)
async function saveProfile() { if (!profile.displayName.trim()) { ElMessage.warning('姓名不能为空'); return }; savingProfile.value = true; try { auth.setUser(await profileApi.update(profile.displayName)); ElMessage.success('个人资料已更新') } catch (e) { ElMessage.error(errorMessage(e, '保存失败')) } finally { savingProfile.value = false } }
async function savePassword() { if (!/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/.test(password.next)) { ElMessage.warning('新密码需为8-72位且同时包含字母和数字'); return }; if (password.next !== password.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }; savingPassword.value = true; try { await profileApi.password(password.current, password.next); Object.assign(password, { current: '', next: '', confirm: '' }); ElMessage.success('密码已修改，请重新登录') } catch (e) { ElMessage.error(errorMessage(e, '密码修改失败')) } finally { savingPassword.value = false } }
</script>

<template>
  <div class="page-wrap"><PageHeader title="个人资料" subtitle="只修改属于你的信息。" /><div class="profile-grid"><section class="surface profile-card"><div class="profile-card-title"><span class="profile-icon"><UserRound :size="19" /></span><div><h2>基本资料</h2><p>家庭里的称呼和账号信息</p></div></div><form class="form-stack" @submit.prevent="saveProfile"><div class="field"><label for="profile-username">用户名</label><el-input id="profile-username" :model-value="auth.user?.username" disabled /></div><div class="field"><label for="profile-name">显示姓名</label><el-input id="profile-name" v-model="profile.displayName" size="large" /></div><button class="primary-button" :disabled="savingProfile"><Check :size="16" />{{ savingProfile ? '保存中…' : '保存资料' }}</button></form></section><section class="surface profile-card"><div class="profile-card-title"><span class="profile-icon"><LockKeyhole :size="19" /></span><div><h2>修改密码</h2><p>使用新密码保护你的账号</p></div></div><form class="form-stack" @submit.prevent="savePassword"><div class="field"><label for="password-current">当前密码</label><el-input id="password-current" v-model="password.current" type="password" show-password autocomplete="current-password" /></div><div class="field"><label for="password-new">新密码</label><el-input id="password-new" v-model="password.next" type="password" show-password autocomplete="new-password" placeholder="8-72位，包含字母和数字" /></div><div class="field"><label for="password-confirm">确认新密码</label><el-input id="password-confirm" v-model="password.confirm" type="password" show-password autocomplete="new-password" /></div><button class="primary-button" :disabled="savingPassword"><LockKeyhole :size="16" />{{ savingPassword ? '更新中…' : '更新密码' }}</button></form></section></div></div>
</template>

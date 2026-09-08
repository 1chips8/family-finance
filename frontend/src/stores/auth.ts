import { defineStore } from 'pinia'
import { authApi } from '../api/auth'
import { householdApi } from '../api/households'
import { errorStatus } from '../api/client'
import type { Household, User } from '../types/domain'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    loading: false,
    user: null as User | null,
    household: null as Household | null,
  }),
  getters: {
    authenticated: (state) => state.user !== null,
    inHousehold: (state) => Boolean(state.user?.hasHousehold),
    isParent: (state) => state.user?.role === 'PARENT',
  },
  actions: {
    async restore() {
      // 同一个页面生命周期只恢复一次，避免每次路由切换都重复请求 /me。
      if (this.initialized) return
      this.loading = true
      try {
        this.user = await authApi.me()
        if (this.user.hasHousehold) {
          // 用户信息仍可用于登录态判断；家庭资料失败时由具体页面提供重试入口。
          try {
            this.household = await householdApi.current()
          } catch {
            this.household = null
          }
        }
      } catch (error) {
        if (errorStatus(error) !== 401) console.warn('session restore failed', error)
        this.user = null
      } finally {
        this.initialized = true
        this.loading = false
      }
    },
    async login(username: string, password: string) {
      await authApi.csrf()
      this.user = await authApi.login({ username, password })
      if (this.user.hasHousehold) this.household = await householdApi.current()
    },
    async register(payload: { username: string; displayName: string; password: string }) {
      await authApi.csrf()
      await authApi.register(payload)
    },
    async ensureCsrf() {
      await authApi.csrf()
    },
    async logout() {
      // 即使服务端 Session 已过期，也必须清空浏览器中的本地身份状态。
      try {
        await authApi.logout()
      } finally {
        this.user = null
        this.household = null
        this.initialized = true
      }
    },
    setUser(user: User) {
      this.user = user
    },
    setHousehold(household: Household) {
      this.household = household
      // 创建或加入家庭后同步派生字段，路由守卫无需再请求一次 /me。
      if (this.user) {
        this.user = {
          ...this.user,
          hasHousehold: true,
          memberNo: household.currentMemberNo,
          role: household.currentRole,
          household: { id: household.id, name: household.name },
        }
      }
    },
  },
})

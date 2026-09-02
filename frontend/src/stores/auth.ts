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
      if (this.initialized) return
      this.loading = true
      try {
        this.user = await authApi.me()
        if (this.user.hasHousehold) {
          try { this.household = await householdApi.current() } catch { this.household = null }
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
    async ensureCsrf() { await authApi.csrf() },
    async logout() {
      try { await authApi.logout() } finally {
        this.user = null
        this.household = null
        this.initialized = true
      }
    },
    setUser(user: User) { this.user = user },
    setHousehold(household: Household) {
      this.household = household
      if (this.user) this.user = { ...this.user, hasHousehold: true, memberNo: household.currentMemberNo, role: household.currentRole,
        household: { id: household.id, name: household.name } }
    },
  },
})

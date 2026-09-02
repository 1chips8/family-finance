import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it, beforeEach } from 'vitest'
import { useAuthStore } from '../stores/auth'

beforeEach(() => setActivePinia(createPinia()))

describe('route state contract', () => {
  it('distinguishes authenticated users without a household from household users', () => {
    const store = useAuthStore()
    expect(store.inHousehold).toBe(false)
    store.setUser({ id: 1, username: 'a', displayName: 'A', memberNo: null, role: null, status: 'ACTIVE', hasHousehold: false, household: null })
    expect(store.authenticated).toBe(true)
    expect(store.inHousehold).toBe(false)
    store.setHousehold({ id: 9, name: '家庭', inviteCode: null, currentMemberId: 1, currentMemberNo: 'M001', currentRole: 'PARENT' })
    expect(store.inHousehold).toBe(true)
    expect(store.isParent).toBe(true)
  })
})

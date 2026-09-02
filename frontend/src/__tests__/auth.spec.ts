import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'

vi.mock('../api/auth', () => ({ authApi: { me: vi.fn(), csrf: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() } }))
vi.mock('../api/households', () => ({ householdApi: { current: vi.fn() } }))

describe('auth store', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('clears the user when session recovery receives 401', async () => {
    vi.mocked(authApi.me).mockRejectedValueOnce({ response: { status: 401 } })
    const store = useAuthStore()
    await store.restore()
    expect(store.initialized).toBe(true)
    expect(store.authenticated).toBe(false)
    expect(store.user).toBeNull()
  })
})

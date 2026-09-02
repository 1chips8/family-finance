import { describe, expect, it, vi } from 'vitest'
import { apiClient, errorMessage, errorStatus, FORBIDDEN_EVENT } from '../api/client'

describe('API error helpers', () => {
  it('exposes Chinese API message and HTTP status for 401/403 handling', () => {
    const error = { response: { status: 403, data: { message: '仅家长可以执行此操作' } } }
    expect(errorStatus(error)).toBe(403)
    expect(errorMessage(error)).toBe('仅家长可以执行此操作')
  })

  it('emits a forbidden navigation event for 403 but not for 401', async () => {
    const listener = vi.fn()
    const httpClient = apiClient as unknown as { defaults: { adapter: unknown } }
    const originalAdapter = httpClient.defaults.adapter
    window.addEventListener(FORBIDDEN_EVENT, listener)
    try {
      httpClient.defaults.adapter = vi.fn(() => Promise.reject({ response: { status: 403 } }))
      await expect(apiClient.get('/parent-only')).rejects.toBeDefined()
      expect(listener).toHaveBeenCalledOnce()

      httpClient.defaults.adapter = vi.fn(() => Promise.reject({ response: { status: 401 } }))
      await expect(apiClient.get('/session-expired')).rejects.toBeDefined()
      expect(listener).toHaveBeenCalledOnce()
    } finally {
      httpClient.defaults.adapter = originalAdapter
      window.removeEventListener(FORBIDDEN_EVENT, listener)
    }
  })
})

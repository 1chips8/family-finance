import { describe, expect, it } from 'vitest'
import { errorMessage, errorStatus } from '../api/client'

describe('API error helpers', () => {
  it('exposes Chinese API message and HTTP status for 401/403 handling', () => {
    const error = { response: { status: 403, data: { message: '仅家长可以执行此操作' } } }
    expect(errorStatus(error)).toBe(403)
    expect(errorMessage(error)).toBe('仅家长可以执行此操作')
  })
})

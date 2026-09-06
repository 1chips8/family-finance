import { describe, expect, it } from 'vitest'
import { localDateString, localMonthString } from '../utils/date'

describe('local date helpers', () => {
  it('formats the calendar date without UTC conversion', () => {
    const date = new Date(2026, 0, 1, 0, 5)
    expect(localDateString(date)).toBe('2026-01-01')
    expect(localMonthString(date)).toBe('2026-01')
  })
})

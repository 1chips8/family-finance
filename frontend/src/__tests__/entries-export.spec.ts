import { describe, expect, it } from 'vitest'
import { entryExportUrl } from '../api/entries'

describe('entry CSV export URL', () => {
  it('omits empty filters so browsers can download directly from the authenticated endpoint', () => {
    expect(entryExportUrl({ type: '', categoryId: '', memberId: '', from: '', to: '' }))
      .toBe('/api/entries/export')
  })

  it('preserves active filters', () => {
    expect(entryExportUrl({ type: 'EXPENSE', categoryId: 7, memberId: 2, from: '2026-09-01', to: '2026-09-30' }))
      .toBe('/api/entries/export?type=EXPENSE&categoryId=7&memberId=2&from=2026-09-01&to=2026-09-30')
  })
})

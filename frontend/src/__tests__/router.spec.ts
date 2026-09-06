import { describe, expect, it } from 'vitest'
import { appRoutes } from '../router'

describe('application routes', () => {
  it('protects the forbidden page with authentication and household metadata', () => {
    const forbidden = appRoutes.find((route) => route.path === '/forbidden')
    expect(forbidden?.meta).toMatchObject({ auth: true, household: true })
  })

  it('declares all course-design pages', () => {
    expect(appRoutes.map((route) => route.path)).toEqual([
      '/login',
      '/register',
      '/onboarding',
      '/dashboard',
      '/entries',
      '/budgets',
      '/members',
      '/categories',
      '/recurring',
      '/audit-logs',
      '/profile',
      '/forbidden',
    ])
  })
})

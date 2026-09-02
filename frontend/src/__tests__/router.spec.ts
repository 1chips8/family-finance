import { describe, expect, it } from 'vitest'
import { appRoutes } from '../router'

describe('application routes', () => {
  it('declares all course-design pages', () => {
    expect(appRoutes.map((route) => route.path)).toEqual([
      '/login',
      '/register',
      '/onboarding',
      '/dashboard',
      '/entries',
      '/members',
      '/categories',
      '/profile',
    ])
  })
})

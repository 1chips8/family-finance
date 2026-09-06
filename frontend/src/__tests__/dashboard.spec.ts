import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardView from '../views/DashboardView.vue'
import { statisticsApi } from '../api/statistics'
import { useAuthStore } from '../stores/auth'
import type { Dashboard } from '../types/domain'

vi.mock('../api/statistics', () => ({ statisticsApi: { dashboard: vi.fn() } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), error: vi.fn(), success: vi.fn() } }))

const dashboard: Dashboard = {
  from: '2026-09-01',
  to: '2026-09-02',
  totals: { income: '1000.00', expense: '300.00', balance: '700.00' },
  trend: [{ month: '2026-09', income: '1000.00', expense: '300.00' }],
  composition: [
    { categoryId: 1, categoryName: '餐饮', type: 'EXPENSE' as const, amount: '200.00' },
    { categoryId: 2, categoryName: '工资', type: 'INCOME' as const, amount: '1000.00' },
  ],
  members: [{ memberId: 1, memberNo: 'M001', memberName: '小林', income: '100.00', expense: '30.00', balance: '70.00' }],
  recentEntries: [],
}

async function mountView(data = dashboard) {
  vi.mocked(statisticsApi.dashboard).mockResolvedValue(data)
  const auth = useAuthStore()
  auth.setUser({ id: 1, username: 'parent', displayName: '小林', memberNo: 'M001', role: 'PARENT', status: 'ACTIVE', hasHousehold: true, household: { id: 1, name: '小林一家' } })
  const wrapper = mount(DashboardView, { global: { stubs: { TrendChart: { template: '<div />' } } } })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-09-02T12:00:00'))
})

afterEach(() => vi.useRealTimers())

describe('dashboard controls and comparisons', () => {
  it('loads the current month with local YYYY-MM-DD dates and exposes all ranges', async () => {
    const wrapper = await mountView()

    expect(wrapper.find('[data-range="month"]').exists()).toBe(true)
    expect(wrapper.find('[data-range="threeMonths"]').exists()).toBe(true)
    expect(wrapper.find('[data-range="year"]').exists()).toBe(true)
    expect(wrapper.find('[data-range="custom"]').exists()).toBe(true)
    expect(statisticsApi.dashboard).toHaveBeenLastCalledWith({ from: '2026-09-01', to: '2026-09-02' })
    expect(wrapper.text()).toContain('2026-09-01 至 2026-09-02')
  })

  it('refreshes the dashboard with the selected preset range', async () => {
    const wrapper = await mountView()

    await wrapper.get('[data-range="threeMonths"]').trigger('click')
    await flushPromises()
    expect(statisticsApi.dashboard).toHaveBeenLastCalledWith({ from: '2026-07-01', to: '2026-09-02' })
    expect(wrapper.get('[data-range="threeMonths"]').attributes('aria-pressed')).toBe('true')

    await wrapper.get('[data-range="year"]').trigger('click')
    await flushPromises()
    expect(statisticsApi.dashboard).toHaveBeenLastCalledWith({ from: '2026-01-01', to: '2026-09-02' })
  })

  it('applies a custom date range', async () => {
    const wrapper = await mountView()
    await wrapper.get('[data-range="custom"]').trigger('click')
    await wrapper.get('[data-test="custom-from"]').setValue('2026-02-03')
    await wrapper.get('[data-test="custom-to"]').setValue('2026-04-05')
    await wrapper.get('[data-test="apply-custom"]').trigger('click')
    await flushPromises()

    expect(statisticsApi.dashboard).toHaveBeenLastCalledWith({ from: '2026-02-03', to: '2026-04-05' })
    expect(wrapper.text()).toContain('2026-02-03 至 2026-04-05')
  })

  it('switches between expense and income composition without requesting again', async () => {
    const wrapper = await mountView()
    const requestCount = vi.mocked(statisticsApi.dashboard).mock.calls.length

    expect(wrapper.text()).toContain('支出构成')
    expect(wrapper.text()).toContain('餐饮')
    expect(wrapper.text()).not.toContain('工资')

    await wrapper.get('[data-composition="income"]').trigger('click')
    expect(vi.mocked(statisticsApi.dashboard).mock.calls).toHaveLength(requestCount)
    expect(wrapper.text()).toContain('收入构成')
    expect(wrapper.text()).toContain('工资')
    expect(wrapper.text()).not.toContain('NaN')
  })

  it('renders income, expense, and balance for each member', async () => {
    const wrapper = await mountView()

    expect(wrapper.text()).toContain('+¥100.00')
    expect(wrapper.text()).toContain('-¥30.00')
    expect(wrapper.text()).toContain('¥70.00')
  })

  it('renders ratio fields as percentages even when they exceed one', async () => {
    const wrapper = await mountView({
      ...dashboard,
      comparison: { income: 1500, expense: 450, balance: 1050, incomeChangeRate: 1.5, expenseChangeRate: 1.5, balanceChangeRate: 1.5 },
      savingsRate: 0.25,
      budget: { month: '2026-09', total: { budget: 200, spent: 300, remaining: -100, usageRate: 1.5, status: 'OVER' as const }, categories: [] },
    })

    expect(wrapper.text()).toContain('150.0%')
    expect(wrapper.text()).toContain('25.0% 储蓄率')
  })
})

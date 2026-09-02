import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ForbiddenView from '../views/ForbiddenView.vue'

const push = vi.fn()
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))

describe('forbidden page', () => {
  it('explains the permission error and provides dashboard navigation', async () => {
    const wrapper = mount(ForbiddenView)

    expect(wrapper.text()).toContain('403')
    expect(wrapper.text()).toContain('无权访问此功能')
    expect(wrapper.find('[data-test="back-dashboard"]').exists()).toBe(true)
    await wrapper.get('[data-test="back-dashboard"]').trigger('click')
    expect(push).toHaveBeenCalledWith('/dashboard')
  })
})

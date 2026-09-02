import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MembersView from '../views/MembersView.vue'
import { useAuthStore } from '../stores/auth'

vi.mock('../api/members', () => ({ memberApi: { list: vi.fn().mockResolvedValue([{ id: 1, memberNo: 'M001', displayName: '成员', role: 'MEMBER', status: 'ACTIVE', joinedAt: '' }]), update: vi.fn(), status: vi.fn() } }))

describe('role-based operation visibility', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('hides member edit operations for ordinary members', async () => {
    const auth = useAuthStore()
    auth.setUser({ id: 1, username: 'member', displayName: '成员', memberNo: 'M001', role: 'MEMBER', status: 'ACTIVE', hasHousehold: true, household: { id: 1, name: '家庭' } })
    const wrapper = mount(MembersView, { global: { stubs: { 'el-dialog': true, 'el-input': true, 'el-select': true, 'el-option': true } } })
    await new Promise((resolve) => setTimeout(resolve, 0))
    expect(wrapper.text()).not.toContain('编辑资料')
    expect(wrapper.text()).toContain('不能修改成员资料')
  })
})

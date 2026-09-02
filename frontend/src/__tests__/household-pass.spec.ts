import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MembersView from '../views/MembersView.vue'
import { useAuthStore } from '../stores/auth'
import { householdApi } from '../api/households'
import { ElMessage, ElMessageBox } from 'element-plus'

vi.mock('../api/members', () => ({
  memberApi: {
    list: vi.fn().mockResolvedValue([{ id: 1, memberNo: 'M001', displayName: '家长', role: 'PARENT', status: 'ACTIVE', joinedAt: '' }]),
    update: vi.fn(),
    status: vi.fn(),
  },
}))
vi.mock('../api/households', () => ({ householdApi: { rotateInviteCode: vi.fn() } }))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))

const parent = { id: 1, username: 'parent', displayName: '家长', memberNo: 'M001', role: 'PARENT' as const, status: 'ACTIVE' as const, hasHousehold: true, household: { id: 9, name: '小林一家' } }
const member = { ...parent, username: 'member', displayName: '普通成员', role: 'MEMBER' as const }
const household = { id: 9, name: '小林一家', inviteCode: 'ABCD2345', currentMemberId: 1, currentMemberNo: 'M001', currentRole: 'PARENT' as const }

function mountView() {
  return mount(MembersView, { global: { stubs: { 'el-dialog': true, 'el-input': true, 'el-select': true, 'el-option': true } } })
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  vi.mocked(ElMessageBox.confirm).mockResolvedValue({} as never)
})

describe('household pass', () => {
  it('shows the parent household name, invite code, and invite actions', async () => {
    const auth = useAuthStore()
    auth.setUser(parent)
    auth.setHousehold(household)
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('家庭通行证')
    expect(wrapper.text()).toContain('小林一家')
    expect(wrapper.text()).toContain('ABCD2345')
    expect(wrapper.find('[data-test="copy-invite"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="rotate-invite"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('新家人先注册账号')
  })

  it('hides invite details and invite actions from ordinary members', async () => {
    const auth = useAuthStore()
    auth.setUser(member)
    auth.setHousehold({ ...household, inviteCode: null, currentRole: 'MEMBER' })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('小林一家')
    expect(wrapper.text()).toContain('普通成员')
    expect(wrapper.text()).not.toContain('ABCD2345')
    expect(wrapper.find('[data-test="copy-invite"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="rotate-invite"]').exists()).toBe(false)
  })

  it('copies the invite code with a success message', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
    const auth = useAuthStore()
    auth.setUser(parent)
    auth.setHousehold(household)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="copy-invite"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith('ABCD2345')
    expect(ElMessage.success).toHaveBeenCalledWith('邀请码已复制')
  })

  it('rotates the invite code after confirmation and updates the store', async () => {
    vi.mocked(householdApi.rotateInviteCode).mockResolvedValue({ ...household, inviteCode: 'NEW23456' })
    const auth = useAuthStore()
    auth.setUser(parent)
    auth.setHousehold(household)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="rotate-invite"]').trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(expect.stringContaining('旧邀请码将立即失效'), '重置家庭邀请码？', expect.any(Object))
    expect(householdApi.rotateInviteCode).toHaveBeenCalledOnce()
    expect(auth.household?.inviteCode).toBe('NEW23456')
    expect(wrapper.text()).toContain('NEW23456')
  })

  it('keeps the old invite code when rotation fails', async () => {
    vi.mocked(householdApi.rotateInviteCode).mockRejectedValue(new Error('network'))
    const auth = useAuthStore()
    auth.setUser(parent)
    auth.setHousehold(household)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-test="rotate-invite"]').trigger('click')
    await flushPromises()

    expect(auth.household?.inviteCode).toBe('ABCD2345')
    expect(wrapper.text()).toContain('ABCD2345')
    expect(ElMessage.error).toHaveBeenCalledWith('邀请码重置失败，请稍后重试')
  })
})

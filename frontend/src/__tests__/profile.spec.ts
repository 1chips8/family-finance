import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProfileView from '../views/ProfileView.vue'
import { profileApi } from '../api/profile'
import { authApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const push = vi.fn()
vi.mock('../api/profile', () => ({ profileApi: { update: vi.fn(), password: vi.fn() } }))
vi.mock('../api/auth', () => ({ authApi: { csrf: vi.fn(), login: vi.fn(), register: vi.fn(), me: vi.fn(), logout: vi.fn().mockResolvedValue(undefined) } }))
vi.mock('../api/households', () => ({ householdApi: { current: vi.fn() } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }), useRoute: () => ({ query: {} }) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), error: vi.fn(), success: vi.fn() } }))

const user = { id: 1, username: 'parent', displayName: '小林', memberNo: 'M001', role: 'PARENT' as const, status: 'ACTIVE' as const, hasHousehold: true, household: { id: 1, name: '小林一家' } }

const inputStub = {
  props: ['modelValue', 'size'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

async function mountView() {
  const auth = useAuthStore()
  auth.setUser(user)
  auth.setHousehold({ id: 1, name: '小林一家', inviteCode: null, currentMemberId: 1, currentMemberNo: 'M001', currentRole: 'PARENT' })
  const wrapper = mount(ProfileView, { global: { stubs: { 'el-input': inputStub } } })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  vi.mocked(profileApi.password).mockResolvedValue(undefined)
})

describe('password update flow', () => {
  it('logs out and redirects to login after a successful password update', async () => {
    const wrapper = await mountView()
    await wrapper.get('#password-current').setValue('Old12345')
    await wrapper.get('#password-new').setValue('New12345')
    await wrapper.get('#password-confirm').setValue('New12345')

    await wrapper.findAll('form')[1]!.trigger('submit')
    await flushPromises()

    expect(profileApi.password).toHaveBeenCalledWith('Old12345', 'New12345')
    expect(authApi.logout).toHaveBeenCalledOnce()
    expect(useAuthStore().user).toBeNull()
    expect(useAuthStore().household).toBeNull()
    expect(push).toHaveBeenCalledWith({ path: '/login', query: { passwordChanged: '1' } })
    expect((wrapper.get('#password-current').element as HTMLInputElement).value).toBe('')
  })

  it('keeps the session and inputs when the password update fails', async () => {
    vi.mocked(profileApi.password).mockRejectedValue({ response: { data: { message: '当前密码不正确' } } })
    const wrapper = await mountView()
    await wrapper.get('#password-current').setValue('Wrong123')
    await wrapper.get('#password-new').setValue('New12345')
    await wrapper.get('#password-confirm').setValue('New12345')

    await wrapper.findAll('form')[1]!.trigger('submit')
    await flushPromises()

    expect(authApi.logout).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
    expect(useAuthStore().user?.username).toBe('parent')
    expect((wrapper.get('#password-current').element as HTMLInputElement).value).toBe('Wrong123')
  })
})

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import EntryForm from '../components/EntryForm.vue'
import type { Category, Member } from '../types/domain'

const members: Member[] = [{ id: 1, memberNo: 'M001', displayName: '家长', role: 'PARENT', status: 'ACTIVE', joinedAt: '' }]
const categories: Category[] = [
  { id: 1, scope: 'SYSTEM', type: 'EXPENSE', name: '餐饮', status: 'ACTIVE' },
  { id: 2, scope: 'SYSTEM', type: 'INCOME', name: '工资', status: 'ACTIVE' },
]

describe('EntryForm', () => {
  it('shows only categories matching the selected type', async () => {
    const wrapper = mount(EntryForm, { props: { members, categories, parent: true }, global: { stubs: { 'el-select': { template: '<select v-bind="$attrs"><slot /></select>' }, 'el-option': { props: ['label'], template: '<option>{{ label }}</option>' }, 'el-input': true, 'el-date-picker': true } } })
    const buttons = wrapper.findAll('.type-toggle button')
    expect(buttons).toHaveLength(2)
    await buttons[1]!.trigger('click')
    expect(wrapper.text()).toContain('工资')
    expect(wrapper.text()).not.toContain('餐饮')
  })
})

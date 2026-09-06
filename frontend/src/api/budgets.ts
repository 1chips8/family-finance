import type { ApiResponse } from '../types/api'
import type { BudgetOverview } from '../types/domain'
import { apiClient, writeConfig } from './client'

export const budgetApi = {
  async overview(month: string) {
    const response = await apiClient.get<ApiResponse<BudgetOverview>>('/budgets', { params: { month } })
    return response.data.data
  },
  async upsertTotal(month: string, amount: string) {
    const response = await apiClient.put<ApiResponse<BudgetOverview>>('/budgets/total', { month, amount }, writeConfig())
    return response.data.data
  },
  async upsertCategory(categoryId: number, month: string, amount: string) {
    const response = await apiClient.put<ApiResponse<BudgetOverview>>(`/budgets/categories/${categoryId}`, { month, amount }, writeConfig())
    return response.data.data
  },
  async removeTotal(month: string) { await apiClient.delete('/budgets/total', { ...writeConfig(), params: { month } }) },
  async removeCategory(categoryId: number, month: string) { await apiClient.delete(`/budgets/categories/${categoryId}`, { ...writeConfig(), params: { month } }) },
}

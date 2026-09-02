import type { ApiResponse } from '../types/api'
import type { Category, CategoryType } from '../types/domain'
import { apiClient, writeConfig } from './client'

export const categoryApi = {
  async list(includeInactive = true) {
    const response = await apiClient.get<ApiResponse<Category[]>>('/categories', { params: { includeInactive } })
    return response.data.data
  },
  async create(payload: { type: CategoryType; name: string }) {
    const response = await apiClient.post<ApiResponse<Category>>('/categories', payload, writeConfig())
    return response.data.data
  },
  async update(id: number, name: string) {
    const response = await apiClient.patch<ApiResponse<Category>>(`/categories/${id}`, { name }, writeConfig())
    return response.data.data
  },
  async status(id: number, active: boolean) {
    const response = await apiClient.patch<ApiResponse<Category>>(`/categories/${id}/status`, { active }, writeConfig())
    return response.data.data
  },
}

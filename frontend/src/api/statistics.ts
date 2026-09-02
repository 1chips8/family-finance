import type { ApiResponse } from '../types/api'
import type { Dashboard } from '../types/domain'
import { apiClient } from './client'

export const statisticsApi = {
  async dashboard(params?: { from?: string; to?: string }) {
    const response = await apiClient.get<ApiResponse<Dashboard>>('/statistics/dashboard', { params })
    return response.data.data
  },
}

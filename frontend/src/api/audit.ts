import type { ApiResponse } from '../types/api'
import type { AuditLogPage } from '../types/domain'
import { apiClient } from './client'

export const auditApi = {
  async list(params: { page?: number; pageSize?: number } = {}) {
    const response = await apiClient.get<ApiResponse<AuditLogPage>>('/audit-logs', { params })
    return response.data.data
  },
}

import type { ApiResponse } from '../types/api'
import type { Member, Role } from '../types/domain'
import { apiClient, writeConfig } from './client'

export const memberApi = {
  async list() {
    const response = await apiClient.get<ApiResponse<Member[]>>('/members')
    return response.data.data
  },
  async update(id: number, payload: { memberNo: string; displayName: string; role: Role }) {
    const response = await apiClient.patch<ApiResponse<Member>>(`/members/${id}`, payload, writeConfig())
    return response.data.data
  },
  async status(id: number, active: boolean) {
    const response = await apiClient.patch<ApiResponse<Member>>(`/members/${id}/status`, { active }, writeConfig())
    return response.data.data
  },
}

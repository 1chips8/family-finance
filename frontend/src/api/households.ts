import type { ApiResponse } from '../types/api'
import type { Household } from '../types/domain'
import { apiClient, writeConfig } from './client'

export const householdApi = {
  async create(name: string) {
    const response = await apiClient.post<ApiResponse<Household>>('/households', { name }, writeConfig())
    return response.data.data
  },
  async join(inviteCode: string) {
    const response = await apiClient.post<ApiResponse<Household>>('/households/join', { inviteCode }, writeConfig())
    return response.data.data
  },
  async current() {
    const response = await apiClient.get<ApiResponse<Household>>('/households/current')
    return response.data.data
  },
  async rotateInviteCode() {
    const response = await apiClient.post<ApiResponse<Household>>('/households/invite-code/rotate', undefined, writeConfig())
    return response.data.data
  },
}

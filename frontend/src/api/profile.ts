import type { ApiResponse } from '../types/api'
import type { User } from '../types/domain'
import { apiClient, writeConfig } from './client'

export const profileApi = {
  async update(displayName: string) {
    const response = await apiClient.patch<ApiResponse<User>>('/profile', { displayName }, writeConfig())
    return response.data.data
  },
  async password(currentPassword: string, newPassword: string) {
    await apiClient.put('/profile/password', { currentPassword, newPassword }, writeConfig())
  },
}

import type { ApiResponse } from '../types/api'
import type { User } from '../types/domain'
import { apiClient, setCsrfToken, writeConfig } from './client'

export interface RegisterPayload { username: string; displayName: string; password: string }
export interface LoginPayload { username: string; password: string }

export const authApi = {
  async csrf() {
    const response = await apiClient.get<ApiResponse<{ token: string }>>('/auth/csrf')
    setCsrfToken(response.data.data.token)
    return response.data.data.token
  },
  async register(payload: RegisterPayload) {
    await apiClient.post<ApiResponse<{ message: string }>>('/auth/register', payload, writeConfig())
  },
  async login(payload: LoginPayload) {
    const response = await apiClient.post<ApiResponse<User>>('/auth/login', payload, writeConfig())
    return response.data.data
  },
  async me() {
    const response = await apiClient.get<ApiResponse<User>>('/auth/me')
    return response.data.data
  },
  async logout() {
    await apiClient.post('/auth/logout', undefined, writeConfig())
  },
}

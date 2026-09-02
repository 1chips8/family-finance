import type { ApiResponse } from '../types/api'
import type { Entry, EntryPage, LedgerType } from '../types/domain'
import { apiClient, writeConfig } from './client'

export interface EntryPayload {
  memberId?: number
  type: LedgerType
  categoryId: number
  amount: string
  occurredOn: string
  note?: string
}

export const entryApi = {
  async list(params: Record<string, unknown>) {
    const response = await apiClient.get<ApiResponse<EntryPage>>('/entries', { params })
    return response.data.data
  },
  async create(payload: EntryPayload) {
    const response = await apiClient.post<ApiResponse<Entry>>('/entries', payload, writeConfig())
    return response.data.data
  },
  async update(id: number, payload: EntryPayload) {
    const response = await apiClient.put<ApiResponse<Entry>>(`/entries/${id}`, payload, writeConfig())
    return response.data.data
  },
  async remove(id: number) {
    await apiClient.delete(`/entries/${id}`, writeConfig())
  },
}

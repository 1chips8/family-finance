import type { ApiResponse } from '../types/api'
import type { LedgerType, RecurringGenerationResult, RecurringTemplate } from '../types/domain'
import { apiClient, writeConfig } from './client'

export interface RecurringPayload { memberId?: number; type: LedgerType; categoryId: number; amount: string; dayOfMonth: number; note?: string; active?: boolean }
export const recurringApi = {
  async list() { const response = await apiClient.get<ApiResponse<RecurringTemplate[]>>('/recurring-templates'); return response.data.data },
  async create(payload: RecurringPayload) { const response = await apiClient.post<ApiResponse<RecurringTemplate>>('/recurring-templates', payload, writeConfig()); return response.data.data },
  async update(id: number, payload: RecurringPayload) { const response = await apiClient.put<ApiResponse<RecurringTemplate>>(`/recurring-templates/${id}`, payload, writeConfig()); return response.data.data },
  async remove(id: number) { await apiClient.delete(`/recurring-templates/${id}`, writeConfig()) },
  async status(id: number, active: boolean) { const response = await apiClient.patch<ApiResponse<RecurringTemplate>>(`/recurring-templates/${id}/status`, { active }, writeConfig()); return response.data.data },
  async generate(month: string) { const response = await apiClient.post<ApiResponse<RecurringGenerationResult>>('/recurring-templates/generate', undefined, { ...writeConfig(), params: { month } }); return response.data.data },
}

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

export interface EntryExportParams extends Record<string, unknown> {
  page?: number
  pageSize?: number
}

export interface EntryImportRow {
  rowNumber: number
  occurredOn?: string
  type?: LedgerType
  categoryId?: number
  categoryName?: string
  memberId?: number
  memberNo?: string
  amount?: string
  note?: string
  errors: Record<string, string> | string[]
}
export interface EntryImportPreview {
  totalRows: number
  validRows: number
  errorRows: number
  checksum: string
  rows: EntryImportRow[]
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
  async exportCsv(params: EntryExportParams = {}) {
    const response = await apiClient.get<Blob>('/entries/export', { params, responseType: 'blob' })
    return response.data
  },
  async importPreview(csv: File | string) {
    const content = typeof csv === 'string' ? csv : await csv.text()
    const response = await apiClient.post<ApiResponse<EntryImportPreview>>('/entries/import/preview', { csv: content }, writeConfig())
    return response.data.data
  },
  async importCommit(csv: File | string, checksum = '') {
    const content = typeof csv === 'string' ? csv : await csv.text()
    const response = await apiClient.post<ApiResponse<{ importedRows: number }>>('/entries/import/commit', { csv: content, checksum }, writeConfig())
    return response.data.data
  },
}

export interface ApiResponse<T> {
  data: T
}

export interface ApiErrorResponse {
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

declare module 'axios' {
  const axios: {
    create(config?: Record<string, unknown>): {
      get<T = unknown>(url: string, config?: Record<string, unknown>): Promise<{ data: T }>
      post<T = unknown>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<{ data: T }>
      put<T = unknown>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<{ data: T }>
      patch<T = unknown>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<{ data: T }>
      delete<T = unknown>(url: string, config?: Record<string, unknown>): Promise<{ data: T }>
      interceptors: { request: { use: (...args: unknown[]) => void }; response: { use: (...args: unknown[]) => void } }
    }
  }
  export default axios
}

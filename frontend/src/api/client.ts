import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

export const FORBIDDEN_EVENT = 'family-finance:forbidden'

// API 层只广播权限失败，具体页面跳转由应用入口统一处理，避免请求模块依赖路由实例。
apiClient.interceptors.response.use(
  (response: unknown) => response,
  (error: unknown) => {
    if (errorStatus(error) === 403 && typeof window !== 'undefined') window.dispatchEvent(new Event(FORBIDDEN_EVENT))
    return Promise.reject(error)
  },
)

let csrfToken = ''

export function setCsrfToken(token: string) {
  csrfToken = token
}

export function writeConfig() {
  // 登录接口返回的 Token 优先；刷新页面后可从同源 Cookie 恢复。
  return { headers: { 'X-XSRF-TOKEN': csrfToken || readCookie('XSRF-TOKEN') } }
}

export function readCookie(name: string): string {
  const prefix = `${name}=`
  const item = document.cookie.split('; ').find((cookie) => cookie.startsWith(prefix))
  return item ? decodeURIComponent(item.slice(prefix.length)) : ''
}

export function errorMessage(error: unknown, fallback = '请求失败，请稍后重试') {
  const response = (error as { response?: { data?: { message?: string } } })?.response
  return response?.data?.message || fallback
}

export function errorStatus(error: unknown) {
  return (error as { response?: { status?: number } })?.response?.status
}

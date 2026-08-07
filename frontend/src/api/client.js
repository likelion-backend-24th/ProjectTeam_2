import axios from 'axios'

// 백엔드 공통 응답 포맷(ApiResponse<T>)
// { success: boolean, message: string, data: T, meta: { pagination?: {...} } | null, errorCode: string | null }

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // refreshToken(httpOnly 쿠키) 자동 전송을 위해 필요
})

// ---------------------------------------------------------------------------
// accessToken 저장소
// 백엔드는 accessToken을 응답 바디로 내려주고(쿠키 아님), refreshToken만 httpOnly
// 쿠키로 관리한다. accessToken은 새로고침 후에도 로그인 상태를 유지하기 위해
// localStorage에 함께 보관한다.
// ---------------------------------------------------------------------------
const ACCESS_TOKEN_KEY = 'accessToken'
let accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)

export function getAccessToken() {
  return accessToken
}

export function setAccessToken(token) {
  accessToken = token
  if (token) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
  } else {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  }
}

// ---------------------------------------------------------------------------
// 요청 인터셉터: accessToken을 Authorization 헤더에 부착
// ---------------------------------------------------------------------------
apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// ---------------------------------------------------------------------------
// 응답 인터셉터: 401 발생 시 /api/auth/reissue로 accessToken을 재발급받아
// 실패했던 요청을 한 번 재시도한다. 동시에 여러 요청이 401을 맞아도
// reissue는 한 번만 호출되도록 큐잉한다.
// ---------------------------------------------------------------------------
let isRefreshing = false
let pendingQueue = []

function resolveQueue(newToken) {
  pendingQueue.forEach(({ resolve, reject, config }) => {
    if (!newToken) {
      reject(new Error('토큰 재발급에 실패했습니다.'))
      return
    }
    config.headers.Authorization = `Bearer ${newToken}`
    resolve(apiClient(config))
  })
  pendingQueue = []
}

const AUTH_FREE_PATHS = ['/api/auth/login', '/api/auth/signup', '/api/auth/reissue']

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error

    const isAuthFreeRequest = AUTH_FREE_PATHS.some((path) => config?.url?.includes(path))

    if (!response || response.status !== 401 || isAuthFreeRequest || config._retry) {
      return Promise.reject(error)
    }

    config._retry = true

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        pendingQueue.push({ resolve, reject, config })
      })
    }

    isRefreshing = true
    try {
      const { data } = await apiClient.post('/api/auth/reissue')
      const newAccessToken = data.data.accessToken
      setAccessToken(newAccessToken)
      resolveQueue(newAccessToken)
      config.headers.Authorization = `Bearer ${newAccessToken}`
      return apiClient(config)
    } catch (refreshError) {
      setAccessToken(null)
      resolveQueue(null)
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

export default apiClient

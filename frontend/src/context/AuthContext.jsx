import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authApi, getAccessToken, userApi } from '../api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  // 앱 최초 진입 시 accessToken이 남아 있으면(localStorage) 내 정보를 조회해
  // 로그인 상태를 복원한다. 토큰이 만료됐다면 client.js의 응답 인터셉터가
  // 자동으로 reissue를 시도한다.
  const loadMe = useCallback(async () => {
    if (!getAccessToken()) {
      setUser(null)
      setIsLoading(false)
      return
    }
    try {
      const { data } = await userApi.getMe()
      setUser(data.data)
    } catch {
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadMe()
  }, [loadMe])

  const login = useCallback(
    async (payload) => {
      await authApi.login(payload)
      await loadMe()
    },
    [loadMe],
  )

  const logout = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  const kakaoLogin = useCallback(
  async (kakaoAccessToken) => {
    await authApi.kakaoLogin(kakaoAccessToken)
    await loadMe()
  },
  [loadMe],
)

  const value = useMemo(
    () => ({
      user,
      isLoading,
      isAuthenticated: Boolean(user),
      login,
      kakaoLogin,
      logout,
      refetchMe: loadMe,
    }),
    [user, isLoading, login, kakaoLogin, logout, loadMe],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.')
  }
  return ctx
}

import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function NaverCallbackPage() {
  const navigate = useNavigate()
  const { naverLogin } = useAuth()
  const hasRun = useRef(false)

  useEffect(() => {
    if (hasRun.current) return
    hasRun.current = true

    if (!window.naver) {
      navigate('/login', { replace: true })
      return
    }

    const naverLoginInstance = new window.naver.LoginWithNaverId({
      clientId: import.meta.env.VITE_NAVER_CLIENT_ID,
      callbackUrl: import.meta.env.VITE_NAVER_REDIRECT_URI,
      isPopup: false,
      callbackHandle: true,
      loginButton: { color: 'green', type: 3, height: 60 },
    })

    naverLoginInstance.init()

    naverLoginInstance.getLoginStatus(async (status) => {
      if (!status) {
        console.error('네이버 로그인 실패')
        navigate('/login', { replace: true })
        return
      }

      try {
        const naverAccessToken = naverLoginInstance.accessToken.accessToken
        await naverLogin(naverAccessToken)
        navigate('/', { replace: true })
      } catch (err) {
        const message = err.response?.data?.message ?? '네이버 로그인에 실패했습니다.'
        console.error('네이버 로그인 처리 실패', err)
        window.alert(message)
        navigate('/login', { replace: true })
      }
    })
  }, [navigate, naverLogin])

  return (
    <>
      <div id="naverIdLogin" style={{ display: 'none' }} />
      <p>네이버 로그인 처리 중입니다...</p>
    </>
)
}
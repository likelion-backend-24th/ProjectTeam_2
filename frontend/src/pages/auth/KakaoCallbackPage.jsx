import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function KakaoCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { kakaoLogin } = useAuth()
  const hasRun = useRef(false)

  useEffect(() => {
    if (hasRun.current) return
    hasRun.current = true

    const code = searchParams.get('code')

    if (!code) {
      navigate('/login', { replace: true })
      return
    }

    async function exchangeCodeAndLogin() {
      try {
        const tokenResponse = await fetch('https://kauth.kakao.com/oauth/token', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: import.meta.env.VITE_KAKAO_REST_API_KEY,
            redirect_uri: import.meta.env.VITE_KAKAO_REDIRECT_URI,
            code,
          }),
        })

        const tokenData = await tokenResponse.json()

        if (!tokenData.access_token) {
          throw new Error('카카오 액세스 토큰 발급 실패')
        }

        await kakaoLogin(tokenData.access_token)
        navigate('/', { replace: true })
      } catch (err) {
        const message = err.response?.data?.message ?? '카카오 로그인에 실패했습니다.'
        console.error('카카오 로그인 처리 실패', err)
        window.alert(message)
        navigate('/login', { replace: true })
      }
    }

    exchangeCodeAndLogin()
  }, [searchParams, navigate, kakaoLogin])

  return <p>카카오 로그인 처리 중입니다...</p>
}
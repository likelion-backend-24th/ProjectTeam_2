import { MessageCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { GoogleIcon, NaverIcon } from './SocialIcons'
import { useAuth } from '../../context/AuthContext'
import styles from './SocialLoginButtons.module.css'

const LABELS = {
  login: { kakao: '카카오 로그인', google: 'Google 로그인', naver: '네이버 로그인' },
  signup: { kakao: '카카오로 가입하기', google: 'Google로 가입하기', naver: '네이버로 가입하기' },
}

// 백엔드는 카카오/구글/네이버 3사 소셜 로그인을 모두 지원한다
// (authApi.kakaoLogin/googleLogin/naverLogin). 실제 동작을 위해서는
// 각 사 JS SDK로 access token을 발급받아 해당 authApi 함수에 넘겨야 하는데,
// SDK 앱 키·리다이렉트 URI 설정이 필요해서 지금은 버튼만 구현하고 연동은 TODO로 남겨둔다.
export default function SocialLoginButtons({ mode }) {
  const labels = LABELS[mode]
  const navigate = useNavigate() 
  const { kakaoLogin } = useAuth()

  function handleKakao() {
    // TODO: Kakao SDK 로그인 -> authApi.kakaoLogin(kakaoAccessToken)
    window.Kakao.Auth.authorize({
      redirectUri: import.meta.env.VITE_KAKAO_REDIRECT_URI,
    })
  }

  function handleGoogle() {
    // TODO: Google Identity Services -> authApi.googleLogin(googleAccessToken)
  }

  function handleNaver() {
    // TODO: 네이버 로그인 SDK -> authApi.naverLogin(naverAccessToken)
  }

  return (
    <>
      <div className={styles.divider}>
        <span />
        또는
        <span />
      </div>

      <div className={styles.buttons}>
        <button type="button" className={`${styles.button} ${styles.kakao}`} onClick={handleKakao}>
          <MessageCircle size={18} />
          {labels.kakao}
        </button>
        <button type="button" className={`${styles.button} ${styles.google}`} onClick={handleGoogle}>
          <GoogleIcon size={18} />
          {labels.google}
        </button>
        <button type="button" className={`${styles.button} ${styles.naver}`} onClick={handleNaver}>
          <NaverIcon size={18} />
          {labels.naver}
        </button>
      </div>
    </>
  )
}

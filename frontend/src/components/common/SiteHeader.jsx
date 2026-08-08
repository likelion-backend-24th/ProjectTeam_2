import { LogIn, LogOut, UserPlus } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './SiteHeader.module.css'

const NAV_ITEMS = [
  { label: '게시글', to: '/posts' },
  { label: '스터디', to: '/studies' },
  { label: '전문가 상담', to: null },
  { label: '구독 플랜', to: null },
]

// TODO: 전문가 상담/구독 플랜 페이지가 생기면 실제 경로 연결
// 전문가 상담 -> /experts, 구독 플랜 -> /subscription
export default function SiteHeader() {
  const { user, isAuthenticated, isLoading, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/')
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <div className={styles.left}>
          <Link to="/" className={styles.logo}>
            <span className={styles.logoMark}>P</span>
            <span className={styles.logoText}>prep2gether</span>
          </Link>
        </div>

        <nav className={styles.nav}>
          {NAV_ITEMS.map((item) =>
            item.to ? (
              <Link key={item.label} to={item.to} className={styles.navLink}>
                {item.label}
              </Link>
            ) : (
              <a key={item.label} href="#" className={styles.navLink}>
                {item.label}
              </a>
            ),
          )}
        </nav>

        <div className={styles.actions}>
          {/* 로그인 상태 복원(getMe) 중에는 아무것도 그리지 않아 로그인/회원가입 -> 닉네임으로
              바뀌는 깜빡임을 막는다. */}
          {!isLoading &&
            (isAuthenticated ? (
              <>
                <Link to="/mypage" className={styles.userGreeting}>
                  {user.nickname}님
                </Link>
                <button type="button" className={styles.logoutButton} onClick={handleLogout}>
                  <LogOut size={16} />
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className={styles.loginLink}>
                  <LogIn size={16} />
                  로그인
                </Link>
                <Link to="/signup" className={styles.signupButton}>
                  <UserPlus size={16} />
                  무료 가입
                </Link>
              </>
            ))}
        </div>
      </div>
    </header>
  )
}

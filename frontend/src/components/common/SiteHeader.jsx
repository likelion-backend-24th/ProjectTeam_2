import { ChevronLeft, LogIn, UserPlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import styles from './SiteHeader.module.css'

const NAV_ITEMS = [
  { label: '게시글', to: '/posts' },
  { label: '스터디', to: null },
  { label: '전문가 상담', to: null },
  { label: '구독 플랜', to: null },
]

// TODO: 스터디/전문가 상담/구독 플랜 페이지가 생기면 실제 경로 연결
// 스터디 -> /studies, 전문가 상담 -> /experts, 구독 플랜 -> /subscription
export default function SiteHeader({ backTo }) {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <div className={styles.left}>
          {backTo && (
            <Link to={backTo} className={styles.backLink}>
              <ChevronLeft size={16} />홈
            </Link>
          )}

          <Link to="/" className={styles.logo}>
            <span className={styles.logoMark}>취</span>
            <span className={styles.logoText}>JOBtogether</span>
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
          <Link to="/login" className={styles.loginLink}>
            <LogIn size={16} />
            로그인
          </Link>
          <Link to="/signup" className={styles.signupButton}>
            <UserPlus size={16} />
            무료 가입
          </Link>
        </div>
      </div>
    </header>
  )
}

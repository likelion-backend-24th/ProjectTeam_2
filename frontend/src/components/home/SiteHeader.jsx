import { LogIn, UserPlus } from 'lucide-react'
import styles from './SiteHeader.module.css'

const NAV_ITEMS = ['게시글', '스터디', '전문가 상담', '구독 플랜']

// TODO: react-router 도입 후 각 항목/버튼에 실제 경로 연결
// 게시글 -> /posts, 스터디 -> /studies, 전문가 상담 -> /experts, 구독 플랜 -> /subscription
// 로그인 -> /login, 무료 가입 -> /signup
export default function SiteHeader() {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <a href="#" className={styles.logo}>
          <span className={styles.logoMark}>취</span>
          <span className={styles.logoText}>JOBtogether</span>
        </a>

        <nav className={styles.nav}>
          {NAV_ITEMS.map((item) => (
            <a key={item} href="#" className={styles.navLink}>
              {item}
            </a>
          ))}
        </nav>

        <div className={styles.actions}>
          <a href="#" className={styles.loginLink}>
            <LogIn size={16} />
            로그인
          </a>
          <a href="#" className={styles.signupButton}>
            <UserPlus size={16} />
            무료 가입
          </a>
        </div>
      </div>
    </header>
  )
}

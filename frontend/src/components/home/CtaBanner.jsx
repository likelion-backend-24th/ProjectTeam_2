import { ArrowRight, UserPlus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './CtaBanner.module.css'
export default function CtaBanner() {
  const { isAuthenticated } = useAuth()

  // 가입·로그인 유도 배너라 로그인 후에는 노출하지 않는다.
  if (isAuthenticated) return null

  return (
    <section className={styles.section}>
      <div className={styles.banner}>
        <div className={styles.blobOne} aria-hidden="true" />
        <div className={styles.blobTwo} aria-hidden="true" />

        <div className={styles.content}>
          <p className={styles.eyebrow}>GET STARTED</p>
          <h2 className={styles.title}>
            오늘 시작하면
            <br />
            내일이 달라집니다.
          </h2>
          <p className={styles.subtext}>38,200명의 취준생이 함께 성장하고 있습니다.</p>
        </div>

        <div className={styles.buttons}>
          <Link to="/signup" className={styles.primaryButton}>
            <UserPlus size={18} />
            무료로 가입하기
          </Link>
          <Link to="/login" className={styles.secondaryButton}>
            <ArrowRight size={18} />
            이미 계정이 있어요
          </Link>
        </div>
      </div>
    </section>
  )
}

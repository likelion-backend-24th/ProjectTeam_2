import { ArrowRight, UserPlus } from 'lucide-react'
import styles from './CtaBanner.module.css'

// TODO: 무료로 가입하기 -> /signup, 이미 계정이 있어요 -> /login
export default function CtaBanner() {
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
          <a href="#" className={styles.primaryButton}>
            <UserPlus size={18} />
            무료로 가입하기
          </a>
          <a href="#" className={styles.secondaryButton}>
            <ArrowRight size={18} />
            이미 계정이 있어요
          </a>
        </div>
      </div>
    </section>
  )
}

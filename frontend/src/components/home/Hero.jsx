import { ArrowRight, ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import styles from './Hero.module.css'

// TODO: 게시글 둘러보기 -> /posts (게시판 페이지 생기면 연결)
export default function Hero() {
  return (
    <section className={styles.hero}>
      <div className={styles.glow} aria-hidden="true" />

      <div className={styles.inner}>
        <p className={styles.eyebrow}>
          <span className={styles.eyebrowLine} />
          취준생 커뮤니티 플랫폼
        </p>

        <h1 className={styles.headline}>
          합격의 길,
          <br />
          <span className={styles.accent}>혼자</span> 걷지
          <br />
          마세요.
        </h1>

        <div className={styles.bottomRow}>
          <p className={styles.subtext}>
            게시글 · 스터디 · 전문가 상담까지,
            <br />
            취업 준비의 모든 과정을 함께합니다.
          </p>

          <div className={styles.ctaGroup}>
            <Link to="/signup" className={styles.primaryButton}>
              무료로 시작하기
              <ArrowRight size={18} />
            </Link>
            <a href="#" className={styles.secondaryButton}>
              게시글 둘러보기
              <ChevronRight size={18} />
            </a>
          </div>
        </div>

        <div className={styles.scrollHint} aria-hidden="true">
          <span className={styles.mouse}>
            <span className={styles.mouseWheel} />
          </span>
        </div>
      </div>
    </section>
  )
}

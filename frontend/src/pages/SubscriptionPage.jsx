import { Link } from 'react-router-dom'
import SiteHeader from '../components/common/SiteHeader'
import styles from './SubscriptionPage.module.css'

const FREE_FEATURES = ['게시글 조회·작성·댓글', '스터디 신청·참여 (최대 2개)', '커뮤니티 모든 기본 기능']

const PREMIUM_FEATURES = [
  '전문가 1:1 상담 무제한',
  '스터디 개설·참여 무제한',
  '합격자 자소서 자료 열람',
  '구독자 전용 스터디 참여',
  '기업 인사이트 리포트',
]

export default function SubscriptionPage() {
  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <p className={styles.eyebrow}>PRICING</p>
        <h1 className={styles.title}>합리적인 요금제</h1>
        <p className={styles.subtitle}>기본 커뮤니티는 항상 무료. 더 빠른 합격을 원한다면 프리미엄을 선택하세요.</p>

        <div className={styles.plans}>
          <section className={styles.card}>
            <p className={styles.planLabel}>FREE</p>
            <p className={styles.price}>무료</p>

            <ul className={styles.featureList}>
              {FREE_FEATURES.map((feature) => (
                <li key={feature} className={styles.featureItem}>
                  <span className={styles.bullet} />
                  {feature}
                </li>
              ))}
            </ul>

            <Link to="/" className={styles.freeButton}>
              무료 시작
            </Link>
          </section>

          <section className={`${styles.card} ${styles.premiumCard}`}>
            <span className={styles.recommendedBadge}>추천 플랜</span>
            <p className={styles.planLabel}>PREMIUM</p>
            <p className={styles.price}>
              9,900원<span className={styles.priceUnit}> / 월</span>
            </p>

            <ul className={styles.featureList}>
              {PREMIUM_FEATURES.map((feature) => (
                <li key={feature} className={styles.featureItem}>
                  <span className={styles.bulletPremium} />
                  {feature}
                </li>
              ))}
            </ul>

            <button
              type="button"
              className={styles.premiumButton}
              onClick={() => window.alert('구독 결제는 아직 준비 중이에요.')}
            >
              지금 구독
            </button>
          </section>
        </div>
      </main>
    </>
  )
}

import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { subscriptionApi } from '../api'
import SiteHeader from '../components/common/SiteHeader'
import PaymentModal from '../components/subscription/PaymentModal'
import { useAuth } from '../context/AuthContext'
import { formatDate } from '../utils/formatDate'
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
  const { isAuthenticated, refetchMe } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  // subscription: undefined(조회 전) | null(구독 없음) | { status, startedAt, expiredAt }
  const [subscription, setSubscription] = useState(undefined)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isCancelling, setIsCancelling] = useState(false)
  const [cancelError, setCancelError] = useState('')

  useEffect(() => {
    if (!isAuthenticated) {
      setSubscription(null)
      return
    }
    let ignore = false

    subscriptionApi
      .getMy()
      .then(({ data }) => {
        if (!ignore) setSubscription(data.data)
      })
      .catch(() => {
        if (ignore) return
        // 구독 내역이 없으면 404(SUBSCRIPTION_NOT_FOUND) — 정상적인 "무료 회원" 상태라 에러로 취급하지 않는다.
        // 그 밖의 예기치 못한 실패도 일단 "무료 회원"으로 보여주고, 실제 구독 중이었다면
        // 구독 시도 시 서버가 SUBSCRIPTION_ALREADY_ACTIVE로 알려준다.
        setSubscription(null)
      })

    return () => {
      ignore = true
    }
  }, [isAuthenticated])

  function handleSubscribeClick() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    setIsModalOpen(true)
  }

  async function handleSubscribed(newSubscription) {
    setSubscription(newSubscription)
    setIsModalOpen(false)
    await refetchMe()
  }

  async function handleCancel() {
    if (!window.confirm('구독을 해지할까요? 해지하면 프리미엄 기능을 바로 이용할 수 없어요.')) return
    setCancelError('')
    setIsCancelling(true)
    try {
      await subscriptionApi.cancel()
      setSubscription(null)
      await refetchMe()
    } catch (err) {
      setCancelError(err.response?.data?.message ?? '구독 해지에 실패했습니다.')
    } finally {
      setIsCancelling(false)
    }
  }

  const isSubscribed = Boolean(subscription)

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

            {isSubscribed ? (
              <div className={styles.activeBox}>
                <p className={styles.activeLabel}>✓ 구독 중이에요</p>
                {subscription?.expiredAt && (
                  <p className={styles.activeExpiry}>다음 결제일 {formatDate(subscription.expiredAt)}</p>
                )}
                {cancelError && <p className={styles.cancelError}>{cancelError}</p>}
                <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isCancelling}>
                  {isCancelling ? '해지 처리 중...' : '구독 해지'}
                </button>
              </div>
            ) : (
              <button type="button" className={styles.premiumButton} onClick={handleSubscribeClick}>
                지금 구독
              </button>
            )}
          </section>
        </div>
      </main>

      {isModalOpen && <PaymentModal onClose={() => setIsModalOpen(false)} onSubscribed={handleSubscribed} />}
    </>
  )
}

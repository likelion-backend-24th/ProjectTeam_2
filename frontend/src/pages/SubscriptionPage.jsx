import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { paymentApi, subscriptionApi } from '../api'
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

  // subscription: undefined(조회 전) | null(이용 중인 구독 없음) | { status, startedAt, expiredAt, autoRenew }
  const [subscription, setSubscription] = useState(undefined)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isActionLoading, setIsActionLoading] = useState(false)
  const [actionError, setActionError] = useState('')

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
        // 이용 중인 구독이 없으면 404(SUBSCRIPTION_NOT_FOUND) — 정상적인 "무료 회원" 상태라 에러로 취급하지 않는다.
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
    setActionError('')
    setIsModalOpen(true)
  }

  async function handleSubscribed(newSubscription) {
    setSubscription(newSubscription)
    setIsModalOpen(false)
    await refetchMe()
  }

  async function runAction(action, confirmMessage) {
    if (confirmMessage && !window.confirm(confirmMessage)) return
    setActionError('')
    setIsActionLoading(true)
    try {
      const { data } = await action()
      setSubscription(data.data)
      await refetchMe()
    } catch (err) {
      setActionError(err.response?.data?.message ?? '요청 처리에 실패했어요.')
    } finally {
      setIsActionLoading(false)
    }
  }

  const handleCancel = () =>
    runAction(subscriptionApi.cancel, '다음 회차부터 자동 갱신을 멈출까요? 만료일까지는 계속 이용할 수 있어요.')
  const handleResume = () => runAction(subscriptionApi.resume)
  const handleRetry = () => runAction(paymentApi.retrySubscriptionPayment)

  const isSubscribed = Boolean(subscription)
  const isPastDue = subscription?.status === 'PAST_DUE'

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
              <div className={isPastDue ? `${styles.activeBox} ${styles.pastDueBox}` : styles.activeBox}>
                {isPastDue ? (
                  <>
                    <p className={styles.pastDueLabel}>⚠ 결제에 실패했어요</p>
                    <p className={styles.activeExpiry}>등록된 카드로 자동으로 다시 시도돼요. 지금 바로 재시도할 수도 있어요.</p>
                  </>
                ) : subscription.autoRenew ? (
                  <p className={styles.activeLabel}>✓ 구독 중이에요</p>
                ) : (
                  <p className={styles.activeLabel}>해지 예약됨</p>
                )}

                {subscription?.expiredAt && (
                  <p className={styles.activeExpiry}>
                    {isPastDue ? '만료 예정일 ' : subscription.autoRenew ? '다음 결제일 ' : '이용 종료 예정일 '}
                    {formatDate(subscription.expiredAt)}
                  </p>
                )}

                {actionError && <p className={styles.cancelError}>{actionError}</p>}

                {isPastDue ? (
                  <button type="button" className={styles.cancelButton} onClick={handleRetry} disabled={isActionLoading}>
                    {isActionLoading ? '처리 중...' : '지금 다시 결제'}
                  </button>
                ) : subscription.autoRenew ? (
                  <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isActionLoading}>
                    {isActionLoading ? '처리 중...' : '구독 해지'}
                  </button>
                ) : (
                  <button type="button" className={styles.cancelButton} onClick={handleResume} disabled={isActionLoading}>
                    {isActionLoading ? '처리 중...' : '구독 재개'}
                  </button>
                )}
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

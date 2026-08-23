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

  // subscription: undefined(조회 전) | null(구독 없음) | { status, startedAt, expiredAt, autoRenew }
  const [subscription, setSubscription] = useState(undefined)
  const [modalMode, setModalMode] = useState(null) // null(닫힘) | 'subscribe' | 'resume'
  const [isCancelling, setIsCancelling] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [isRetrying, setIsRetrying] = useState(false)
  const [retryError, setRetryError] = useState('')

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
    setModalMode('subscribe')
  }

  // 해지 예약 취소(자동갱신 재개). 로그인 안 된 상태로는 애초에 이 버튼이 보일 일이 없어서
  // handleSubscribeClick과 달리 인증 체크는 안 함.
  function handleResumeClick() {
    setModalMode('resume')
  }

  async function handleSubscribed(newSubscription) {
    setSubscription(newSubscription)
    setModalMode(null)
    await refetchMe()
  }

  // 유예기간(PAST_DUE) 중 스케줄러(최대 하루 1회)를 기다리지 않고 등록된 카드로 지금 바로 재시도.
  // 서버는 실패해도 에러가 아니라 200으로 응답하므로(재시도 기회가 남아있는 정상 케이스), catch가
  // 아니라 응답의 status로 성공/실패를 판단해야 한다.
  async function handleRetryNow() {
    setRetryError('')
    setIsRetrying(true)
    try {
      const { data } = await subscriptionApi.retryPastDueNow()
      setSubscription(data.data)
      if (data.data.status === 'ACTIVE') {
        await refetchMe() // 복구됐으니 헤더 등 상위 화면의 구독 상태도 같이 갱신
      } else {
        setRetryError('다시 시도했지만 결제에 실패했어요. 카드 상태를 확인해주세요.')
      }
    } catch (err) {
      setRetryError(err.response?.data?.message ?? err.message ?? '재시도에 실패했어요.')
    } finally {
      setIsRetrying(false)
    }
  }

  // 해지 = "다음 자동갱신 예약 취소". 즉시 끊기는 게 아니라 이미 결제한 기간까지는 그대로 이용 가능.
  async function handleCancel() {
    if (!window.confirm('구독을 해지할까요? 이미 결제한 기간까지는 계속 이용할 수 있어요.')) return

    setCancelError('')
    setIsCancelling(true)
    try {
      const { data } = await subscriptionApi.cancel()
      setSubscription(data.data)
      await refetchMe()
    } catch (err) {
      setCancelError(err.response?.data?.message ?? err.message ?? '구독 해지에 실패했어요.')
    } finally {
      setIsCancelling(false)
    }
  }

  // status: undefined(조회 전) | null(구독 없음) | 'ACTIVE'(정상 이용 중) | 'PAST_DUE'(결제 실패, 유예기간 중 - 접근 권한은 이미 끊김)
  const isPastDue = subscription?.status === 'PAST_DUE'
  const isSubscribed = Boolean(subscription) && !isPastDue

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

            {isPastDue ? (
              <div className={styles.pastDueBox}>
                <p className={styles.pastDueLabel}>⚠ 결제에 실패했어요</p>
                <p className={styles.activeExpiry}>
                  {subscription.autoRenew
                    ? subscription.graceEndsAt
                      ? `${formatDate(subscription.graceEndsAt)}까지 등록된 카드로 매일 자동 재시도돼요. 지금 바로 다시 시도할 수도 있어요.`
                      : '등록된 카드로 자동 재시도돼요. 지금 바로 다시 시도할 수도 있어요.'
                    : '이용 권한이 중단됐어요. 다시 구독하시려면 새로 결제해주세요.'}
                </p>
                {cancelError && <p className={styles.cancelError}>{cancelError}</p>}
                {retryError && <p className={styles.cancelError}>{retryError}</p>}
                {subscription.autoRenew ? (
                  <>
                    {/* 스케줄러(최대 하루 1회)를 안 기다리고 등록된 카드로 바로 재시도 */}
                    <button
                      type="button"
                      className={styles.premiumButton}
                      onClick={handleRetryNow}
                      disabled={isRetrying || isCancelling}
                    >
                      {isRetrying ? '재시도 중...' : '지금 재시도'}
                    </button>
                    <button
                      type="button"
                      className={styles.cancelButton}
                      onClick={handleCancel}
                      disabled={isCancelling || isRetrying}
                    >
                      {isCancelling ? '처리 중...' : '재시도 중단(구독 해지)'}
                    </button>
                  </>
                ) : (
                  // 이미 재시도용 결제수단도 없는 상태(직접 해지했거나 카드가 지워짐) -> 새로 구독해야 함.
                  // 완료 API는 PAST_DUE 구독을 자동으로 정리하고 새로 시작해준다(서버 PaymentService.completePayment 참고).
                  <button type="button" className={styles.premiumButton} onClick={handleSubscribeClick}>
                    지금 다시 구독
                  </button>
                )}
              </div>
            ) : isSubscribed ? (
              <div className={styles.activeBox}>
                <p className={styles.activeLabel}>{subscription.autoRenew ? '✓ 구독 중이에요' : '해지 예약됨'}</p>
                {subscription?.expiredAt && (
                  <p className={styles.activeExpiry}>
                    {subscription.autoRenew
                      ? `${formatDate(subscription.expiredAt)}에 자동 갱신돼요`
                      : `${formatDate(subscription.expiredAt)}까지 이용 가능 (이후 자동 갱신 안 됨)`}
                  </p>
                )}
                {cancelError && <p className={styles.cancelError}>{cancelError}</p>}
                {subscription.autoRenew ? (
                  <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isCancelling}>
                    {isCancelling ? '처리 중...' : '구독 해지'}
                  </button>
                ) : (
                  // 해지 예약된 상태(빌링키 없음) - 이미 결제한 기간은 그대로 살아있으니 새로 결제할
                  // 필요 없이 카드만 다시 등록하면 됨(PaymentModal mode="resume" 참고).
                  <button type="button" className={styles.resumeButton} onClick={handleResumeClick}>
                    해지 예약 취소
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

      {modalMode && (
        <PaymentModal mode={modalMode} onClose={() => setModalMode(null)} onSubscribed={handleSubscribed} />
      )}
    </>
  )
}

import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { billingKeyApi, paymentApi, subscriptionApi } from '../api'
import SiteHeader from '../components/common/SiteHeader'
import { useAuth } from '../context/AuthContext'
import { formatDate } from '../utils/formatDate'
import styles from './SubscriptionPage.module.css'
import * as PortOne from '@portone/browser-sdk/v2'

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
  const [isCancelling, setIsCancelling] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [isSubscribing, setIsSubscribing] = useState(false)
  const [subscribeError, setSubscribeError] = useState('')
  const [schedule, setSchedule] = useState(null)

  useEffect(() => {
    if (!isAuthenticated) {
      setSubscription(null)
      setSchedule(null)
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
        setSubscription(null)
      })

       paymentApi
    .getSchedule()
    .then(({ data }) => {
      if (!ignore) setSchedule(data.data)
    })
    .catch(() => {
      if (ignore) return
      setSchedule(null)
    })

    return () => {
      ignore = true
    }
  }, [isAuthenticated])

  // "지금 구독" 클릭: 빌링키 있으면 결제 진행, 없으면 카드 등록부터 진행
  async function handleSubscribeClick() {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }

    setSubscribeError('')
    setIsSubscribing(true)

    try {
      await paymentApi.subscribe()
      const { data } = await subscriptionApi.getMy()
      setSubscription(data.data)
      const { data: scheduleData } = await paymentApi.getSchedule()
      setSchedule(scheduleData.data)
      await refetchMe()
      alert('구독이 시작됐어요!')
    } catch (err) {
      const errorCode = err.response?.data?.errorCode

      if (errorCode === 'BILLING_KEY_NOT_FOUND') {
        await handleRegisterBillingKey()
        return
      }

      setSubscribeError(err.response?.data?.message ?? '구독에 실패했습니다.')
    } finally {
      setIsSubscribing(false)
    }
  }

  // 빌링키(카드) 등록만 진행 (결제는 안 함, 등록 후 사용자가 구독 버튼을 다시 눌러야 함)
  async function handleRegisterBillingKey() {
    try {
      const { data: prepareRes } = await billingKeyApi.prepare()
      const { storeId, channelKey, issueId } = prepareRes.data

      const issueResponse = await PortOne.requestIssueBillingKey({
        storeId,
        channelKey,
        billingKeyMethod: 'CARD',
        issueId,
        issueName: 'prep2gether 정기결제 카드 등록',
      })

      if (issueResponse.code !== undefined) {
        setSubscribeError('카드 등록 실패: ' + issueResponse.message)
        return
      }

      await billingKeyApi.verify(issueId, issueResponse.billingKey)
      alert('카드 등록이 완료됐어요. 구독 버튼을 다시 눌러주세요.')
    } catch (err) {
      setSubscribeError(err.response?.data?.message ?? '카드 등록에 실패했습니다.')
    } finally {
      setIsSubscribing(false)
    }
  }

  async function handleCancel() {
  if (!window.confirm('구독을 해지할까요? 다음 결제만 취소되고, 이미 결제한 기간까지는 계속 이용할 수 있어요.')) return
  setCancelError('')
  setIsCancelling(true)
  try {
    await paymentApi.cancelAutoRenewal()
    const { data } = await paymentApi.getSchedule()
    setSchedule(data.data)
    alert('다음 결제가 취소됐어요. 이용 기간까지는 계속 이용 가능해요.')
  } catch (err) {
    setCancelError(err.response?.data?.message ?? '구독 해지에 실패했습니다.')
  } finally {
    setIsCancelling(false)
  }
}

async function handleResume() {
  setCancelError('')
  setIsCancelling(true)
  try {
    await paymentApi.resumeAutoRenewal()
    const { data } = await paymentApi.getSchedule()
    setSchedule(data.data)
    alert('정기결제가 다시 예약됐어요!')
  } catch (err) {
    setCancelError(err.response?.data?.message ?? '정기결제 재개에 실패했습니다.')
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
                {schedule?.autoRenew === true && schedule?.nextChargeAt && (
  <p className={styles.activeExpiry}>다음 결제일 {formatDate(schedule.nextChargeAt)}</p>
)}
{schedule?.autoRenew === false && subscription?.expiredAt && (
  <p className={styles.activeExpiry}>{formatDate(subscription.expiredAt)}까지 이용 가능 (자동결제 해지됨)</p>
)}
                {cancelError && <p className={styles.cancelError}>{cancelError}</p>}
                {schedule?.autoRenew !== false ? (
      <button type="button" className={styles.cancelButton} onClick={handleCancel} disabled={isCancelling}>
        {isCancelling ? '해지 처리 중...' : '구독 해지'}
      </button>
    ) : (
      <button type="button" className={styles.cancelButton} onClick={handleResume} disabled={isCancelling}>
        {isCancelling ? '처리 중...' : '다시 구독하기'}
      </button>
    )}
              </div>
            ) : (
              <>
                <button
                  type="button"
                  className={styles.premiumButton}
                  onClick={handleSubscribeClick}
                  disabled={isSubscribing}
                >
                  {isSubscribing ? '처리 중...' : '지금 구독'}
                </button>
                {subscribeError && <p className={styles.cancelError}>{subscribeError}</p>}
              </>
            )}
          </section>
        </div>
      </main>
    </>
  )
}
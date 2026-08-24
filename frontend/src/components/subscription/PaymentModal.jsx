import { CheckCircle2, CreditCard, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { billingKeyApi, paymentApi } from '../../api'
import styles from './PaymentModal.module.css'
import { registerCard } from './registerCard'
import { formatCardLabel } from '../../utils/formatCard'

const PLAN_FEATURES = ['전문가 1:1 상담 무제한', '스터디 개설·참여 무제한', '구독자 전용 스터디 참여']

// 카드 정보는 PortOne이 직접 띄우는 등록 창에서만 입력받는다 (우리 서버/프론트를 거치지 않음).
// 등록된 카드는 매달 자동으로 청구되며, 결제 성공 여부는 서버가 PortOne 재조회로 직접 검증한다.
export default function PaymentModal({ onClose, onSubscribed }) {
  const [stage, setStage] = useState('summary') // 'summary' | 'processing' | 'success' | 'error'
  const [error, setError] = useState('')
  // registeredCard: undefined(조회 전) | null(없음) | { cardName, cardNumberMasked, ... }
  const [registeredCard, setRegisteredCard] = useState(undefined)

  useEffect(() => {
    let ignore = false
    billingKeyApi
      .getMy()
      .then(({ data }) => {
        if (!ignore) setRegisteredCard(data.data.registered ? data.data : null)
      })
      .catch(() => {
        if (!ignore) setRegisteredCard(null)
      })
    return () => {
      ignore = true
    }
  }, [])

  function closeUnlessProcessing() {
    if (stage === 'processing') return
    onClose()
  }

  async function handleSubscribe() {
    setError('')
    setStage('processing')

    try {
      // 이미 등록된 카드가 있으면 재등록하지 않는다. 재등록하면 기존 카드가 교체(소프트 삭제)된다.
      if (!registeredCard) {
        await registerCard()
      }
      const { data } = await paymentApi.subscribeWithBillingKey()

      setStage('success')
      setTimeout(() => onSubscribed(data.data), 900)
    } catch (err) {
      setStage('error')
      setError(err.response?.data?.message ?? err.message ?? '카드 등록 또는 결제에 실패했어요.')
    }
  }

  return (
    <div className={styles.overlay} onClick={closeUnlessProcessing}>
      <div className={styles.modal} onClick={(event) => event.stopPropagation()}>
        {stage !== 'processing' && stage !== 'success' && (
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="닫기">
            <X size={18} />
          </button>
        )}

        {stage === 'success' ? (
          <div className={styles.resultState}>
            <CheckCircle2 size={40} className={styles.successIcon} />
            <p className={styles.resultTitle}>구독이 시작됐어요</p>
            <p className={styles.resultSubtitle}>프리미엄 기능을 바로 이용할 수 있어요.</p>
          </div>
        ) : (
          <>
            <p className={styles.eyebrow}>프리미엄 구독</p>
            <h2 className={styles.title}>매달 자동으로 결제돼요</h2>
            <p className={styles.price}>
              9,900원<span className={styles.priceUnit}> / 월</span>
            </p>

            <ul className={styles.featureList}>
              {PLAN_FEATURES.map((feature) => (
                <li key={feature} className={styles.featureItem}>
                  <span className={styles.bullet} />
                  {feature}
                </li>
              ))}
            </ul>

            {registeredCard && (
              <p className={styles.registeredCard}>
                <CreditCard size={14} />
                {formatCardLabel(registeredCard.cardName, registeredCard.cardNumberMasked)} 로 결제돼요
              </p>
            )}

            {error && <p className={styles.error}>{error}</p>}

            <button
              type="button"
              className={styles.payButton}
              onClick={handleSubscribe}
              disabled={stage === 'processing' || registeredCard === undefined}
            >
              <CreditCard size={16} />
              {stage === 'processing'
                ? '처리 중...'
                : registeredCard
                  ? '이 카드로 구독 시작'
                  : '카드 등록하고 시작하기'}
            </button>
            <p className={styles.disclaimer}>
              {registeredCard
                ? '마이페이지에 등록된 카드로 매달 자동 결제돼요. 언제든 해지할 수 있어요.'
                : '카드 정보는 PortOne 결제창에만 입력되며 저장되지 않아요. 언제든 해지할 수 있어요.'}
            </p>
          </>
        )}
      </div>
    </div>
  )
}

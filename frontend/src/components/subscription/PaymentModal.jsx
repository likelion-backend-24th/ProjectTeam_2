import { requestIssueBillingKey } from '@portone/browser-sdk/v2'
import { CheckCircle2, CreditCard, X } from 'lucide-react'
import { useState } from 'react'
import { billingKeyApi, paymentApi } from '../../api'
import styles from './PaymentModal.module.css'

const PLAN_FEATURES = ['전문가 1:1 상담 무제한', '스터디 개설·참여 무제한', '구독자 전용 스터디 참여']

// 카드 정보는 PortOne이 직접 띄우는 등록 창에서만 입력받는다 (우리 서버/프론트를 거치지 않음).
// 등록된 카드는 매달 자동으로 청구되며, 결제 성공 여부는 서버가 PortOne 재조회로 직접 검증한다.
export default function PaymentModal({ onClose, onSubscribed }) {
  const [stage, setStage] = useState('summary') // 'summary' | 'processing' | 'success' | 'error'
  const [error, setError] = useState('')

  function closeUnlessProcessing() {
    if (stage === 'processing') return
    onClose()
  }

  async function handleSubscribe() {
    setError('')
    setStage('processing')

    try {
      const { data: prepareRes } = await billingKeyApi.prepareIssue()
      const { storeId, channelKey, issueId, customerId } = prepareRes.data

      const issued = await requestIssueBillingKey({
        storeId,
        channelKey,
        billingKeyMethod: 'CARD',
        issueId,
        issueName: 'prep2gether 정기결제 카드 등록',
        customer: { customerId },
      })

      if (!issued || issued.code) {
        throw new Error(issued?.message ?? '카드 등록이 취소되었어요.')
      }

      // 채널이 수동 승인이면 billingKey는 'NEEDS_CONFIRMATION' 자리표시자로 오고,
      // billingIssueToken으로 서버가 PortOne에 발급을 확정해야 진짜 빌링키를 받을 수 있다.
      await billingKeyApi.completeIssue(issued.billingKey, issued.billingIssueToken)
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

            {error && <p className={styles.error}>{error}</p>}

            <button
              type="button"
              className={styles.payButton}
              onClick={handleSubscribe}
              disabled={stage === 'processing'}
            >
              <CreditCard size={16} />
              {stage === 'processing' ? '처리 중...' : '카드 등록하고 시작하기'}
            </button>
            <p className={styles.disclaimer}>카드 정보는 PortOne 결제창에만 입력되며 저장되지 않아요. 언제든 해지할 수 있어요.</p>
          </>
        )}
      </div>
    </div>
  )
}

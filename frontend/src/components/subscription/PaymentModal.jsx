import { requestPayment } from '@portone/browser-sdk/v2'
import { CheckCircle2, X } from 'lucide-react'
import { useState } from 'react'
import { paymentApi, subscriptionApi } from '../../api'
import styles from './PaymentModal.module.css'

// PortOne V2 결제창 연동.
// 카드 정보는 PortOne이 띄우는 결제창(PG사 화면)에서만 입력되고 우리 서버·프론트는
// 절대 다루지 않는다. 결제창의 응답(response)은 "단서"일 뿐이며, 최종 확정은 항상
// 서버(complete API)가 PortOne 결제 결과를 다시 조회·검증한 뒤에만 이뤄진다.
export default function PaymentModal({ onClose, onSubscribed }) {
  const [stage, setStage] = useState('idle') // 'idle' | 'processing' | 'success' | 'error'
  const [error, setError] = useState('')

  function closeUnlessProcessing() {
    if (stage === 'processing') return
    onClose()
  }

  async function handlePay() {
    setError('')
    setStage('processing')

    try {
      // 1. 결제 준비 — 서버가 planType으로 금액을 확정하고 paymentId를 발급한다.
      const { data: prepareRes } = await paymentApi.prepare('BASIC')
      const { paymentId, storeId, channelKey, amount, orderName } = prepareRes.data

      // 2. PortOne 결제창 호출 — 카드 정보는 여기서만 입력된다.
      const response = await requestPayment({
        storeId,
        channelKey,
        paymentId,
        orderName,
        totalAmount: amount,
        currency: 'KRW',
        payMethod: 'CARD',
      })

      // response가 undefined면 결제창 자체가 뜨지 못한 것(설정 오류 등)
      if (!response) {
        throw new Error('결제창을 여는 데 실패했습니다.')
      }

      if (response.code != null) {
        // 사용자가 결제창에서 취소했거나 PG 승인이 실패한 경우.
        // 서버는 아직 아무 상태도 PAID로 바꾸지 않았으니 그대로 안내만 한다.
        setStage('error')
        setError(response.message ?? '결제가 취소되었거나 실패했습니다.')
        return
      }

      // 3. 완료 API — 브라우저 응답을 신뢰하지 않고, 서버가 PortOne 결제를 다시 조회·검증한다.
      await paymentApi.complete(paymentId)

      // complete API는 결제 성공 여부만 알려줄 뿐 구독 정보를 내려주지 않으므로,
      // 최신 구독 상태를 별도로 조회해서 상위 컴포넌트에 전달한다.
      const { data: subscriptionRes } = await subscriptionApi.getMy()

      setStage('success')
      setTimeout(() => onSubscribed(subscriptionRes.data), 900)
    } catch (err) {
      setStage('error')
      setError(err.response?.data?.message ?? err.message ?? '결제에 실패했습니다.')
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
            <p className={styles.eyebrow}>PORTONE PAYMENT</p>
            <h2 className={styles.title}>프리미엄 구독</h2>
            <p className={styles.price}>9,900원 / 월</p>

            {error && <p className={styles.error}>{error}</p>}

            <button type="button" className={styles.payButton} onClick={handlePay} disabled={stage === 'processing'}>
              {stage === 'processing' ? '결제 처리 중...' : '9,900원 결제하기'}
            </button>
            <p className={styles.disclaimer}>결제 버튼을 누르면 PortOne 결제창이 열려요.</p>
          </>
        )}
      </div>
    </div>
  )
}

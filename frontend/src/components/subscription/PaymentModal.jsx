import { CheckCircle2, X } from 'lucide-react'
import { useState } from 'react'
import { subscriptionApi } from '../../api'
import styles from './PaymentModal.module.css'

// F-UI-12: 실제 PG 연동 없이 프론트에서 카드 입력 폼만 흉내 낸 뒤, 성공하면 백엔드에
// 구독 상태 반영(Mock)을 요청한다. 카드 정보는 어디에도 전송하지 않는다(입력값 검증만 함).
export default function PaymentModal({ onClose, onSubscribed }) {
  const [cardNumber, setCardNumber] = useState('')
  const [expiry, setExpiry] = useState('')
  const [cvc, setCvc] = useState('')
  const [stage, setStage] = useState('form') // 'form' | 'processing' | 'success' | 'error'
  const [error, setError] = useState('')

  const isFormValid = cardNumber.replace(/\s/g, '').length === 16 && /^\d{2}\/\d{2}$/.test(expiry) && cvc.length === 3

  function closeUnlessProcessing() {
    if (stage === 'processing') return
    onClose()
  }

  async function handlePay(event) {
    event.preventDefault()
    if (!isFormValid) {
      setError('카드 정보를 다시 확인해주세요.')
      return
    }
    setError('')
    setStage('processing')

    // 실제 결제창처럼 잠깐의 처리 시간을 흉내낸다.
    await new Promise((resolve) => setTimeout(resolve, 700))

    try {
      const { data } = await subscriptionApi.subscribe()
      setStage('success')
      setTimeout(() => onSubscribed(data.data), 900)
    } catch (err) {
      setStage('error')
      setError(err.response?.data?.message ?? '결제에 실패했습니다.')
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
            <p className={styles.eyebrow}>MOCK PAYMENT</p>
            <h2 className={styles.title}>프리미엄 구독</h2>
            <p className={styles.price}>9,900원 / 월</p>

            <form className={styles.form} onSubmit={handlePay}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="cardNumber">
                  카드 번호
                </label>
                <input
                  id="cardNumber"
                  className={styles.input}
                  placeholder="0000 0000 0000 0000"
                  inputMode="numeric"
                  maxLength={19}
                  value={cardNumber}
                  onChange={(event) => setCardNumber(event.target.value.replace(/[^\d ]/g, ''))}
                  disabled={stage === 'processing'}
                />
              </div>

              <div className={styles.row}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="expiry">
                    유효기간
                  </label>
                  <input
                    id="expiry"
                    className={styles.input}
                    placeholder="MM/YY"
                    maxLength={5}
                    value={expiry}
                    onChange={(event) => setExpiry(event.target.value)}
                    disabled={stage === 'processing'}
                  />
                </div>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="cvc">
                    CVC
                  </label>
                  <input
                    id="cvc"
                    className={styles.input}
                    placeholder="000"
                    inputMode="numeric"
                    maxLength={3}
                    value={cvc}
                    onChange={(event) => setCvc(event.target.value.replace(/\D/g, ''))}
                    disabled={stage === 'processing'}
                  />
                </div>
              </div>

              {error && <p className={styles.error}>{error}</p>}

              <button type="submit" className={styles.payButton} disabled={stage === 'processing'}>
                {stage === 'processing' ? '결제 처리 중...' : '9,900원 결제하기'}
              </button>
              <p className={styles.disclaimer}>* 실제 결제가 이뤄지지 않는 테스트 화면이에요.</p>
            </form>
          </>
        )}
      </div>
    </div>
  )
}

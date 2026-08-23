import { requestIssueBillingKey } from '@portone/browser-sdk/v2'
import { CheckCircle2, X } from 'lucide-react'
import { useState } from 'react'
import { paymentApi, subscriptionApi } from '../../api'
import styles from './PaymentModal.module.css'

const PLAN_TYPE = 'BASIC'

// mode='subscribe': 신규 구독 - prepare/complete를 호출하고, complete가 그 자리에서 첫 결제까지 실행함.
// mode='resume': 해지 예약 취소(자동갱신 재개) - resume/prepare/resume을 호출하고, 새 결제는 발생하지
// 않음(이미 낸 기간을 그대로 쓰다가 다음 자동갱신부터 이 카드로 청구됨).
// 두 흐름 다 PortOne 발급창을 띄우는 부분(requestIssueBillingKey)과 에러 처리는 동일해서 문구/호출할
// API만 여기서 갈라준다.
const MODE_TEXT = {
  subscribe: {
    title: '프리미엄 구독',
    priceLabel: '9,900원 / 월',
    payButtonLabel: '9,900원 결제하기',
    payButtonLoadingLabel: '결제 처리 중...',
    disclaimer: '결제 버튼을 누르면 PortOne 결제창이 열려요.',
    resultTitle: '구독이 시작됐어요',
    resultSubtitle: '프리미엄 기능을 바로 이용할 수 있어요.',
    genericError: '결제에 실패했습니다.',
  },
  resume: {
    title: '해지 예약 취소',
    priceLabel: '다음 결제일에 9,900원이 청구돼요',
    payButtonLabel: '결제수단 등록하고 재개하기',
    payButtonLoadingLabel: '처리 중...',
    disclaimer: '지금 결제가 발생하지 않아요. 카드 등록창만 열려요.',
    resultTitle: '해지 예약이 취소됐어요',
    resultSubtitle: '이용 중인 기간이 끝나면 자동으로 갱신돼요.',
    genericError: '결제수단 등록에 실패했습니다.',
  },
}

// PortOne V2 빌링키 발급 연동.
// 카드 정보는 PortOne이 띄우는 빌링키 발급 창(PG사 화면)에서만 입력되고 우리 서버·프론트는
// 절대 다루지 않는다. 발급 창의 응답(response)은 "단서"일 뿐이며, 최종 확정(빌링키 검증 +
// mode에 따른 결제/등록 실행)은 항상 서버가 PortOne에 다시 조회·요청한 뒤에만 이뤄진다.
export default function PaymentModal({ mode = 'subscribe', onClose, onSubscribed }) {
  const [stage, setStage] = useState('idle') // 'idle' | 'processing' | 'success' | 'error'
  const [error, setError] = useState('')
  const text = MODE_TEXT[mode]

  function closeUnlessProcessing() {
    if (stage === 'processing') return
    onClose()
  }

  async function handlePay() {
    setError('')
    setStage('processing')

    try {
      // 1. 발급 준비 — 서버가 issueId(발급 식별자)를 내려준다. resume은 결제가 없으니 서버가 금액을
      // 새로 계산하지 않고 현재 플랜 정보를 그대로 내려줄 뿐이다.
      const { data: prepareRes } =
        mode === 'resume' ? await subscriptionApi.prepareResume() : await paymentApi.prepare(PLAN_TYPE)
      const { issueId, storeId, channelKey, amount, orderName } = prepareRes.data

      // 2. PortOne 빌링키 발급 창 호출 — 카드 정보는 여기서만 입력된다. 결제는 아직 일어나지 않는다.
      const response = await requestIssueBillingKey({
        storeId,
        channelKey,
        billingKeyMethod: 'CARD',
        issueId,
        issueName: orderName,
        displayAmount: amount,
        currency: 'KRW',
      })

      // response가 undefined면 발급 창 자체가 뜨지 못한 것(설정 오류 등)
      if (!response) {
        throw new Error('결제창을 여는 데 실패했습니다.')
      }

      if (response.code != null) {
        // 사용자가 발급 창에서 취소했거나 PG 승인이 실패한 경우.
        // 서버는 아직 빌링키를 저장하지도, 결제를 시도하지도 않았으니 그대로 안내만 한다.
        setStage('error')
        setError(response.message ?? '취소되었거나 실패했습니다.')
        return
      }

      // 3. 완료 API — 발급받은 billingKey를 서버가 검증·저장한다.
      // subscribe는 그 빌링키로 첫 결제까지 서버가 바로 실행하고, resume은 등록만 하고 끝난다.
      // 이 호출이 성공하면 서버 쪽 처리는 이미 다 끝난 것이므로, 아래 구독 재조회는 별도
      // try/catch로 분리해 실패해도 "실패"로 보이지 않게 한다.
      if (mode === 'resume') {
        await subscriptionApi.resume(response.billingKey)
      } else {
        await paymentApi.complete(response.billingKey, PLAN_TYPE)
      }
    } catch (err) {
      setStage('error')
      setError(err.response?.data?.message ?? err.message ?? text.genericError)
      return
    }

    setStage('success')

    try {
      // 완료 API는 성공 여부만 알려줄 뿐 구독 정보를 내려주지 않으므로,
      // 최신 구독 상태를 별도로 조회해서 상위 컴포넌트에 전달한다.
      const { data: subscriptionRes } = await subscriptionApi.getMy()
      setTimeout(() => onSubscribed(subscriptionRes.data), 900)
    } catch (err) {
      // 위 작업 자체는 이미 성공했고 이 조회만 (일시적 네트워크 문제 등으로) 실패한 것이므로,
      // 사용자에게는 그대로 성공 화면을 보여준다. 상위 화면 갱신은 다음 방문/새로고침 때 자연히 맞춰진다.
      console.error('처리 후 구독 정보 재조회 실패', err)
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
            <p className={styles.resultTitle}>{text.resultTitle}</p>
            <p className={styles.resultSubtitle}>{text.resultSubtitle}</p>
          </div>
        ) : (
          <>
            <p className={styles.eyebrow}>PORTONE PAYMENT</p>
            <h2 className={styles.title}>{text.title}</h2>
            <p className={styles.price}>{text.priceLabel}</p>

            {error && <p className={styles.error}>{error}</p>}

            <button type="button" className={styles.payButton} onClick={handlePay} disabled={stage === 'processing'}>
              {stage === 'processing' ? text.payButtonLoadingLabel : text.payButtonLabel}
            </button>
            <p className={styles.disclaimer}>{text.disclaimer}</p>
          </>
        )}
      </div>
    </div>
  )
}

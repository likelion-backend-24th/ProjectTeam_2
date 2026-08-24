import apiClient from './client'

// GET /api/subscriptions/me — 구독 내역이 없으면 404(SUBSCRIPTION_NOT_FOUND)
function getMy() {
  return apiClient.get('/api/subscriptions/me')
}

// DELETE /api/subscriptions — 구독 해지 "예약". 즉시 끊기는 게 아니라 다음 자동갱신만 막힘.
// 이미 결제한 기간(expiredAt)까지는 계속 이용 가능.
function cancel() {
  return apiClient.delete('/api/subscriptions')
}

// POST /api/subscriptions/resume — 해지 예약 취소(자동갱신 재개). 해지 시 카드를 지우지 않으므로
// 새 카드 등록/결제 없이 바로 반영된다(파라미터도 없음).
function resume() {
  return apiClient.post('/api/subscriptions/resume')
}

// POST /api/subscriptions/retry — 유예기간(PAST_DUE) 중 스케줄러(최대 하루 1회)를 기다리지 않고
// 이미 등록된 카드로 지금 바로 재시도한다. 새 카드 등록 없음. 실패해도 에러가 아니라 200으로
// 응답하고, 응답의 status가 여전히 PAST_DUE면 이번 시도도 실패한 것(재시도 기회는 유지됨).
function retryPastDueNow() {
  return apiClient.post('/api/subscriptions/retry')
}

// POST /api/subscriptions/card/prepare — 결제수단 변경용 빌링키 발급 파라미터. 이미 자동갱신
// 중이어야만 가능(최초 등록은 paymentApi.prepare/resume 쪽 대상).
function prepareCardChange() {
  return apiClient.post('/api/subscriptions/card/prepare')
}

// POST /api/subscriptions/card — 발급받은 billingKey로 기존 카드를 교체한다. 결제는 안 함(다음
// 정상 갱신부터 새 카드로 청구).
function changeCard(billingKey) {
  return apiClient.post('/api/subscriptions/card', { billingKey })
}

export const subscriptionApi = {
  getMy,
  cancel,
  resume,
  retryPastDueNow,
  prepareCardChange,
  changeCard,
}

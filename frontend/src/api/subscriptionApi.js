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

// POST /api/subscriptions/resume/prepare — 해지 예약 취소(자동갱신 재개)용 빌링키 발급 파라미터.
// paymentApi.prepare와 달리 새 결제가 발생하지 않는다 - 이미 낸 기간은 그대로 유지됨.
function prepareResume() {
  return apiClient.post('/api/subscriptions/resume/prepare')
}

// POST /api/subscriptions/resume — 발급받은 billingKey를 등록해 해지 예약을 취소한다. 결제는 안 함.
function resume(billingKey) {
  return apiClient.post('/api/subscriptions/resume', { billingKey })
}

// POST /api/subscriptions/retry — 유예기간(PAST_DUE) 중 스케줄러(최대 하루 1회)를 기다리지 않고
// 이미 등록된 카드로 지금 바로 재시도한다. 새 카드 등록 없음. 실패해도 에러가 아니라 200으로
// 응답하고, 응답의 status가 여전히 PAST_DUE면 이번 시도도 실패한 것(재시도 기회는 유지됨).
function retryPastDueNow() {
  return apiClient.post('/api/subscriptions/retry')
}

export const subscriptionApi = {
  getMy,
  cancel,
  prepareResume,
  resume,
  retryPastDueNow,
}

import apiClient from './client'

// POST /api/payments/subscriptions/ready — 서버가 금액을 확정하고 paymentId를 발급한다.
function readySubscriptionPayment() {
  return apiClient.post('/api/payments/subscriptions/ready')
}

// POST /api/payments/subscriptions/complete — PortOne 결제 단건 조회로 서버가 결제 결과를 검증한다.
function completeSubscriptionPayment(paymentId) {
  return apiClient.post('/api/payments/subscriptions/complete', { paymentId })
}

// POST /api/payments/subscriptions/billing-key — 등록된 카드로 즉시 청구하고 정기결제 구독을 시작한다.
function subscribeWithBillingKey() {
  return apiClient.post('/api/payments/subscriptions/billing-key')
}

// POST /api/payments/subscriptions/retry — 결제 실패(PAST_DUE) 상태에서 수동으로 재결제를 시도한다.
function retrySubscriptionPayment() {
  return apiClient.post('/api/payments/subscriptions/retry')
}

export const paymentApi = {
  readySubscriptionPayment,
  completeSubscriptionPayment,
  subscribeWithBillingKey,
  retrySubscriptionPayment,
}

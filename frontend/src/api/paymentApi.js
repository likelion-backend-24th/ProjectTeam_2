import apiClient from './client'

// POST /api/payment/prepare — 서버가 planType으로 금액을 확정하고 issueId(빌링키 발급 식별자)를 내려준다.
// 클라이언트는 금액을 보내지 않는다.
function prepare(planType) {
  return apiClient.post('/api/payment/prepare', { planType })
}

// POST /api/payment/complete — 발급된 billingKey를 서버가 검증·저장하고,
// 그 빌링키로 첫 결제까지 서버가 직접 수행한다.
function complete(billingKey, planType) {
  return apiClient.post('/api/payment/complete', { billingKey, planType })
}

export const paymentApi = {
  prepare,
  complete,
}

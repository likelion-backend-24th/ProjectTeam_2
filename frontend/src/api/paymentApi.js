import apiClient from './client'

// POST /api/payment/prepare — 서버가 planType으로 금액을 확정하고 paymentId를 발급한다.
// 클라이언트는 금액을 보내지 않는다.
function prepare(planType) {
  return apiClient.post('/api/payment/prepare', { planType })
}

// POST /api/payment/complete — 결제창 완료 후 PortOne 결제 결과를 서버가 다시 조회·검증한다.
function complete(paymentId) {
  return apiClient.post('/api/payment/complete', { paymentId })
}

export const paymentApi = {
  prepare,
  complete,
}

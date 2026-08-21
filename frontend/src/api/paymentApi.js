import apiClient from './client'

// POST /api/payments/subscribe — 등록된 빌링키로 첫 결제를 실행하고 구독을 활성화한다.
function subscribe() {
  return apiClient.post('/api/payments/subscribe')
}

export const paymentApi = {
  subscribe,
}
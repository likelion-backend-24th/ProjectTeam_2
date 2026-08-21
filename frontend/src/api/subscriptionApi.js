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

export const subscriptionApi = {
  getMy,
  cancel,
}

import apiClient from './client'

// DELETE /api/subscriptions — 다음 회차 자동 갱신만 멈춘다. 만료일까지는 계속 이용 가능하다.
function cancel() {
  return apiClient.delete('/api/subscriptions')
}

// GET /api/subscriptions/me — 구독 내역이 없으면 404(SUBSCRIPTION_NOT_FOUND)
function getMy() {
  return apiClient.get('/api/subscriptions/me')
}

export const subscriptionApi = {
  cancel,
  getMy,
}

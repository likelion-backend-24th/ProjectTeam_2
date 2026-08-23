import apiClient from './client'

// POST /api/subscriptions — 프론트에서 결제 성공 처리 후 호출. 실제 PG 연동 없이 서버 상태만 ACTIVE로 반영(Mock).
function subscribe() {
  return apiClient.post('/api/subscriptions')
}

// DELETE /api/subscriptions — 다음 회차 자동 갱신만 멈춘다. 만료일까지는 계속 이용 가능하다.
function cancel() {
  return apiClient.delete('/api/subscriptions')
}

// POST /api/subscriptions/resume — 해지 예약 상태에서 자동 갱신을 다시 켠다.
function resume() {
  return apiClient.post('/api/subscriptions/resume')
}

// GET /api/subscriptions/me — 구독 내역이 없으면 404(SUBSCRIPTION_NOT_FOUND)
function getMy() {
  return apiClient.get('/api/subscriptions/me')
}

export const subscriptionApi = {
  subscribe,
  cancel,
  resume,
  getMy,
}

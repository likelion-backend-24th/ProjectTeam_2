import apiClient from './client'

// POST /api/payments/subscribe — 등록된 빌링키로 첫 결제를 실행하고 구독을 활성화한다.
function subscribe() {
  return apiClient.post('/api/payments/subscribe')
}

// DELETE /api/payments/subscribe — 다음 자동결제만 취소한다. 이용 기간은 만료일까지 유지된다.
function cancelAutoRenewal() {
  return apiClient.delete('/api/payments/subscribe')
}

// POST /api/payments/subscribe/resume — 취소했던 다음 자동결제를 같은 예정일로 다시 예약한다.
function resumeAutoRenewal() {
  return apiClient.post('/api/payments/subscribe/resume')
}


// GET /api/payments/schedule — 다음 결제 예정일과 자동갱신 여부를 조회한다.
function getSchedule() {
  return apiClient.get('/api/payments/schedule')
}

export const paymentApi = {
  subscribe,
  cancelAutoRenewal,
  resumeAutoRenewal,
   getSchedule,
}
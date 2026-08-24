import apiClient from './client'

// POST /api/billing-keys/prepare — PortOne 카드 등록 팝업 호출에 필요한 값을 발급한다.
function prepareIssue() {
  return apiClient.post('/api/billing-keys/prepare')
}

// POST /api/billing-keys/complete — PortOne이 발급한 빌링키를 서버가 검증한 뒤 저장한다.
// 채널이 수동 승인이면 billingKey 대신 billingIssueToken으로 서버가 발급을 확정한다.
function completeIssue(billingKey, billingIssueToken) {
  return apiClient.post('/api/billing-keys/complete', { billingKey, billingIssueToken })
}

// GET /api/billing-keys/me — 등록 여부와 등록 시각만 내려온다 (카드번호·카드사는 서버가 보관하지 않음).
function getMy() {
  return apiClient.get('/api/billing-keys/me')
}

// DELETE /api/billing-keys — PortOne과 서버에서 카드를 함께 삭제한다.
// 이용 중인 구독이 있으면 다음 회차 자동 갱신도 함께 해지된다 (이미 결제된 기간은 만료일까지 유지).
function remove() {
  return apiClient.delete('/api/billing-keys')
}

export const billingKeyApi = {
  prepareIssue,
  completeIssue,
  getMy,
  remove,
}

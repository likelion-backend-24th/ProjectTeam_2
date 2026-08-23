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

export const billingKeyApi = {
  prepareIssue,
  completeIssue,
}

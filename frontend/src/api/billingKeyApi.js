import apiClient from './client'

// POST /api/billing-keys/prepare — 카드 등록에 필요한 storeId, channelKey, issueId를 받아온다.
function prepare() {
  return apiClient.post('/api/billing-keys/prepare')
}

// POST /api/billing-keys/verify — PortOne 카드 등록 완료 후, 서버가 재조회하여 검증하고 빌링키를 저장한다.
function verify(issueId, billingKey) {
  return apiClient.post('/api/billing-keys/verify', { issueId, billingKey })
}

export const billingKeyApi = {
  prepare,
  verify,
}
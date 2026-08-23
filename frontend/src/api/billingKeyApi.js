import apiClient from './client'

// POST /api/billing-keys/prepare — 카드 등록에 필요한 storeId, channelKey, issueId를 받아온다.
function prepare() {
  return apiClient.post('/api/billing-keys/prepare')
}

// POST /api/billing-keys/verify — PortOne 카드 등록 완료 후, 서버가 재조회하여 검증하고 빌링키를 저장한다.
function verify(issueId, billingKey) {
  return apiClient.post('/api/billing-keys/verify', { issueId, billingKey })
}

// GET /api/billing-keys — 등록된 카드 목록을 등록 순서대로 조회한다.
function getMyBillingKeys() {
  return apiClient.get('/api/billing-keys')
}

// PATCH /api/billing-keys/{billingKeyId}/select — 지정한 카드를 결제/예약에 사용할 카드로 선택한다.
function selectBillingKey(billingKeyId) {
  return apiClient.patch(`/api/billing-keys/${billingKeyId}/select`)
}

// DELETE /api/billing-keys/{billingKeyId} — 등록된 카드를 삭제한다. 선택된 카드는 삭제할 수 없다.
function deleteBillingKey(billingKeyId) {
  return apiClient.delete(`/api/billing-keys/${billingKeyId}`)
}

export const billingKeyApi = {
  prepare,
  verify,
  getMyBillingKeys,
  selectBillingKey,
  deleteBillingKey,
}
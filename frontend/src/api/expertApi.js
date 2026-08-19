import apiClient from './client'

// POST /api/experts/signup
function signupExpert(payload) {
  // payload: { introduction, careers: [...], certifications: [...] }
  return apiClient.post('/api/experts/signup', payload)
}

// GET /api/experts (비로그인 접근 가능, 승인된 전문가만, 페이지당 12명)
function getPublicExperts(params) {
  return apiClient.get('/api/experts', { params })
}

// GET /api/experts/:id (비로그인 접근 가능, 승인된 전문가만)
function getExpertDetail(id) {
  return apiClient.get(`/api/experts/${id}`)
}

// GET /api/experts/me (본인 신청 상태 + 반려사유만 내려줌, 신청 내용 자체는 조회 불가)
function getMyApplicationStatus() {
  return apiClient.get('/api/experts/me')
}

// PATCH /api/experts/me (PENDING 상태일 때만 수정 가능)
function updateMyApplication(payload) {
  return apiClient.patch('/api/experts/me', payload)
}

// ---- 관리자 전용 ----

// GET /api/admin/experts?status=&page=&size= (페이지당 기본 10명)
function getExperts(status, params) {
  return apiClient.get('/api/admin/experts', { params: { status, ...params } })
}

// PATCH /api/admin/experts/:id/approve
function approveExpert(id) {
  return apiClient.patch(`/api/admin/experts/${id}/approve`)
}

// PATCH /api/admin/experts/:id/reject
function rejectExpert(id, reason) {
  return apiClient.patch(`/api/admin/experts/${id}/reject`, reason ? { reason } : undefined)
}

// DELETE /api/admin/experts/:id
function revokeExpert(id, reason) {
  return apiClient.delete(`/api/admin/experts/${id}`, { data: reason ? { reason } : undefined })
}

export const expertApi = {
  signupExpert,
  getPublicExperts,
  getExpertDetail,
  getMyApplicationStatus,
  updateMyApplication,
  getExperts,
  approveExpert,
  rejectExpert,
  revokeExpert,
}

import apiClient from './client'

// POST /api/experts/signup
function signupExpert(payload) {
  // payload: { introduction, careers: [...], certifications: [...] }
  return apiClient.post('/api/experts/signup', payload)
}

// GET /api/experts (비로그인 접근 가능, 승인된 전문가만)
function getPublicExperts() {
  return apiClient.get('/api/experts')
}

// ---- 관리자 전용 ----

// GET /api/admin/experts?status=
function getExperts(status) {
  return apiClient.get('/api/admin/experts', { params: { status } })
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
  getExperts,
  approveExpert,
  rejectExpert,
  revokeExpert,
}

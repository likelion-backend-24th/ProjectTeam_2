import apiClient from './client'

// POST /api/studies
function createStudy(payload) {
  // payload: { title, description, capacity, recruitEnd, category } — recruitEnd는 선택(null이면 상시 모집)
  return apiClient.post('/api/studies', payload)
}

// GET /api/studies/my
function getMyStudies(params) {
  return apiClient.get('/api/studies/my', { params })
}

// GET /api/studies (비로그인 접근 가능)
function getStudies(params) {
  // params: { keyword, page, size } — 정렬은 백엔드가 고정(구독자 모집글 상단 고정 + 최신순)하므로 sort는 넘기지 않는다.
  return apiClient.get('/api/studies', { params })
}

// GET /api/studies/:id
function getStudyById(id) {
  return apiClient.get(`/api/studies/${id}`)
}

// PUT /api/studies/:id
function updateStudy(id, payload) {
  return apiClient.put(`/api/studies/${id}`, payload)
}

// ---- 스터디 멤버 ----

// POST /api/studies/:id/members
function joinStudy(id) {
  return apiClient.post(`/api/studies/${id}/members`)
}

// GET /api/studies/:id/members
function getStudyMembers(id) {
  return apiClient.get(`/api/studies/${id}/members`)
}

// DELETE /api/studies/:id/members/:memberId
function removeStudyMember(id, memberId) {
  return apiClient.delete(`/api/studies/${id}/members/${memberId}`)
}

// PATCH /api/studies/:id/leader
function delegateLeader(id, newLeaderId) {
  return apiClient.patch(`/api/studies/${id}/leader`, { newLeaderId })
}

// DELETE /api/studies/:id/leave
function leaveStudy(id) {
  return apiClient.delete(`/api/studies/${id}/leave`)
}

export const studyApi = {
  createStudy,
  getMyStudies,
  getStudies,
  getStudyById,
  updateStudy,
  joinStudy,
  getStudyMembers,
  removeStudyMember,
  delegateLeader,
  leaveStudy,
}

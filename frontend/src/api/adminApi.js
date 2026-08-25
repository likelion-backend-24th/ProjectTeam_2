import apiClient from './client'

// GET /api/admin/users (ADMIN 전용)
function getUsers(params) {
  return apiClient.get('/api/admin/users', { params })
}

// GET /api/admin/studies?keyword=&category= (ADMIN 전용)
function getStudies(params) {
  return apiClient.get('/api/admin/studies', { params })
}

// GET /api/admin/posts?keyword=&category= (ADMIN 전용)
function getPosts(params) {
  return apiClient.get('/api/admin/posts', { params })
}

// PATCH /api/admin/users/:id/status  { status: 'ACTIVE' | 'SUSPENDED' }
function updateUserStatus(id, status) {
  return apiClient.patch(`/api/admin/users/${id}/status`, { status })
}

// DELETE /api/admin/posts/:id (강제 삭제)
function deletePost(id) {
  return apiClient.delete(`/api/admin/posts/${id}`)
}

// DELETE /api/admin/studies/:id (강제 삭제)
function deleteStudy(id) {
  return apiClient.delete(`/api/admin/studies/${id}`)
}

// DELETE /api/admin/comments/:id (강제 삭제)
function deleteComment(id) {
  return apiClient.delete(`/api/admin/comments/${id}`)
}

// DELETE /api/admin/study-posts/:id (강제 삭제)
function deleteStudyPost(id) {
  return apiClient.delete(`/api/admin/study-posts/${id}`)
}

// DELETE /api/admin/study-post-comments/:id (강제 삭제)
function deleteStudyPostComment(id) {
  return apiClient.delete(`/api/admin/study-post-comments/${id}`)
}

// GET /api/admin/reports?status= (status 없으면 전체)
function getReports(params) {
  return apiClient.get('/api/admin/reports', { params })
}

// GET /api/admin/feedbacks/:id (신고 접수된 상담 스레드만 조회 가능. expertProfileId 확인용)
function getFeedback(id) {
  return apiClient.get(`/api/admin/feedbacks/${id}`)
}

// PATCH /api/admin/reports/:id/resolve (신고 대상 콘텐츠 삭제로 종료)
function resolveReport(id) {
  return apiClient.patch(`/api/admin/reports/${id}/resolve`)
}

// PATCH /api/admin/reports/:id/reject (콘텐츠는 유지하고 반려)
function rejectReport(id) {
  return apiClient.patch(`/api/admin/reports/${id}/reject`)
}

export const adminApi = {
  getUsers,
  getStudies,
  getPosts,
  updateUserStatus,
  deletePost,
  deleteStudy,
  deleteComment,
  deleteStudyPost,
  deleteStudyPostComment,
  getReports,
  getFeedback,
  resolveReport,
  rejectReport,
}

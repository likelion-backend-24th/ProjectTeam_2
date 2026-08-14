import apiClient from './client'

// GET /api/admin/users (ADMIN 전용)
function getUsers(params) {
  return apiClient.get('/api/admin/users', { params })
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

export const adminApi = {
  getUsers,
  updateUserStatus,
  deletePost,
  deleteStudy,
}

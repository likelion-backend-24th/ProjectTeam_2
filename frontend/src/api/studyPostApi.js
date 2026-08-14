import apiClient from './client'

// ---- 스터디 게시판 ----

// POST /api/studies/:id/posts (멀티파트: 백엔드가 @RequestPart("data")+images로 받음)
function createStudyPost(studyId, payload, images = []) {
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  images.forEach((file) => formData.append('images', file))
  return apiClient.post(`/api/studies/${studyId}/posts`, formData)
}

// GET /api/studies/:id/posts
function getStudyPosts(studyId) {
  return apiClient.get(`/api/studies/${studyId}/posts`)
}

// GET /api/studies/:id/posts/:postId
function getStudyPostDetail(studyId, postId) {
  return apiClient.get(`/api/studies/${studyId}/posts/${postId}`)
}

// PUT /api/studies/:id/posts/:postId
function updateStudyPost(studyId, postId, payload) {
  return apiClient.put(`/api/studies/${studyId}/posts/${postId}`, payload)
}

// DELETE /api/studies/:id/posts/:postId
function deleteStudyPost(studyId, postId) {
  return apiClient.delete(`/api/studies/${studyId}/posts/${postId}`)
}

// ---- 스터디 게시판 댓글 ----

// POST /api/studies/:id/posts/:postId/comments
function createStudyPostComment(studyId, postId, payload) {
  return apiClient.post(`/api/studies/${studyId}/posts/${postId}/comments`, payload)
}

// PUT /api/studies/:id/posts/:postId/comments/:commentId
function updateStudyPostComment(studyId, postId, commentId, payload) {
  return apiClient.put(`/api/studies/${studyId}/posts/${postId}/comments/${commentId}`, payload)
}

// DELETE /api/studies/:id/posts/:postId/comments/:commentId
function deleteStudyPostComment(studyId, postId, commentId) {
  return apiClient.delete(`/api/studies/${studyId}/posts/${postId}/comments/${commentId}`)
}

export const studyPostApi = {
  createStudyPost,
  getStudyPosts,
  getStudyPostDetail,
  updateStudyPost,
  deleteStudyPost,
  createStudyPostComment,
  updateStudyPostComment,
  deleteStudyPostComment,
}

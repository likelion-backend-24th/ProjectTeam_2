import apiClient from './client'

// POST /api/posts/:postId/comments
function createComment(postId, payload) {
  return apiClient.post(`/api/posts/${postId}/comments`, payload)
}

// PUT /api/posts/:postId/comments/:commentId
function updateComment(postId, commentId, payload) {
  return apiClient.put(`/api/posts/${postId}/comments/${commentId}`, payload)
}

// DELETE /api/posts/:postId/comments/:commentId
function deleteComment(postId, commentId) {
  return apiClient.delete(`/api/posts/${postId}/comments/${commentId}`)
}

export const commentApi = {
  createComment,
  updateComment,
  deleteComment,
}

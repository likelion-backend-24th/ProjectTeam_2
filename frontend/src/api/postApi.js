import apiClient from './client'

// POST /api/posts
function createPost(payload) {
  // payload: { title, content, category }
  return apiClient.post('/api/posts', payload)
}

// GET /api/posts (비로그인 접근 가능)
function getPosts(params) {
  // params: { category, keyword, page, size, sort }
  return apiClient.get('/api/posts', { params })
}

// GET /api/posts/me
function getMyPosts(params) {
  return apiClient.get('/api/posts/me', { params })
}

// GET /api/posts/:postId
function getPostDetail(postId, params) {
  // params: 댓글 페이징(page, size)
  return apiClient.get(`/api/posts/${postId}`, { params })
}

// PUT /api/posts/:postId
function updatePost(postId, payload) {
  return apiClient.put(`/api/posts/${postId}`, payload)
}

// DELETE /api/posts/:postId
function deletePost(postId) {
  return apiClient.delete(`/api/posts/${postId}`)
}

export const postApi = {
  createPost,
  getPosts,
  getMyPosts,
  getPostDetail,
  updatePost,
  deletePost,
}

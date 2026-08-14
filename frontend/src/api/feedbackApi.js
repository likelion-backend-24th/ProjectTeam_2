import apiClient from './client'

// POST /api/feedbacks
function createFeedback(payload) {
  // payload: { expertProfileId, topic }
  return apiClient.post('/api/feedbacks', payload)
}

// GET /api/feedbacks/:id
function getFeedback(id) {
  return apiClient.get(`/api/feedbacks/${id}`)
}

// POST /api/feedbacks/:id/messages
function addMessage(id, payload) {
  // payload: { content }
  return apiClient.post(`/api/feedbacks/${id}/messages`, payload)
}

// GET /api/feedbacks/:id/messages
function getMessages(id) {
  return apiClient.get(`/api/feedbacks/${id}/messages`)
}

// GET /api/feedbacks/me (구독자용 내 문의 목록)
function getMyFeedbacks() {
  return apiClient.get('/api/feedbacks/me')
}

// GET /api/feedbacks/expert (전문가용 받은 문의 목록)
function getMyExpertFeedbacks() {
  return apiClient.get('/api/feedbacks/expert')
}

// PATCH /api/feedbacks/:id/close (요청자 본인만 종료 가능)
function closeFeedback(id) {
  return apiClient.patch(`/api/feedbacks/${id}/close`)
}

export const feedbackApi = {
  createFeedback,
  getFeedback,
  addMessage,
  getMessages,
  getMyFeedbacks,
  getMyExpertFeedbacks,
  closeFeedback,
}

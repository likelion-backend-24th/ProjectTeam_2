import apiClient from './client'

// POST /api/feedbacks (멀티파트: 백엔드가 @RequestPart("data")+images로 받음)
function createFeedback(payload, images = []) {
  // payload: { expertProfileId, topic, content }
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  images.forEach((file) => formData.append('images', file))
  return apiClient.post('/api/feedbacks', formData)
}

// GET /api/feedbacks/:id
function getFeedback(id) {
  return apiClient.get(`/api/feedbacks/${id}`)
}

// POST /api/feedbacks/:id/messages (멀티파트: 백엔드가 @RequestPart("data")+images로 받음)
function addMessage(id, payload, images = []) {
  // payload: { content }
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  images.forEach((file) => formData.append('images', file))
  return apiClient.post(`/api/feedbacks/${id}/messages`, formData)
}

// GET /api/feedbacks/:id/messages
function getMessages(id) {
  return apiClient.get(`/api/feedbacks/${id}/messages`)
}

// GET /api/feedbacks/me (구독자용 내 문의 목록)
function getMyFeedbacks(params) {
  return apiClient.get('/api/feedbacks/me', { params })
}

// GET /api/feedbacks/expert (전문가용 받은 문의 목록)
function getMyExpertFeedbacks() {
  return apiClient.get('/api/feedbacks/expert')
}

// PATCH /api/feedbacks/:id/close (요청자 본인만 종료 가능)
function closeFeedback(id) {
  return apiClient.patch(`/api/feedbacks/${id}/close`)
}

// DELETE /api/feedbacks/{id} — 요청자 본인만 삭제 가능
function deleteFeedback(id) {
  return apiClient.delete(`/api/feedbacks/${id}`)
}

export const feedbackApi = {
  createFeedback,
  getFeedback,
  addMessage,
  getMessages,
  getMyFeedbacks,
  getMyExpertFeedbacks,
  closeFeedback,
  deleteFeedback,
}
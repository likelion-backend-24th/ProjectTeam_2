import apiClient from './client'

function getUnreadCount() {
  return apiClient.get('/api/notifications/count')
}

function getNotifications(params) {
  return apiClient.get('/api/notifications', { params })
}

// PATCH /api/notifications/:id/read
function markAsRead(id) {
  return apiClient.patch(`/api/notifications/${id}/read`)
}

export const notificationApi = {
  getUnreadCount,
  getNotifications,
  markAsRead,
}
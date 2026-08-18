import apiClient from './client'

function getUnreadCount() {
  return apiClient.get('/api/notifications/count')
}

function getNotifications(params) {
  return apiClient.get('/api/notifications', { params })
}

export const notificationApi = {
  getUnreadCount,
  getNotifications,
}
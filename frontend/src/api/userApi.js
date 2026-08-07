import apiClient from './client'

// GET /api/users/me
function getMe() {
  return apiClient.get('/api/users/me')
}

// PATCH /api/users/me/nickname
function updateNickname(nickname) {
  return apiClient.patch('/api/users/me/nickname', { nickname })
}

// PATCH /api/users/me/password
function updatePassword({ currentPassword, newPassword, newPasswordConfirm }) {
  return apiClient.patch('/api/users/me/password', {
    currentPassword,
    newPassword,
    newPasswordConfirm,
  })
}

// DELETE /api/users/me
function withdraw(password) {
  return apiClient.delete('/api/users/me', { data: { password } })
}

export const userApi = {
  getMe,
  updateNickname,
  updatePassword,
  withdraw,
}

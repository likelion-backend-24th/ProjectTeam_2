import apiClient, { setAccessToken } from './client'

// POST /api/auth/signup
function signup(payload) {
  // payload: { name, username, password, nickname }
  return apiClient.post('/api/auth/signup', payload)
}

// POST /api/auth/login
async function login(payload) {
  // payload: { username, password }
  const { data } = await apiClient.post('/api/auth/login', payload)
  setAccessToken(data.data.accessToken)
  return data
}

// POST /api/auth/email/send
function sendEmailVerificationCode(email) {
  return apiClient.post('/api/auth/email/send', { email })
}

// POST /api/auth/email/verify
function verifyEmailCode(email, code) {
  return apiClient.post('/api/auth/email/verify', { email, code })
}

// POST /api/auth/resetpassword (이메일 인증 완료 후에만 성공)
function resetPassword({ username, newPassword }) {
  return apiClient.post('/api/auth/resetpassword', { username, newPassword })
}

// POST /api/auth/logout (인증 필요)
async function logout() {
  const { data } = await apiClient.post('/api/auth/logout')
  setAccessToken(null)
  return data
}

// POST /api/auth/reissue (refreshToken 쿠키 필요, 보통 axios 인터셉터가 자동 호출)
async function reissue() {
  const { data } = await apiClient.post('/api/auth/reissue')
  setAccessToken(data.data.accessToken)
  return data
}

// POST /api/auth/kakao
async function kakaoLogin(kakaoAccessToken) {
  const { data } = await apiClient.post('/api/auth/kakao', { kakaoAccessToken })
  setAccessToken(data.data.accessToken)
  return data
}

// POST /api/auth/google
async function googleLogin(googleAccessToken) {
  const { data } = await apiClient.post('/api/auth/google', { googleAccessToken })
  setAccessToken(data.data.accessToken)
  return data
}

// POST /api/auth/naver
async function naverLogin(naverAccessToken) {
  const { data } = await apiClient.post('/api/auth/naver', { naverAccessToken })
  setAccessToken(data.data.accessToken)
  return data
}

export const authApi = {
  signup,
  login,
  logout,
  reissue,
  kakaoLogin,
  googleLogin,
  naverLogin,
  sendEmailVerificationCode,
  verifyEmailCode,
  resetPassword,
}

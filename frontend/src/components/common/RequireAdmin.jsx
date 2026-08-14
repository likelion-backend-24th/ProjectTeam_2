import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

// 관리자 전용 라우트를 감싸는 가드. 비로그인은 로그인 페이지로, 관리자가 아니면 홈으로 보낸다.
export default function RequireAdmin({ children }) {
  const { user, isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return null
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return children
}

import { createBrowserRouter, Outlet, ScrollRestoration } from 'react-router-dom'
import RequireAuth from './components/common/RequireAuth'
import HomePage from './pages/HomePage'
import LoginPage from './pages/auth/LoginPage'
import SignupPage from './pages/auth/SignupPage'
import PostDetailPage from './pages/posts/PostDetailPage'
import PostFormPage from './pages/posts/PostFormPage'
import PostListPage from './pages/posts/PostListPage'

// 페이지 이동 시 스크롤 위치를 맨 위로 초기화(뒤로가기/앞으로가기는 위치 복원)하기 위한
// 최상위 레이아웃. 실제 화면 레이아웃에는 관여하지 않고 <Outlet/>만 그대로 렌더링한다.
function RootLayout() {
  return (
    <>
      <Outlet />
      <ScrollRestoration />
    </>
  )
}

// 아직 와이어프레임이 없는 페이지(스터디/전문가 등)는 추가하지 않고,
// 확정된 페이지만 라우트로 연결한다.
export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignupPage /> },
      { path: '/posts', element: <PostListPage /> },
      {
        path: '/posts/new',
        element: (
          <RequireAuth>
            <PostFormPage />
          </RequireAuth>
        ),
      },
      { path: '/posts/:postId', element: <PostDetailPage /> },
    ],
  },
])

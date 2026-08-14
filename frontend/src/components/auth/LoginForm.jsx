import { ArrowRight, Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './AuthFormLayout.module.css'
import SocialLoginButtons from './SocialLoginButtons'

export default function LoginForm() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login } = useAuth()
  // 게시글 상세 등 로그인이 필요해 이 페이지로 넘어온 경우, 로그인 후 원래 보려던 페이지로 되돌려준다.
  const from = location.state?.from
    ? `${location.state.from.pathname}${location.state.from.search ?? ''}`
    : '/'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      await login({ username, password })
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.response?.data?.message ?? '이메일 또는 비밀번호를 확인해주세요.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.panel}>
      <div className={styles.headingRow}>
        <h1 className={styles.title}>로그인</h1>
        <p className={styles.subtitle}>
          계정이 없으신가요?{' '}
          <Link to="/signup" className={styles.link}>
            회원가입
          </Link>
        </p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="username">
            이메일
          </label>
          <input
            id="username"
            name="username"
            type="email"
            autoComplete="username email"
            placeholder="이메일을 입력하세요"
            className={styles.input}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="password">
            비밀번호
          </label>
          <div className={styles.inputWrapper}>
            <input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="비밀번호를 입력하세요"
              className={styles.input}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
            <button
              type="button"
              className={styles.eyeButton}
              onClick={() => setShowPassword((prev) => !prev)}
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>

        {error && <p className={styles.error}>{error}</p>}

        <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
          {isSubmitting ? '로그인 중...' : '로그인'}
          <ArrowRight size={18} />
        </button>
      </form>

      <SocialLoginButtons mode="login" />

      <p className={styles.footerText}>
        비밀번호를 잊으셨나요?{' '}
        <Link to="/find-password" className={styles.linkUnderline}>
          비밀번호 재설정
        </Link>
      </p>
    </div>
  )
}

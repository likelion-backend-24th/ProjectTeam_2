import { ArrowRight, Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import layout from './AuthFormLayout.module.css'
import SocialLoginButtons from './SocialLoginButtons'
import styles from './SignupForm.module.css'

export default function SignupForm({
  name,
  onNameChange,
  username,
  onUsernameChange,
  nickname,
  onNicknameChange,
  password,
  onPasswordChange,
  confirmPassword,
  onConfirmPasswordChange,
  agreed,
  onAgreedChange,
  isSubmitting,
  error,
  onSubmit,
}) {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  return (
    <div className={layout.panel}>
      <div className={layout.headingRow}>
        <h1 className={layout.title}>회원가입</h1>
        <p className={layout.subtitle}>
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className={layout.link}>
            로그인
          </Link>
        </p>
      </div>

      <form className={layout.form} onSubmit={onSubmit}>
        <div className={layout.field}>
          <label className={layout.label} htmlFor="name">
            이름
          </label>
          <input
            id="name"
            name="name"
            type="text"
            autoComplete="name"
            placeholder="실명을 입력하세요"
            className={layout.input}
            value={name}
            onChange={(event) => onNameChange(event.target.value)}
            required
          />
        </div>

        <div className={layout.field}>
          <label className={layout.label} htmlFor="username">
            이메일
          </label>
          <input
            id="username"
            name="username"
            type="email"
            autoComplete="username email"
            placeholder="이메일을 입력하세요"
            className={layout.input}
            value={username}
            onChange={(event) => onUsernameChange(event.target.value)}
            required
          />
          <p className={layout.helperText}>로그인에 사용할 이메일이에요.</p>
        </div>

        <div className={layout.field}>
          <label className={layout.label} htmlFor="nickname">
            닉네임
          </label>
          <input
            id="nickname"
            name="nickname"
            type="text"
            autoComplete="nickname"
            placeholder="커뮤니티에서 표시될 이름"
            className={layout.input}
            value={nickname}
            onChange={(event) => onNicknameChange(event.target.value)}
            required
          />
          <p className={layout.helperText}>2~12자, 언제든 변경 가능해요.</p>
        </div>

        <div className={layout.field}>
          <label className={layout.label} htmlFor="password">
            비밀번호
          </label>
          <div className={layout.inputWrapper}>
            <input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="8자 이상 입력하세요"
              className={layout.input}
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              required
            />
            <button
              type="button"
              className={layout.eyeButton}
              onClick={() => setShowPassword((prev) => !prev)}
              aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>

        <div className={layout.field}>
          <label className={layout.label} htmlFor="confirmPassword">
            비밀번호 확인
          </label>
          <div className={layout.inputWrapper}>
            <input
              id="confirmPassword"
              name="confirmPassword"
              type={showConfirmPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="비밀번호를 다시 입력하세요"
              className={layout.input}
              value={confirmPassword}
              onChange={(event) => onConfirmPasswordChange(event.target.value)}
              required
            />
            <button
              type="button"
              className={layout.eyeButton}
              onClick={() => setShowConfirmPassword((prev) => !prev)}
              aria-label={showConfirmPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
            >
              {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>

        <label className={styles.termsRow}>
          <input
            type="checkbox"
            className={styles.checkbox}
            checked={agreed}
            onChange={(event) => onAgreedChange(event.target.checked)}
          />
          <span className={styles.termsText}>
            <a href="#" className={styles.termsLink}>
              이용약관
            </a>{' '}
            및{' '}
            <a href="#" className={styles.termsLink}>
              개인정보처리방침
            </a>
            에 동의합니다. (필수)
          </span>
        </label>

        {error && <p className={layout.error}>{error}</p>}

        <button type="submit" className={layout.submitButton} disabled={isSubmitting}>
          {isSubmitting ? '가입 중...' : '가입하기'}
          <ArrowRight size={18} />
        </button>
      </form>

      <SocialLoginButtons mode="signup" />
    </div>
  )
}

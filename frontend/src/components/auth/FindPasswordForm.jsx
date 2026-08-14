import { ArrowRight, CheckCircle2, Eye, EyeOff } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../../api'
import layout from './AuthFormLayout.module.css'
import styles from './FindPasswordForm.module.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const RESEND_COOLDOWN_SECONDS = 60

// 비밀번호 재설정 3단계: 이메일 인증코드 발송 → 코드 검증 → 새 비밀번호 설정
// 백엔드 POST /api/auth/resetpassword는 직전에 이메일 인증(verifyCode)이 완료돼 있어야만 성공한다.
export default function FindPasswordForm() {
  const navigate = useNavigate()
  const [step, setStep] = useState('email') // 'email' | 'code' | 'password' | 'done'

  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [cooldown, setCooldown] = useState(0)

  const isEmailValid = EMAIL_PATTERN.test(email)
  const isCodeValid = code.trim().length > 0
  const isPasswordValid = newPassword.length >= 8
  const isConfirmValid = confirmPassword.length > 0 && confirmPassword === newPassword

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setInterval(() => setCooldown((prev) => Math.max(prev - 1, 0)), 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  async function handleSendCode(event) {
    event.preventDefault()
    if (!isEmailValid) {
      setError('올바른 이메일 형식을 입력해주세요.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      await authApi.sendEmailVerificationCode(email)
      setNotice('인증코드를 발송했어요. 메일함을 확인해주세요. (유효시간 5분)')
      setCode('')
      setCooldown(RESEND_COOLDOWN_SECONDS)
      setStep('code')
    } catch (err) {
      setError(err.response?.data?.message ?? '인증코드 발송에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleResendCode() {
    if (cooldown > 0 || isSubmitting) return
    setError('')
    setNotice('')
    setIsSubmitting(true)
    try {
      await authApi.sendEmailVerificationCode(email)
      setNotice('인증코드를 다시 발송했어요.')
      setCode('')
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err) {
      setError(err.response?.data?.message ?? '인증코드 발송에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleVerifyCode(event) {
    event.preventDefault()
    if (!isCodeValid) {
      setError('인증코드를 입력해주세요.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      await authApi.verifyEmailCode(email, code)
      setNotice('')
      setStep('password')
    } catch (err) {
      setError(err.response?.data?.message ?? '인증코드가 올바르지 않습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault()
    if (!isPasswordValid || !isConfirmValid) {
      setError('비밀번호를 다시 확인해주세요.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      await authApi.resetPassword({ username: email, newPassword })
      setStep('done')
    } catch (err) {
      setError(err.response?.data?.message ?? '비밀번호 재설정에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (step === 'done') {
    return (
      <div className={layout.panel}>
        <div className={styles.doneState}>
          <CheckCircle2 size={40} className={styles.doneIcon} />
          <h1 className={layout.title}>비밀번호가 변경됐어요</h1>
          <p className={styles.doneText}>새 비밀번호로 다시 로그인해주세요.</p>
          <button type="button" className={layout.submitButton} onClick={() => navigate('/login')}>
            로그인하러 가기
            <ArrowRight size={18} />
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={layout.panel}>
      <div className={layout.headingRow}>
        <h1 className={layout.title}>비밀번호 재설정</h1>
        <p className={layout.subtitle}>
          계정이 기억나셨나요?{' '}
          <Link to="/login" className={layout.link}>
            로그인
          </Link>
        </p>
      </div>

      {step === 'email' && (
        <form className={layout.form} onSubmit={handleSendCode}>
          <div className={layout.field}>
            <label className={layout.label} htmlFor="email">
              이메일
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="username email"
              placeholder="가입 시 사용한 이메일을 입력하세요"
              className={layout.input}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
            <p className={layout.helperText}>입력한 이메일로 인증코드를 보내드려요.</p>
          </div>

          {error && <p className={layout.error}>{error}</p>}

          <button type="submit" className={layout.submitButton} disabled={isSubmitting || !isEmailValid}>
            {isSubmitting ? '발송 중...' : '인증코드 받기'}
            <ArrowRight size={18} />
          </button>
        </form>
      )}

      {step === 'code' && (
        <form className={layout.form} onSubmit={handleVerifyCode}>
          <div className={layout.field}>
            <label className={layout.label} htmlFor="code">
              인증코드
            </label>
            <div className={styles.inlineRow}>
              <input
                id="code"
                name="code"
                type="text"
                inputMode="numeric"
                placeholder="6자리 코드를 입력하세요"
                className={`${layout.input} ${styles.codeInput}`}
                value={code}
                onChange={(event) => setCode(event.target.value)}
                required
              />
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={handleResendCode}
                disabled={cooldown > 0 || isSubmitting}
              >
                {cooldown > 0 ? `재발송 (${cooldown}s)` : '재발송'}
              </button>
            </div>
            <p className={layout.helperText}>{email}로 보낸 6자리 코드예요.</p>
          </div>

          {notice && !error && <p className={styles.success}>{notice}</p>}
          {error && <p className={layout.error}>{error}</p>}

          <button type="submit" className={layout.submitButton} disabled={isSubmitting || !isCodeValid}>
            {isSubmitting ? '확인 중...' : '인증코드 확인'}
            <ArrowRight size={18} />
          </button>
        </form>
      )}

      {step === 'password' && (
        <form className={layout.form} onSubmit={handleResetPassword}>
          <div className={layout.field}>
            <label className={layout.label} htmlFor="newPassword">
              새 비밀번호
            </label>
            <div className={layout.inputWrapper}>
              <input
                id="newPassword"
                name="newPassword"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="8자 이상 입력하세요"
                className={layout.input}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
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
              새 비밀번호 확인
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
                onChange={(event) => setConfirmPassword(event.target.value)}
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

          {error && <p className={layout.error}>{error}</p>}

          <button
            type="submit"
            className={layout.submitButton}
            disabled={isSubmitting || !isPasswordValid || !isConfirmValid}
          >
            {isSubmitting ? '변경 중...' : '비밀번호 변경'}
            <ArrowRight size={18} />
          </button>
        </form>
      )}
    </div>
  )
}

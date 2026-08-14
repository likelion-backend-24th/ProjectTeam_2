import { ArrowRight, CheckCircle2, Eye, EyeOff } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../../api'
import layout from './AuthFormLayout.module.css'
import SocialLoginButtons from './SocialLoginButtons'
import styles from './SignupForm.module.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const RESEND_COOLDOWN_SECONDS = 60

export default function SignupForm({
  name,
  onNameChange,
  username,
  onUsernameChange,
  isEmailVerified,
  onEmailVerifiedChange,
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

  // 이메일 인증(F-39): 백엔드 회원가입이 사전에 인증 완료된 이메일만 허용하므로,
  // 가입 폼 안에서 코드 발송 -> 확인까지 끝내야 '가입하기'가 활성화된다.
  const isEmailValid = EMAIL_PATTERN.test(username)
  const [hasSentCode, setHasSentCode] = useState(false)
  const [emailCode, setEmailCode] = useState('')
  const [isSendingCode, setIsSendingCode] = useState(false)
  const [isVerifyingCode, setIsVerifyingCode] = useState(false)
  const [cooldown, setCooldown] = useState(0)
  const [emailNotice, setEmailNotice] = useState('')
  const [emailError, setEmailError] = useState('')

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setInterval(() => setCooldown((prev) => Math.max(prev - 1, 0)), 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  async function handleSendCode() {
    if (!isEmailValid || cooldown > 0) return
    setEmailError('')
    setIsSendingCode(true)
    try {
      await authApi.sendEmailVerificationCode(username)
      setEmailNotice(hasSentCode ? '인증코드를 다시 발송했어요.' : '인증코드를 발송했어요. 메일함을 확인해주세요. (유효시간 5분)')
      setEmailCode('')
      setCooldown(RESEND_COOLDOWN_SECONDS)
      setHasSentCode(true)
    } catch (err) {
      setEmailError(err.response?.data?.message ?? '인증코드 발송에 실패했습니다.')
    } finally {
      setIsSendingCode(false)
    }
  }

  async function handleVerifyCode() {
    if (!emailCode.trim()) return
    setEmailError('')
    setIsVerifyingCode(true)
    try {
      await authApi.verifyEmailCode(username, emailCode)
      setEmailNotice('')
      onEmailVerifiedChange(true)
    } catch (err) {
      setEmailError(err.response?.data?.message ?? '인증코드가 올바르지 않습니다.')
    } finally {
      setIsVerifyingCode(false)
    }
  }

  function handleUsernameChange(value) {
    // 이메일을 바꾸면 이전 인증은 무효이므로 처음부터 다시 받게 한다.
    onUsernameChange(value)
    onEmailVerifiedChange(false)
    setHasSentCode(false)
    setEmailCode('')
    setEmailNotice('')
    setEmailError('')
    setCooldown(0)
  }

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
          <div className={styles.inlineRow}>
            <input
              id="username"
              name="username"
              type="email"
              autoComplete="username email"
              placeholder="이메일을 입력하세요"
              className={`${layout.input} ${styles.emailInput}`}
              value={username}
              onChange={(event) => handleUsernameChange(event.target.value)}
              disabled={isEmailVerified}
              required
            />
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={handleSendCode}
              disabled={!isEmailValid || isEmailVerified || isSendingCode || cooldown > 0}
            >
              {isEmailVerified
                ? '인증완료'
                : cooldown > 0
                  ? `재발송 (${cooldown}s)`
                  : isSendingCode
                    ? '발송 중...'
                    : hasSentCode
                      ? '재발송'
                      : '인증코드 받기'}
            </button>
          </div>
          {isEmailVerified ? (
            <p className={styles.success}>
              <CheckCircle2 size={14} />
              이메일 인증이 완료됐어요.
            </p>
          ) : (
            <p className={layout.helperText}>로그인에 사용할 이메일이에요. 인증 후 가입할 수 있어요.</p>
          )}
        </div>

        {hasSentCode && !isEmailVerified && (
          <div className={layout.field}>
            <label className={layout.label} htmlFor="emailCode">
              인증코드
            </label>
            <div className={styles.inlineRow}>
              <input
                id="emailCode"
                name="emailCode"
                type="text"
                inputMode="numeric"
                placeholder="6자리 코드를 입력하세요"
                className={`${layout.input} ${styles.codeInput}`}
                value={emailCode}
                onChange={(event) => setEmailCode(event.target.value)}
              />
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={handleVerifyCode}
                disabled={!emailCode.trim() || isVerifyingCode}
              >
                {isVerifyingCode ? '확인 중...' : '확인'}
              </button>
            </div>
            {emailError && <p className={layout.error}>{emailError}</p>}
            {emailNotice && !emailError && <p className={styles.success}>{emailNotice}</p>}
          </div>
        )}

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
            <Link to="/terms" target="_blank" rel="noopener noreferrer" className={styles.termsLink}>
              이용약관
            </Link>{' '}
            및{' '}
            <Link to="/privacy" target="_blank" rel="noopener noreferrer" className={styles.termsLink}>
              개인정보처리방침
            </Link>
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

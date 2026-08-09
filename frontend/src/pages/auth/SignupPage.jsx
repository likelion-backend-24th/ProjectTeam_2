import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../../api'
import AuthShowcase from '../../components/auth/AuthShowcase'
import SignupForm from '../../components/auth/SignupForm'
import SignupProgress from '../../components/auth/SignupProgress'
import SiteHeader from '../../components/common/SiteHeader'
import styles from './AuthPageLayout.module.css'

// 백엔드 SignupRequest 기준: name(이름), username(이메일 형식), nickname, password 모두 필수
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function SignupPage() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [username, setUsername] = useState('')
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [agreed, setAgreed] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  const isNameValid = name.trim().length > 0
  const isUsernameValid = EMAIL_PATTERN.test(username)
  const isNicknameValid = nickname.length >= 2 && nickname.length <= 12
  const isPasswordValid = password.length >= 8
  const isConfirmValid = confirmPassword.length > 0 && confirmPassword === password
  const isPasswordStepDone = isPasswordValid && isConfirmValid
  const canSubmit = isNameValid && isUsernameValid && isNicknameValid && isPasswordStepDone && agreed

  const steps = [
    { label: '이름', done: isNameValid },
    { label: '이메일', done: isUsernameValid },
    { label: '닉네임', done: isNicknameValid },
    { label: '비밀번호', done: isPasswordStepDone },
    { label: '약관 동의', done: agreed },
  ]

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canSubmit) {
      setError('입력값을 다시 확인해주세요.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      await authApi.signup({ name, username, nickname, password })
      navigate('/login')
    } catch (err) {
      setError(err.response?.data?.message ?? '회원가입에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.card}>
          <AuthShowcase
            heading={['함께라면', '더 빠릅니다.']}
            description={['지금 가입하고 38,200명의 취준생 커뮤니티에 합류하세요.']}
          >
            <SignupProgress steps={steps} />
          </AuthShowcase>

          <SignupForm
            name={name}
            onNameChange={setName}
            username={username}
            onUsernameChange={setUsername}
            nickname={nickname}
            onNicknameChange={setNickname}
            password={password}
            onPasswordChange={setPassword}
            confirmPassword={confirmPassword}
            onConfirmPasswordChange={setConfirmPassword}
            agreed={agreed}
            onAgreedChange={setAgreed}
            isSubmitting={isSubmitting}
            error={error}
            onSubmit={handleSubmit}
          />
        </div>
      </main>
    </>
  )
}

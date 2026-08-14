import AuthShowcase from '../../components/auth/AuthShowcase'
import FindPasswordForm from '../../components/auth/FindPasswordForm'
import SiteHeader from '../../components/common/SiteHeader'
import styles from './AuthPageLayout.module.css'

export default function FindPasswordPage() {
  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.card}>
          <AuthShowcase
            heading={['금방', '다시 찾아드릴게요.']}
            description={['이메일 인증만 마치면', '바로 새 비밀번호로 로그인할 수 있어요.']}
          />
          <FindPasswordForm />
        </div>
      </main>
    </>
  )
}

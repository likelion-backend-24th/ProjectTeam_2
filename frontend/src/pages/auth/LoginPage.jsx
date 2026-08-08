import AuthShowcase from '../../components/auth/AuthShowcase'
import LoginForm from '../../components/auth/LoginForm'
import Testimonial from '../../components/auth/Testimonial'
import SiteHeader from '../../components/common/SiteHeader'
import styles from './AuthPageLayout.module.css'

export default function LoginPage() {
  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.card}>
          <AuthShowcase
            heading={['다시 돌아온 것을', '환영해요.']}
            description={['38,200명의 취준생과 함께', '오늘도 한 걸음 더 나아가세요.']}
          >
            <Testimonial
              quote={['스터디 덕분에 6개월 만에 네이버 합격했어요.', '혼자였으면 불가능했을 거예요.']}
              avatarInitial="김"
              name="김민지 · 네이버 프론트엔드 합격"
            />
          </AuthShowcase>
          <LoginForm />
        </div>
      </main>
    </>
  )
}

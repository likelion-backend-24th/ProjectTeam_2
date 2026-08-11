import styles from './SignupProgress.module.css'

const BENEFITS = ['커뮤니티 게시글 작성 · 댓글', '스터디 신청 · 참여 (최대 2개)', '전문가 상담 (구독 후)']
// .
// AuthShowcase 하단에 들어가는 가입 진행률 + 혜택 안내 (회원가입 페이지에서 사용).
// steps: [{ label, done }] 형태로 폼 입력 상태에 맞춰 실시간으로 채워진다.
export default function SignupProgress({ steps }) {
  const completedCount = steps.filter((step) => step.done).length

  return (
    <div className={styles.wrapper}>
      <p className={styles.title}>가입 진행률</p>

      <div className={styles.steps}>
        {steps.map((step) => (
          <div key={step.label} className={styles.step}>
            <span className={`${styles.stepLabel} ${step.done ? styles.stepLabelDone : ''}`}>
              {step.label}
            </span>
            <span className={styles.bar}>
              <span className={`${styles.barFill} ${step.done ? styles.barFillDone : ''}`} />
            </span>
          </div>
        ))}
      </div>

      <p className={styles.count}>
        {completedCount}/{steps.length} 완료
      </p>

      <ul className={styles.benefits}>
        {BENEFITS.map((benefit) => (
          <li key={benefit}>
            <span className={styles.dot} />
            {benefit}
          </li>
        ))}
      </ul>
    </div>
  )
}

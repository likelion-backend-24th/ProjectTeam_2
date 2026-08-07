import { Sparkles, TrendingUp, Users } from 'lucide-react'
import styles from './WhySection.module.css'

const REASONS = [
  {
    icon: TrendingUp,
    title: '실시간 취업 정보',
    description: '채용공고, 기업 리뷰, 면접 후기가 매일 업데이트됩니다.',
  },
  {
    icon: Users,
    title: '스터디 매칭',
    description: '직무 · 언어 · 자격증 카테고리별로 딱 맞는 스터디를 찾으세요.',
  },
  {
    icon: Sparkles,
    title: '전문가 멘토링',
    description: '현직자의 1:1 피드백으로 서류 · 면접 합격률을 높이세요.',
  },
]

export default function WhySection() {
  return (
    <section className={styles.section}>
      <div className={styles.inner}>
        <div className={styles.heading}>
          <p className={styles.eyebrow}>WHY JOBTOGETHER</p>
          <h2 className={styles.title}>혼자보다 같이가 훨씬 빠릅니다</h2>
        </div>

        <div className={styles.grid}>
          {REASONS.map(({ icon: Icon, title, description }) => (
            <div key={title} className={styles.card}>
              <span className={styles.iconBox}>
                <Icon size={20} />
              </span>
              <p className={styles.cardTitle}>{title}</p>
              <p className={styles.cardDescription}>{description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

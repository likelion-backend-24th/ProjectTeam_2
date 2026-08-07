import styles from './StatsBar.module.css'

// TODO: 실제 통계 API 연동 전까지는 정적 값 표시
const STATS = [
  { value: '0+', label: '활성 회원' },
  { value: '0+', label: '진행 중 스터디' },
  { value: '0+', label: '합격 후기' },
  { value: '0%', label: '구독 만족도' },
]

export default function StatsBar() {
  return (
    <section className={styles.bar}>
      <div className={styles.inner}>
        {STATS.map((stat) => (
          <div key={stat.label} className={styles.item}>
            <p className={styles.value}>{stat.value}</p>
            <p className={styles.label}>{stat.label}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
